#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Songxi (松息) — Idempotent Non-/tmp JDK 17 + Android SDK + Gradle 8.10.2 Setup
# Safe to BOTH execute (`bash /home/antigravity/env-setup.sh`) and `source`.
# ==============================================================================

export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export ANDROID_HOME="/home/antigravity/.android-sdk"
export ANDROID_SDK_ROOT="/home/antigravity/.android-sdk"
export GRADLE_USER_HOME="/home/antigravity/.gradle"
export GRADLE_DIST_HOME="${GRADLE_USER_HOME}/gradle-dist/gradle-8.10.2"

# Preserve caller-specific non-/tmp TMPDIR; only fall back to default_env/tmp when unset or /tmp*
SONGXI_DEFAULT_TMP="/home/antigravity/.gemini/jetski/scratch/teamwork_tmp/default_env/tmp"
if [ -z "${TMPDIR:-}" ] || [[ "${TMPDIR}" == /tmp ]] || [[ "${TMPDIR}" == /tmp/* ]]; then
  export TMPDIR="${SONGXI_DEFAULT_TMP}"
fi
mkdir -p "${TMPDIR}"
export TMP="${TMPDIR}"
export TEMP="${TMPDIR}"
export npm_config_tmp="${TMPDIR}"

mkdir -p "${ANDROID_HOME}/cmdline-tools" "${GRADLE_USER_HOME}/ssl" "${GRADLE_USER_HOME}/tmp" "${HOME}/.android"
[ -f "${HOME}/.android/repositories.cfg" ] || touch "${HOME}/.android/repositories.cfg"

# Synchronize Java PKCS12 trustStore from /etc/ssl/certs/ca-certificates.crt when missing or updated on uVM boot
SONGXI_TRUSTSTORE="${GRADLE_USER_HOME}/ssl/songxi-cacerts.p12"
if [ -f "/etc/ssl/certs/ca-certificates.crt" ] && { [ ! -f "${SONGXI_TRUSTSTORE}" ] || [ "/etc/ssl/certs/ca-certificates.crt" -nt "${SONGXI_TRUSTSTORE}" ]; }; then
  _sx_ssl_stage="${GRADLE_USER_HOME}/ssl/.stage-$$"
  rm -rf "${_sx_ssl_stage}"
  mkdir -p "${_sx_ssl_stage}"
  awk -v dir="${_sx_ssl_stage}" 'BEGIN {c=0} /BEGIN CERTIFICATE/ {c++} { if (c>0) print > (dir "/cert_" c ".pem") }' /etc/ssl/certs/ca-certificates.crt
  for _sx_pem in "${_sx_ssl_stage}"/cert_*.pem; do
    [ -f "${_sx_pem}" ] || continue
    "${JAVA_HOME}/bin/keytool" -importcert -noprompt -storetype PKCS12 \
      -keystore "${_sx_ssl_stage}/cacerts.p12" -storepass changeit \
      -alias "ca_$(basename "${_sx_pem}")" -file "${_sx_pem}" >/dev/null 2>&1 || true
  done
  if [ -f "${_sx_ssl_stage}/cacerts.p12" ]; then
    mv -f "${_sx_ssl_stage}/cacerts.p12" "${SONGXI_TRUSTSTORE}"
  fi
  rm -rf "${_sx_ssl_stage}"
  unset _sx_ssl_stage _sx_pem
fi

# Synchronize JAVA_TOOL_OPTIONS and GRADLE_USER_HOME/gradle.properties with active trustStore & proxy
_songxi_clean_jto="$(printf '%s' "${JAVA_TOOL_OPTIONS:-}" | sed -E '
  s/(^|[[:space:]])-Djava\.io\.tmpdir=[^[:space:]]+//g;
  s/(^|[[:space:]])-XX:-UsePerfData//g;
  s/(^|[[:space:]])-Djavax\.net\.ssl\.trustStore[^[:space:]]*//g;
  s/(^|[[:space:]])-Dhttps?\.proxy(Host|Port)=[^[:space:]]+//g;
  s/(^|[[:space:]])-Dhttp\.nonProxyHosts=[^[:space:]]+//g;
  s/^[[:space:]]+//;
  s/[[:space:]]+$//;
  s/[[:space:]]+/ /g
')"
_songxi_extra_jto="-Djava.io.tmpdir=${TMPDIR} -XX:-UsePerfData"
_songxi_gprops=""
if [ -f "${SONGXI_TRUSTSTORE}" ]; then
  _songxi_extra_jto="${_songxi_extra_jto} -Djavax.net.ssl.trustStore=${SONGXI_TRUSTSTORE} -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
  _songxi_gprops="systemProp.javax.net.ssl.trustStore=${SONGXI_TRUSTSTORE}
systemProp.javax.net.ssl.trustStorePassword=changeit
systemProp.javax.net.ssl.trustStoreType=PKCS12"
fi
_songxi_proxy_raw="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
if [ -n "${_songxi_proxy_raw}" ]; then
  _sx_hp="${_songxi_proxy_raw#*://}"
  _sx_hp="${_sx_hp%%/*}"
  _sx_host="${_sx_hp%%:*}"
  _sx_port="${_sx_hp##*:}"
  if [ -n "${_sx_host}" ] && [ -n "${_sx_port}" ] && [ "${_sx_host}" != "${_sx_port}" ]; then
    _songxi_extra_jto="${_songxi_extra_jto} -Dhttp.proxyHost=${_sx_host} -Dhttp.proxyPort=${_sx_port} -Dhttps.proxyHost=${_sx_host} -Dhttps.proxyPort=${_sx_port} -Dhttp.nonProxyHosts=localhost|127.0.0.1|[::1]"
    _songxi_gprops="${_songxi_gprops}${_songxi_gprops:+
}systemProp.http.proxyHost=${_sx_host}
systemProp.http.proxyPort=${_sx_port}
systemProp.https.proxyHost=${_sx_host}
systemProp.https.proxyPort=${_sx_port}
systemProp.http.nonProxyHosts=localhost|127.0.0.1|[::1]"
  fi
  unset _sx_hp _sx_host _sx_port
fi
if [ -n "${_songxi_gprops}" ]; then
  _sx_gp_file="${GRADLE_USER_HOME}/gradle.properties"
  if [ ! -f "${_sx_gp_file}" ] || [ "$(cat "${_sx_gp_file}" 2>/dev/null || true)" != "${_songxi_gprops}" ]; then
    printf '%s\n' "${_songxi_gprops}" > "${_sx_gp_file}"
  fi
  unset _sx_gp_file
fi
if [ -n "${_songxi_clean_jto}" ]; then
  export JAVA_TOOL_OPTIONS="${_songxi_clean_jto} ${_songxi_extra_jto}"
else
  export JAVA_TOOL_OPTIONS="${_songxi_extra_jto}"
fi
unset _songxi_clean_jto _songxi_extra_jto _songxi_proxy_raw _songxi_gprops

# Idempotent PATH helper
_songxi_prepend_path() {
  case ":${PATH:-}:" in
    *":$1:"*) ;;
    *) export PATH="$1${PATH:+:$PATH}" ;;
  esac
}
_songxi_prepend_path "${JAVA_HOME}/bin"
_songxi_prepend_path "${GRADLE_DIST_HOME}/bin"
_songxi_prepend_path "${ANDROID_HOME}/platform-tools"
_songxi_prepend_path "${ANDROID_HOME}/build-tools/35.0.0"
_songxi_prepend_path "${ANDROID_HOME}/cmdline-tools/latest/bin"

