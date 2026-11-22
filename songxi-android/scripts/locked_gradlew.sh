#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Songxi (松息) — Exclusive Flock-Serialized Non-/tmp Gradle Entrypoint
# - Preserves caller's non-/tmp TMPDIR, or creates a unique non-/tmp fallback.
# - Uses `flock --close` (-o) so background GradleDaemon NEVER inherits the FD.
# - Stops stale daemon if TMPDIR or proxy changed across agents/reboots.
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SCRATCH_ROOT="/home/antigravity/.gemini/jetski/scratch/teamwork_tmp"
LOCK_FILE="${SCRATCH_ROOT}/songxi_gradle.lock"
LAST_TMPDIR_FILE="${SCRATCH_ROOT}/songxi_gradle_last_tmpdir"

mkdir -p "${SCRATCH_ROOT}" "/home/antigravity/.gradle/tmp"

# 1. Ensure caller-isolated non-/tmp TMPDIR before sourcing env-setup.sh
if [ -z "${TMPDIR:-}" ] || [[ "${TMPDIR}" == /tmp ]] || [[ "${TMPDIR}" == /tmp/* ]]; then
  export TMPDIR="${SCRATCH_ROOT}/gradlew_fallback_${PPID:-default}/tmp"
fi
mkdir -p "${TMPDIR}"
export TMP="${TMPDIR}"
export TEMP="${TMPDIR}"
export npm_config_tmp="${TMPDIR}"

# 2. Load JDK 17 + Android SDK + synchronized JAVA_TOOL_OPTIONS (tmpdir + trustStore + proxy)
REPO_ROOT="$(cd "${PROJECT_ROOT}/.." && pwd)"
if [ -f "${REPO_ROOT}/env-setup.sh" ]; then
  # shellcheck disable=SC1091
  source "${REPO_ROOT}/env-setup.sh"
elif [ -f "/home/antigravity/env-setup.sh" ]; then
  # shellcheck disable=SC1091
  source /home/antigravity/env-setup.sh
fi

# Move JAVA_TOOL_OPTIONS into GRADLE_OPTS so child KotlinCompileDaemon stdout/stderr is clean
export GRADLE_OPTS="${GRADLE_OPTS:-} ${JAVA_TOOL_OPTIONS:-}"
unset JAVA_TOOL_OPTIONS

# 3. Acquire exclusive workspace mutex (`--close` prevents GradleDaemon from inheriting FD)
cd "${PROJECT_ROOT}"
exec flock --close "${LOCK_FILE}" bash -c '
  set -euo pipefail
  LAST_FILE="$1"
  shift
  CUR_SIG="${TMPDIR}|${HTTPS_PROXY:-${https_proxy:-}}"
  PREV_SIG="$(cat "${LAST_FILE}" 2>/dev/null || true)"
  if [ -n "${PREV_SIG}" ] && [ "${PREV_SIG}" != "${CUR_SIG}" ]; then
    ./gradlew --stop >/dev/null 2>&1 || true
  fi
  printf "%s" "${CUR_SIG}" > "${LAST_FILE}"
  exec ./gradlew --max-workers=1 "$@"
' _ "${LAST_TMPDIR_FILE}" "$@"