# 1. Strip any non-interactive guard in ~/.bashrc and persist idempotent env block once
BASHRC_FILE="${HOME}/.bashrc"
touch "${BASHRC_FILE}"
if grep -qE 'case \$- in|\[ -z "\$PS1" \] && return' "${BASHRC_FILE}" 2>/dev/null; then
  sed -i '/case \$- in/,/esac/d; /\[ -z "\$PS1" \] && return/d' "${BASHRC_FILE}" 2>/dev/null || true
fi

if ! grep -qxF '# songxi-android-env-v3' "${BASHRC_FILE}" 2>/dev/null; then
  sed -i '/# >>> songxi-android-env >>>/,/# <<< songxi-android-env <<</d' "${BASHRC_FILE}" 2>/dev/null || true
  cat >> "${BASHRC_FILE}" <<'EOF'

# >>> songxi-android-env >>>
# songxi-android-env-v3
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export ANDROID_HOME="/home/antigravity/.android-sdk"
export ANDROID_SDK_ROOT="/home/antigravity/.android-sdk"
export GRADLE_USER_HOME="/home/antigravity/.gradle"
if [ -z "${TMPDIR:-}" ] || [[ "${TMPDIR}" == /tmp ]] || [[ "${TMPDIR}" == /tmp/* ]]; then
  export TMPDIR="/home/antigravity/.gemini/jetski/scratch/teamwork_tmp/default_env/tmp"
fi
mkdir -p "${TMPDIR}" "${GRADLE_USER_HOME}/ssl" 2>/dev/null || true
export TMP="${TMPDIR}"
export TEMP="${TMPDIR}"
export npm_config_tmp="${TMPDIR}"
_sx_ts="${GRADLE_USER_HOME}/ssl/songxi-cacerts.p12"
if [ -f "/etc/ssl/certs/ca-certificates.crt" ] && { [ ! -f "${_sx_ts}" ] || [ "/etc/ssl/certs/ca-certificates.crt" -nt "${_sx_ts}" ]; }; then
  _sx_st="${GRADLE_USER_HOME}/ssl/.stage-$$"
  rm -rf "${_sx_st}" && mkdir -p "${_sx_st}"
  awk -v dir="${_sx_st}" 'BEGIN {c=0} /BEGIN CERTIFICATE/ {c++} { if (c>0) print > (dir "/cert_" c ".pem") }' /etc/ssl/certs/ca-certificates.crt
  for _sx_pem in "${_sx_st}"/cert_*.pem; do
    [ -f "${_sx_pem}" ] || continue
    "${JAVA_HOME}/bin/keytool" -importcert -noprompt -storetype PKCS12 -keystore "${_sx_st}/cacerts.p12" -storepass changeit -alias "ca_$(basename "${_sx_pem}")" -file "${_sx_pem}" >/dev/null 2>&1 || true
  done
  [ -f "${_sx_st}/cacerts.p12" ] && mv -f "${_sx_st}/cacerts.p12" "${_sx_ts}"
  rm -rf "${_sx_st}"
  unset _sx_st _sx_pem
fi
_sx_jto="$(printf '%s' "${JAVA_TOOL_OPTIONS:-}" | sed -E 's/(^|[[:space:]])-Djava\.io\.tmpdir=[^[:space:]]+//g; s/(^|[[:space:]])-XX:-UsePerfData//g; s/(^|[[:space:]])-Djavax\.net\.ssl\.trustStore[^[:space:]]*//g; s/(^|[[:space:]])-Dhttps?\.proxy(Host|Port)=[^[:space:]]+//g; s/(^|[[:space:]])-Dhttp\.nonProxyHosts=[^[:space:]]+//g; s/^[[:space:]]+//; s/[[:space:]]+$//; s/[[:space:]]+/ /g')"
_sx_ext="-Djava.io.tmpdir=${TMPDIR} -XX:-UsePerfData"
_sx_gp=""
if [ -f "${_sx_ts}" ]; then
  _sx_ext="${_sx_ext} -Djavax.net.ssl.trustStore=${_sx_ts} -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
  _sx_gp="systemProp.javax.net.ssl.trustStore=${_sx_ts}
systemProp.javax.net.ssl.trustStorePassword=changeit
systemProp.javax.net.ssl.trustStoreType=PKCS12"
fi
_sx_pr="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
if [ -n "${_sx_pr}" ]; then
  _sx_hp="${_sx_pr#*://}"; _sx_hp="${_sx_hp%%/*}"
  _sx_h="${_sx_hp%%:*}"; _sx_pt="${_sx_hp##*:}"
  if [ -n "${_sx_h}" ] && [ -n "${_sx_pt}" ] && [ "${_sx_h}" != "${_sx_pt}" ]; then
    _sx_ext="${_sx_ext} -Dhttp.proxyHost=${_sx_h} -Dhttp.proxyPort=${_sx_pt} -Dhttps.proxyHost=${_sx_h} -Dhttps.proxyPort=${_sx_pt} -Dhttp.nonProxyHosts=localhost|127.0.0.1|[::1]"
    _sx_gp="${_sx_gp}${_sx_gp:+
}systemProp.http.proxyHost=${_sx_h}
systemProp.http.proxyPort=${_sx_pt}
systemProp.https.proxyHost=${_sx_h}
systemProp.https.proxyPort=${_sx_pt}
systemProp.http.nonProxyHosts=localhost|127.0.0.1|[::1]"
  fi
  unset _sx_hp _sx_h _sx_pt
fi
if [ -n "${_sx_gp}" ]; then
  _sx_gpf="${GRADLE_USER_HOME}/gradle.properties"
  if [ ! -f "${_sx_gpf}" ] || [ "$(cat "${_sx_gpf}" 2>/dev/null || true)" != "${_sx_gp}" ]; then
    printf '%s\n' "${_sx_gp}" > "${_sx_gpf}" 2>/dev/null || true
  fi
  unset _sx_gpf
fi
if [ -n "${_sx_jto}" ]; then
  export JAVA_TOOL_OPTIONS="${_sx_jto} ${_sx_ext}"
else
  export JAVA_TOOL_OPTIONS="${_sx_ext}"
fi
unset _sx_ts _sx_jto _sx_ext _sx_pr _sx_gp
for _sx_p in \
  "${ANDROID_HOME}/cmdline-tools/latest/bin" \
  "${ANDROID_HOME}/build-tools/35.0.0" \
  "${ANDROID_HOME}/platform-tools" \
  "${GRADLE_USER_HOME}/gradle-dist/gradle-8.10.2/bin" \
  "${JAVA_HOME}/bin"; do
  case ":${PATH:-}:" in
    *":${_sx_p}:"*) ;;
    *) export PATH="${_sx_p}${PATH:+:${PATH}}" ;;
  esac
done
unset _sx_p
# <<< songxi-android-env <<<
EOF
fi

# 2. Bootstrap Android Command-Line Tools (13114758_latest) — skipped once installed
if [ ! -x "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" ]; then
  CMDLINE_STAGE="${ANDROID_HOME}/.cmdline-stage-$$"
  CMDLINE_ZIP="${CMDLINE_STAGE}/commandlinetools-linux-13114758_latest.zip"
  rm -rf "${CMDLINE_STAGE}"
  mkdir -p "${CMDLINE_STAGE}" "${ANDROID_HOME}/cmdline-tools"
  curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip" -o "${CMDLINE_ZIP}"
  unzip -q -o "${CMDLINE_ZIP}" -d "${CMDLINE_STAGE}/extract"
  rm -rf "${ANDROID_HOME}/cmdline-tools/latest"
  mv "${CMDLINE_STAGE}/extract/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"
  rm -rf "${CMDLINE_STAGE}"
fi

# 3. Accept licenses & install ONLY required SDK packages (NO emulator, NO system-images)
#    Fast-path check skips sdkmanager JVM startup (<0.02s) when all packages + licenses exist!
if [ ! -f "${ANDROID_HOME}/licenses/android-sdk-license" ] || \
   [ ! -x "${ANDROID_HOME}/platform-tools/adb" ] || \
   [ ! -f "${ANDROID_HOME}/platforms/android-35/android.jar" ] || \
   [ ! -f "${ANDROID_HOME}/platforms/android-34/android.jar" ] || \
   [ ! -x "${ANDROID_HOME}/build-tools/35.0.0/aapt2" ] || \
   [ ! -x "${ANDROID_HOME}/build-tools/34.0.0/aapt2" ]; then
  (
    set +o pipefail
    yes | "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --sdk_root="${ANDROID_HOME}" --licenses >/dev/null
  )
  "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --sdk_root="${ANDROID_HOME}" \
    "platform-tools" \
    "platforms;android-35" \
    "platforms;android-34" \
    "build-tools;35.0.0" \
    "build-tools;34.0.0"
fi

# 4. Bootstrap standalone Gradle 8.10.2 binary (for `gradle wrapper` generation) — skipped once installed
if [ ! -x "${GRADLE_DIST_HOME}/bin/gradle" ]; then
  GRADLE_STAGE="${GRADLE_USER_HOME}/.gradle-stage-$$"
  GRADLE_ZIP="${GRADLE_STAGE}/gradle-8.10.2-bin.zip"
  rm -rf "${GRADLE_STAGE}"
  mkdir -p "${GRADLE_STAGE}" "${GRADLE_USER_HOME}/gradle-dist"
  curl -fsSL "https://services.gradle.org/distributions/gradle-8.10.2-bin.zip" -o "${GRADLE_ZIP}"
  unzip -q -o "${GRADLE_ZIP}" -d "${GRADLE_STAGE}/extract"
  rm -rf "${GRADLE_DIST_HOME}"
  mv "${GRADLE_STAGE}/extract/gradle-8.10.2" "${GRADLE_DIST_HOME}"
  rm -rf "${GRADLE_STAGE}"
fi
