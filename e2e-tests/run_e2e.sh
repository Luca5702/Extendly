#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# 松息 (Sōngxī) — Unified Opaque-Box End-to-End Test Runner
# Supports:
#   ./run_e2e.sh [--tier <1|2|3|4|all>]
#   ./run_e2e.sh --verify-harness [--m1]
#   ./run_e2e.sh --verify-m1
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_ROOT="/home/antigravity"
ANDROID_PROJECT_DIR="${WORKSPACE_ROOT}/songxi-android"
LOCKED_GRADLEW="${ANDROID_PROJECT_DIR}/scripts/locked_gradlew.sh"
ANDROID_E2E_DIR="${SCRIPT_DIR}/android/src/test/kotlin/com/songxi/e2e"
PROTOTYPE_E2E_DIR="${SCRIPT_DIR}/prototype"

# ==============================================================================
# 1. Strict 5-Layer Non-/tmp Isolation & Deterministic Timezone/TLS Setup
# ==============================================================================
export SCRATCH_ROOT="/home/antigravity/.gemini/jetski/scratch/teamwork_tmp/e2e_runner"
export SCRATCH_TMP="${SCRATCH_ROOT}/tmp"
export PW_RUNTIME_DIR="/home/antigravity/.gemini/jetski/scratch/teamwork_tmp/e2e_pw_runtime"
mkdir -p \
  "${SCRATCH_TMP}" \
  "${SCRATCH_TMP}/node-compile-cache" \
  "${SCRATCH_ROOT}/android-user" \
  "${SCRATCH_ROOT}/npm-cache" \
  "${PW_RUNTIME_DIR}"

export TMPDIR="${SCRATCH_TMP}"
export TMP="${SCRATCH_TMP}"
export TEMP="${SCRATCH_TMP}"
export TZ="Asia/Shanghai"
export NODE_EXTRA_CA_CERTS="/etc/ssl/certs/ca-certificates.crt"

export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
# -XX:-UsePerfData prevents OpenJDK Hotspot from writing /tmp/hsperfdata_<user>/<pid>
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=${SCRATCH_TMP} -XX:-UsePerfData"
export JDK_JAVA_OPTIONS="-Djava.io.tmpdir=${SCRATCH_TMP} -XX:-UsePerfData"
export GRADLE_OPTS="-Djava.io.tmpdir=${SCRATCH_TMP} -XX:-UsePerfData"

export ANDROID_HOME="/home/antigravity/.android-sdk"
export ANDROID_SDK_ROOT="/home/antigravity/.android-sdk"
export ANDROID_USER_HOME="${SCRATCH_ROOT}/android-user"
export GRADLE_USER_HOME="/home/antigravity/.gradle"
export PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools/35.0.0:${JAVA_HOME}/bin:${PATH}"

export npm_config_tmp="${SCRATCH_TMP}"
export npm_config_cache="${SCRATCH_ROOT}/npm-cache"
export DISABLE_V8_COMPILE_CACHE=1
export NODE_COMPILE_CACHE="${SCRATCH_TMP}/node-compile-cache"
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
export CHROMIUM_BIN="/usr/lib/chromium/chromium"
export NODE_PATH="${PW_RUNTIME_DIR}/node_modules"

# ==============================================================================
# 2. CLI Argument Parsing
# ==============================================================================
MODE="run"
TIER="all"
VERIFY_PHASE="full" # "full" (E2E-M2 / TEST_READY gate) or "m1" (E2E-M1 gate)

usage() {
  cat >&2 <<'EOF'
Usage:
  run_e2e.sh [--tier <1|2|3|4|all>]
  run_e2e.sh --verify-harness [--m1]
  run_e2e.sh --verify-m1
EOF
  exit 2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tier)
      [[ $# -ge 2 ]] || usage
      TIER="$2"
      shift 2
      ;;
    --tier=*)
      TIER="${1#*=}"
      shift
      ;;
    --verify-harness)
      MODE="verify"
      shift
      ;;
    --verify-m1)
      MODE="verify"
      VERIFY_PHASE="m1"
      shift
      ;;
    --m1)
      VERIFY_PHASE="m1"
      shift
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "[ERROR] Unknown argument: $1" >&2
      usage
      ;;
  esac
done

if [[ "${MODE}" == "run" ]]; then
  case "${TIER}" in
    1|2|3|4|all) ;;
    *)
      echo "[ERROR] Invalid --tier value: '${TIER}' (expected 1|2|3|4|all)" >&2
      usage
      ;;
  esac
fi

# ==============================================================================
# 3. Daemon-Safe /tmp Before/After Cleanliness Verification
# ==============================================================================
RUN_ID="$$_$(date +%s%N)"
TMP_SNAP_BEFORE="${SCRATCH_TMP}/tmp_before_${RUN_ID}.txt"
TMP_SNAP_AFTER="${SCRATCH_TMP}/tmp_after_${RUN_ID}.txt"

cleanup_run_scratch() {
  rm -f \
    "${TMP_SNAP_BEFORE}" \
    "${TMP_SNAP_AFTER}" \
    "${SCRATCH_TMP}/pw_list_${RUN_ID}.json" \
    "${SCRATCH_TMP}/pw_report_${RUN_ID}.json" \
    "${SCRATCH_TMP}/gradle_marker_${RUN_ID}" \
    2>/dev/null || true
}
trap cleanup_run_scratch EXIT

snapshot_system_tmp() {
  ls -1A /tmp 2>/dev/null \
    | grep -Ev '^(agy-plugin-proxy\.|agy-plugin-tokens-|exebox-escape-|initgoogle_syslog_dir\.|\.jetski-proxy-init-ran$|shell\.sock$)' \
    | sort || true
}

verify_tmp_cleanliness() {
  snapshot_system_tmp > "${TMP_SNAP_AFTER}"
  local added
  added="$(comm -13 "${TMP_SNAP_BEFORE}" "${TMP_SNAP_AFTER}" || true)"
  if [[ -n "${added}" ]]; then
    echo "[FAIL] /tmp cleanliness violated — new entries created in /tmp:" >&2
    echo "${added}" >&2
    return 1
  fi
  local leaked
  leaked="$(find /tmp -maxdepth 2 -user "$(id -u)" 2>/dev/null \
    | grep -Ei '/tmp/(songxi|robolectric|roborazzi|\.org\.chromium|playwright|puppeteer|gradle|kotlin-daemon|datastore|aapt2|apksigner|npm-)' || true)"
  if [[ -n "${leaked}" ]]; then
    echo "[FAIL] /tmp contains forbidden build/browser/JVM temp artifacts:" >&2
    echo "${leaked}" >&2
    return 1
  fi
  local fresh_perf_or_v8
  fresh_perf_or_v8="$(find /tmp/hsperfdata_antigravity /tmp/v8-compile-cache* -newer "${TMP_SNAP_BEFORE}" 2>/dev/null || true)"
  if [[ -n "${fresh_perf_or_v8}" ]]; then
    echo "[FAIL] JVM/Node wrote new perfdata or v8 cache entries into /tmp during run:" >&2
    echo "${fresh_perf_or_v8}" >&2
    return 1
  fi
  local shm_leaked
  shm_leaked="$(find /dev/shm -maxdepth 1 -user "$(id -u)" -name 'songxi_chr_sock_*' 2>/dev/null || true)"
  if [[ -n "${shm_leaked}" ]]; then
    echo "[FAIL] /dev/shm contains uncleaned Chromium socket directories:" >&2
    echo "${shm_leaked}" >&2
    return 1
  fi
  echo "[OK] /tmp cleanliness verified (0 new entries, 0 project temp artifacts)."
  return 0
}

snapshot_system_tmp > "${TMP_SNAP_BEFORE}"

# ==============================================================================
# 4. Idempotent playwright-core Bootstrap (Zero node_modules in e2e-tests/)
# ==============================================================================
ensure_playwright_runtime() {
  if [[ -d "${SCRIPT_DIR}/node_modules" ]] || [[ -d "${PROTOTYPE_E2E_DIR}/node_modules" ]]; then
    echo "[FAIL] Forbidden node_modules found inside ${SCRIPT_DIR}; runtime must stay in ${PW_RUNTIME_DIR}." >&2
    return 1
  fi
  if [[ ! -d "${PW_RUNTIME_DIR}/node_modules/playwright-core" ]]; then
    (
      flock -x 200
      if [[ ! -d "${PW_RUNTIME_DIR}/node_modules/playwright-core" ]]; then
        echo "[INFO] Bootstrapping playwright-core into ${PW_RUNTIME_DIR} (--no-bin-links for virtiofs)..."
        [[ -f "${PW_RUNTIME_DIR}/package.json" ]] || printf '{"private":true}\n' > "${PW_RUNTIME_DIR}/package.json"
        PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 npm install \
          --prefix "${PW_RUNTIME_DIR}" \
          --no-bin-links --no-save --no-audit --no-fund --loglevel=error \
          playwright-core
      fi
    ) 200>"${PW_RUNTIME_DIR}.lock"
  fi
}

# ==============================================================================
# 5. Opaque-Box Static Import & Internal Symbol Coupling Guard
# ==============================================================================
check_opaque_box_hygiene() {
  local phase="$1"
  local rc=0

  if [[ -d "${SCRIPT_DIR}/node_modules" ]] || [[ -d "${PROTOTYPE_E2E_DIR}/node_modules" ]]; then
    echo "[FAIL] node_modules directory detected inside ${SCRIPT_DIR}." >&2
    rc=1
  fi

  local required_kt=(
    "${ANDROID_E2E_DIR}/SongxiAndroidE2EHarness.kt"
    "${ANDROID_E2E_DIR}/Tier1FeatureE2ETest.kt"
    "${ANDROID_E2E_DIR}/Tier2BoundaryE2ETest.kt"
  )
  if [[ "${phase}" == "full" ]]; then
    required_kt+=(
      "${ANDROID_E2E_DIR}/Tier3PairwiseE2ETest.kt"
      "${ANDROID_E2E_DIR}/Tier4ScenarioE2ETest.kt"
    )
  fi

  for kt_file in "${required_kt[@]}"; do
    if [[ ! -f "${kt_file}" ]]; then
      echo "[FAIL] Missing required Android E2E source file (${phase} gate): ${kt_file}" >&2
      rc=1
    fi
  done

  if [[ -d "${ANDROID_E2E_DIR}" ]]; then
    local import_hits
    import_hits="$(rg -n '^\s*import\s+com\.songxi\.app(\.|\b)' "${ANDROID_E2E_DIR}" 2>/dev/null || true)"
    if [[ -n "${import_hits}" ]]; then
      echo "[FAIL] Forbidden internal 'import com.songxi.app.*' detected in Android E2E sources:" >&2
      echo "${import_hits}" >&2
      rc=1
    fi

    if ! python3 - "${ANDROID_E2E_DIR}" <<'PY'
import pathlib, re, sys

e2e_dir = pathlib.Path(sys.argv[1])
violations = []

def strip_strings_and_comments(src: str) -> str:
    # Remove block comments, line comments, triple-quoted strings, and standard string literals
    pattern = re.compile(
        r'/\*[\s\S]*?\*/|//[^\n]*|"""[\s\S]*?"""|"(?:\\.|[^"\\])*"',
        re.MULTILINE,
    )
    return pattern.sub('""', src)

for kt in sorted(e2e_dir.glob("*.kt")):
    raw = kt.read_text(encoding="utf-8")
    sanitized = strip_strings_and_comments(raw)
    for idx, line in enumerate(sanitized.splitlines(), start=1):
        if re.search(r'\bcom\.songxi\.app\b', line):
            violations.append(f"{kt.name}:{idx}: {line.strip()}")

if violations:
    print("[FAIL] Forbidden non-literal com.songxi.app symbol reference(s):", file=sys.stderr)
    for v in violations:
        print("  " + v, file=sys.stderr)
    sys.exit(1)
PY
    then
      rc=1
    fi
  fi

  if [[ "${rc}" -eq 0 ]]; then
    echo "[OK] Opaque-box hygiene verified (0 com.songxi.app.* imports/symbols, 0 node_modules in e2e-tests/)."
  fi
  return "${rc}"
}

# ==============================================================================
# 6. Mode A: --verify-harness [--m1]
# ==============================================================================
if [[ "${MODE}" == "verify" ]]; then
  echo "=============================================================================="
  echo "[VERIFY-HARNESS] Phase: ${VERIFY_PHASE} (松息 Sōngxī E2E Infrastructure Gate)"
  echo "=============================================================================="
  VERIFY_RC=0

  # Step V1: Static opaque-box check
  if ! check_opaque_box_hygiene "${VERIFY_PHASE}"; then
    VERIFY_RC=1
  fi

  # Step V2: Playwright runtime + module syntax + real Headless Chromium smoke check + descriptor dump
  PW_LIST_JSON="${SCRATCH_TMP}/pw_list_${RUN_ID}.json"
  if ! ensure_playwright_runtime; then
    VERIFY_RC=1
  else
    set +e
    node "${PROTOTYPE_E2E_DIR}/run_prototype_e2e.cjs" \
      --verify-harness \
      --phase "${VERIFY_PHASE}" \
      --list-json "${PW_LIST_JSON}"
    PW_VERIFY_RC=$?
    set -e
    if [[ "${PW_VERIFY_RC}" -ne 0 ]]; then
      echo "[FAIL] Prototype --verify-harness (${VERIFY_PHASE}) exited with status ${PW_VERIFY_RC}." >&2
      VERIFY_RC=1
    fi
  fi

  # Step V3: Programmatic Per-Tier & Per-Feature (F1..F15) Count & Non-Vacuity Audit
  set +e
  python3 - "${ANDROID_E2E_DIR}" "${PW_LIST_JSON}" "${VERIFY_PHASE}" <<'PY'
import json
import pathlib
import re
import sys

android_dir = pathlib.Path(sys.argv[1])
pw_json_path = pathlib.Path(sys.argv[2])
phase = sys.argv[3]

errors = []
features = [f"F{i}" for i in range(1, 16)]

android_counts = {1: {f: 0 for f in features}, 2: {f: 0 for f in features}, 3: 0, 4: 0}
proto_counts = {1: {f: 0 for f in features}, 2: {f: 0 for f in features}, 3: 0, 4: 0}

ASSERT_TOKENS = (
    "assert",
    "assertTagDisplayed",
    "assertMinTouchBounds",
    "assertValidSignedReleaseAndDebugApks",
    "assertCleanManifestPermissions",
    "assertValidAndDistinctSkinPngs",
    "check(",
    "checkNotNull(",
    "fail(",
)

TAUTOLOGY_PATTERNS = [
    re.compile(r"\bassertTrue\s*\(\s*(?:[^,)]+,\s*)?true\s*\)"),
    re.compile(r"\bassertFalse\s*\(\s*(?:[^,)]+,\s*)?false\s*\)"),
    re.compile(r"\bassertEquals\s*\(\s*(?:true\s*,\s*true|false\s*,\s*false|1\s*,\s*1|0\s*,\s*0)\s*\)"),
]

def extract_kotlin_tests(kt_path: pathlib.Path):
    if not kt_path.is_file():
        return []
    src = kt_path.read_text(encoding="utf-8")
    if re.search(r"@\s*(?:org\.junit\.)?(?:Ignore|Disabled)\b", src):
        errors.append(f"{kt_path.name}: contains forbidden @Ignore/@Disabled annotation")

    results = []
    # Locate every @Test annotated fun
    test_iter = re.finditer(r"@Test\b[\s\S]*?\bfun\s+([A-Za-z0-9_`]+)\s*\([^)]*\)\s*\{", src)
    for m in test_iter:
        fn_name = m.group(1).strip("`")
        brace_start = m.end() - 1
        depth = 0
        pos = brace_start
        in_str = False
        in_char = False
        in_line_comment = False
        in_block_comment = False
        while pos < len(src):
            ch = src[pos]
            nxt = src[pos + 1] if pos + 1 < len(src) else ""
            if in_line_comment:
                if ch == "\n":
                    in_line_comment = False
            elif in_block_comment:
                if ch == "*" and nxt == "/":
                    in_block_comment = False
                    pos += 1
            elif in_str:
                if ch == "\\":
                    pos += 1
                elif ch == '"':
                    in_str = False
            elif in_char:
                if ch == "\\":
                    pos += 1
                elif ch == "'":
                    in_char = False
            else:
                if ch == "/" and nxt == "/":
                    in_line_comment = True
                    pos += 1
                elif ch == "/" and nxt == "*":
                    in_block_comment = True
                    pos += 1
                elif ch == '"':
                    in_str = True
                elif ch == "'":
                    in_char = True
                elif ch == "{":
                    depth += 1
                elif ch == "}":
                    depth -= 1
                    if depth == 0:
                        body = src[brace_start + 1 : pos]
                        results.append((fn_name, body))
                        break
            pos += 1
    return results

tier_kt_map = {
    1: android_dir / "Tier1FeatureE2ETest.kt",
    2: android_dir / "Tier2BoundaryE2ETest.kt",
    3: android_dir / "Tier3PairwiseE2ETest.kt",
    4: android_dir / "Tier4ScenarioE2ETest.kt",
}

seen_kt_names = set()
for tier_num, kt_file in tier_kt_map.items():
    if not kt_file.is_file():
        if tier_num in (1, 2) or phase == "full":
            errors.append(f"Missing Kotlin suite for Tier {tier_num}: {kt_file.name}")
        continue
    tests = extract_kotlin_tests(kt_file)
    for fn_name, body in tests:
        if fn_name in seen_kt_names:
            errors.append(f"Duplicate Kotlin @Test name: {fn_name}")
        seen_kt_names.add(fn_name)

        non_comment_lines = [
            ln.strip()
            for ln in body.splitlines()
            if ln.strip() and not ln.strip().startswith("//")
        ]
        if len(non_comment_lines) < 2:
            errors.append(f"{kt_file.name}::{fn_name} has fewer than 2 non-comment lines")
        if not any(tok in body for tok in ASSERT_TOKENS):
            errors.append(f"{kt_file.name}::{fn_name} has no assertion/verification call")
        for pat in TAUTOLOGY_PATTERNS:
            if pat.search(body):
                errors.append(f"{kt_file.name}::{fn_name} contains tautological assertion")

        if tier_num in (1, 2):
            fm = re.match(rf"^t{tier_num}_f(0[1-9]|1[0-5])_\d+_", fn_name)
            if not fm:
                errors.append(
                    f"{kt_file.name}::{fn_name} does not match naming convention t{tier_num}_f<01..15>_<NN>_*"
                )
            else:
                feat_key = f"F{int(fm.group(1))}"
                android_counts[tier_num][feat_key] += 1
        elif tier_num == 3:
            android_counts[3] += 1
        elif tier_num == 4:
            android_counts[4] += 1

if pw_json_path.is_file():
    pw_items = json.loads(pw_json_path.read_text(encoding="utf-8"))
    for item in pw_items:
        t = int(item.get("tier", 0))
        tid = item.get("id", "<unknown>")
        if not item.get("hasAssertion", False) or int(item.get("bodyLength", 0)) < 50:
            errors.append(f"Prototype test {tid} failed non-vacuity check")
        if t in (1, 2):
            feat = item.get("feature", "")
            if feat not in proto_counts[t]:
                errors.append(f"Prototype test {tid} has invalid feature '{feat}'")
            else:
                proto_counts[t][feat] += 1
        elif t == 3:
            proto_counts[3] += 1
        elif t == 4:
            proto_counts[4] += 1
else:
    errors.append(f"Missing Prototype descriptor list JSON: {pw_json_path}")

print("\nPer-Feature Coverage Audit (Tiers 1 & 2):")
print("Feature | T1 Android | T1 Proto | T1 Total | T2 Android | T2 Proto | T2 Total")
print("--------+------------+----------+----------+------------+----------+---------")
t1_android_sum = 0
t1_proto_sum = 0
t2_android_sum = 0
t2_proto_sum = 0

for f in features:
    a1 = android_counts[1][f]
    p1 = proto_counts[1][f]
    tot1 = a1 + p1
    a2 = android_counts[2][f]
    p2 = proto_counts[2][f]
    tot2 = a2 + p2
    t1_android_sum += a1
    t1_proto_sum += p1
    t2_android_sum += a2
    t2_proto_sum += p2
    print(f"{f:<7} | {a1:>10} | {p1:>8} | {tot1:>8} | {a2:>10} | {p2:>8} | {tot2:>8}")
    if tot1 < 5:
        errors.append(f"Tier 1 {f} total is {tot1} (< 5 minimum)")
    if tot2 < 5:
        errors.append(f"Tier 2 {f} total is {tot2} (< 5 minimum)")

t1_total = t1_android_sum + t1_proto_sum
t2_total = t2_android_sum + t2_proto_sum
t3_total = android_counts[3] + proto_counts[3]
t4_total = android_counts[4] + proto_counts[4]
grand_total = t1_total + t2_total + t3_total + t4_total

print("--------+------------+----------+----------+------------+----------+---------")
print(
    f"TOTAL   | {t1_android_sum:>10} | {t1_proto_sum:>8} | {t1_total:>8} | "
    f"{t2_android_sum:>10} | {t2_proto_sum:>8} | {t2_total:>8}"
)
print(
    f"Tier 3 Pairwise : Android={android_counts[3]}, Prototype={proto_counts[3]}, Total={t3_total} "
    f"(required={'20' if phase == 'full' else 'deferred to E2E-M2'})"
)
print(
    f"Tier 4 Scenarios: Android={android_counts[4]}, Prototype={proto_counts[4]}, Total={t4_total} "
    f"(required={'10' if phase == 'full' else 'deferred to E2E-M2'})"
)
print(f"Grand Total     : {grand_total}\n")

if t1_android_sum <= 0 or t1_proto_sum <= 0 or t1_total < 75:
    errors.append(f"Tier 1 threshold failed: total={t1_total} (expected >= 75 with Android>0 & Proto>0)")
if t2_android_sum <= 0 or t2_proto_sum <= 0 or t2_total < 75:
    errors.append(f"Tier 2 threshold failed: total={t2_total} (expected >= 75 with Android>0 & Proto>0)")

if phase == "full":
    if android_counts[3] <= 0 or proto_counts[3] <= 0 or t3_total < 20:
        errors.append(f"Tier 3 threshold failed: total={t3_total} (expected >= 20 with Android>0 & Proto>0)")
    if android_counts[4] <= 0 or proto_counts[4] <= 0 or t4_total < 10:
        errors.append(f"Tier 4 threshold failed: total={t4_total} (expected >= 10 with Android>0 & Proto>0)")
    if grand_total < 180:
        errors.append(f"Grand total threshold failed: total={grand_total} (expected >= 180)")

if errors:
    print(f"[FAIL] Programmatic count / non-vacuity gate failed ({phase}):", file=sys.stderr)
    for e in errors:
        print(f"  - {e}", file=sys.stderr)
    sys.exit(1)

print(f"[OK] Programmatic count & non-vacuity audit passed ({phase}: {grand_total} genuine E2E tests).")
PY
  COUNT_AUDIT_RC=$?
  set -e
  if [[ "${COUNT_AUDIT_RC}" -ne 0 ]]; then
    VERIFY_RC=1
  fi

  # Step V4: Gradle :app:compileDebugUnitTestKotlin -Psongxi.e2e=true + .class existence verification
  if [[ -x "${LOCKED_GRADLEW}" ]] && [[ -x "${ANDROID_PROJECT_DIR}/gradlew" ]]; then
    echo "[INFO] Compiling Android E2E Kotlin suite via locked_gradlew.sh (-Psongxi.e2e=true)..."
    set +e
    "${LOCKED_GRADLEW}" :app:compileDebugUnitTestKotlin -Psongxi.e2e=true
    COMPILE_RC=$?
    set -e
    if [[ "${COMPILE_RC}" -ne 0 ]]; then
      echo "[FAIL] :app:compileDebugUnitTestKotlin -Psongxi.e2e=true failed with exit code ${COMPILE_RC}." >&2
      VERIFY_RC=1
    else
      REQUIRED_CLASSES=(
        "SongxiAndroidE2EHarness.class"
        "Tier1FeatureE2ETest.class"
        "Tier2BoundaryE2ETest.class"
      )
      if [[ "${VERIFY_PHASE}" == "full" ]]; then
        REQUIRED_CLASSES+=(
          "Tier3PairwiseE2ETest.class"
          "Tier4ScenarioE2ETest.class"
        )
      fi
      for cls in "${REQUIRED_CLASSES[@]}"; do
        hit="$(find "${ANDROID_PROJECT_DIR}/app/build" -name "${cls}" 2>/dev/null | head -n 1 || true)"
        if [[ -z "${hit}" ]]; then
          echo "[FAIL] Expected compiled class '${cls}' not found under ${ANDROID_PROJECT_DIR}/app/build (check -Psongxi.e2e=true sourceSets wiring)." >&2
          VERIFY_RC=1
        fi
      done
      if [[ "${VERIFY_RC}" -eq 0 ]]; then
        echo "[OK] Kotlin E2E compilation & .class output verification passed (${REQUIRED_CLASSES[*]})."
      fi
    fi
  else
    if [[ "${VERIFY_PHASE}" == "full" ]]; then
      echo "[FAIL] Full --verify-harness requires executable ${LOCKED_GRADLEW} and ${ANDROID_PROJECT_DIR}/gradlew." >&2
      VERIFY_RC=1
    else
      echo "[M1-PENDING] ${LOCKED_GRADLEW} or gradlew not yet finalized by parallel M1 milestone; skipping Gradle compile in --m1 mode."
    fi
  fi

  # Step V5: /tmp cleanliness verification
  if ! verify_tmp_cleanliness; then
    VERIFY_RC=1
  fi
  cleanup_run_scratch

  if [[ "${VERIFY_RC}" -ne 0 ]]; then
    echo "[FAIL] --verify-harness (${VERIFY_PHASE}) FAILED." >&2
    exit 1
  fi
  echo "[PASS] --verify-harness (${VERIFY_PHASE}) PASSED 100%."
  exit 0
fi

# ==============================================================================
# 7. Mode B: Progressive / Full E2E Execution (--tier 1|2|3|4|all)
# ==============================================================================
echo "=============================================================================="
echo "[RUN-E2E] Executing 松息 (Sōngxī) Opaque-Box E2E Suite — Tier: ${TIER}"
echo "=============================================================================="

if [[ ! -x "${LOCKED_GRADLEW}" ]] || [[ ! -x "${ANDROID_PROJECT_DIR}/gradlew" ]]; then
  echo "[FAIL] Missing executable Gradle wrapper: ${LOCKED_GRADLEW}" >&2
  exit 1
fi

check_opaque_box_hygiene "$( [[ "${TIER}" == "1" || "${TIER}" == "2" ]] && echo "m1" || echo "full" )"
ensure_playwright_runtime

case "${TIER}" in
  1)
    GRADLE_TEST_FILTER="com.songxi.e2e.Tier1FeatureE2ETest"
    TIER_MIN_TOTAL=75
    EXPECTED_SUITES=1
    ;;
  2)
    GRADLE_TEST_FILTER="com.songxi.e2e.Tier2BoundaryE2ETest"
    TIER_MIN_TOTAL=75
    EXPECTED_SUITES=1
    ;;
  3)
    GRADLE_TEST_FILTER="com.songxi.e2e.Tier3PairwiseE2ETest"
    TIER_MIN_TOTAL=20
    EXPECTED_SUITES=1
    ;;
  4)
    GRADLE_TEST_FILTER="com.songxi.e2e.Tier4ScenarioE2ETest"
    TIER_MIN_TOTAL=10
    EXPECTED_SUITES=1
    ;;
  all)
    GRADLE_TEST_FILTER="com.songxi.e2e.Tier*"
    TIER_MIN_TOTAL=180
    EXPECTED_SUITES=4
    ;;
esac

# Step R1: Build fresh debug + signed release APKs & ensure Roborazzi 4-skin PNGs exist
echo "[INFO] Ensuring fresh app-debug.apk and signed app-release.apk via locked_gradlew.sh..."
"${LOCKED_GRADLEW}" assembleDebug assembleRelease

ROBORAZZI_SKIN_COUNT="$(python3 - "${ANDROID_PROJECT_DIR}" <<'PY'
import pathlib, sys
root = pathlib.Path(sys.argv[1])
keys = ["ink_mist", "qinglv_celadon", "dusk_pine", "spring_blossom"]
pngs = [
    p for p in root.rglob("*.png")
    if "/src/main/res/" not in p.as_posix().lower()
    and any(tok in p.as_posix().lower() for tok in ("roborazzi", "snapshot", "screenshot", "skin"))
]
found = sum(1 for k in keys if any(k in p.name.lower() or k.replace("_", "") in p.name.lower().replace("-", "").replace("_", "") for p in pngs))
print(found)
PY
)"
if [[ "${ROBORAZZI_SKIN_COUNT}" -lt 4 ]]; then
  echo "[INFO] Recording Roborazzi 4-skin PNG captures via locked_gradlew.sh..."
  "${LOCKED_GRADLEW}" recordRoborazziDebug || "${LOCKED_GRADLEW}" testDebugUnitTest -Droborazzi.test.record=true || true
fi

# Step R2: Execute Android Robolectric + Compose UI E2E tier(s)
MARKER="${SCRATCH_TMP}/gradle_marker_${RUN_ID}"
touch "${MARKER}"

echo "[INFO] Running Android E2E tests (${GRADLE_TEST_FILTER})..."
set +e
"${LOCKED_GRADLEW}" :app:testDebugUnitTest \
  -Psongxi.e2e=true \
  --tests "${GRADLE_TEST_FILTER}" \
  --rerun
ANDROID_GRADLE_RC=$?
set -e

# Step R3: Execute Playwright Prototype + Brain Artifact E2E tier(s)
PW_REPORT="${SCRATCH_TMP}/pw_report_${RUN_ID}.json"
echo "[INFO] Running Playwright Prototype & Artifact E2E tests (--tier ${TIER})..."
set +e
node "${PROTOTYPE_E2E_DIR}/run_prototype_e2e.cjs" \
  --tier "${TIER}" \
  --json-report "${PW_REPORT}"
PROTO_RC=$?
set -e

# Step R4: Parse fresh JUnit XMLs + Playwright JSON report & enforce strict non-empty pass contract
XML_DIR="${ANDROID_PROJECT_DIR}/app/build/test-results/testDebugUnitTest"
set +e
python3 - \
  "${XML_DIR}" \
  "${MARKER}" \
  "${EXPECTED_SUITES}" \
  "${ANDROID_GRADLE_RC}" \
  "${PW_REPORT}" \
  "${PROTO_RC}" \
  "${TIER_MIN_TOTAL}" \
  "${TIER}" <<'PY'
import json
import os
import pathlib
import sys
import xml.etree.ElementTree as ET

xml_dir = pathlib.Path(sys.argv[1])
marker_mtime = os.path.getmtime(sys.argv[2])
expected_suites = int(sys.argv[3])
android_rc = int(sys.argv[4])
pw_report_path = pathlib.Path(sys.argv[5])
proto_rc = int(sys.argv[6])
tier_min_total = int(sys.argv[7])
tier_label = sys.argv[8]

errors = []
failure_details = []

fresh_xmls = []
if xml_dir.is_dir():
    for p in sorted(xml_dir.glob("TEST-com.songxi.e2e.Tier*.xml")):
        if os.path.getmtime(p) >= marker_mtime - 0.05:
            fresh_xmls.append(p)

suites_found = len(fresh_xmls)
android_total = 0
android_failed = 0
android_errors = 0
android_skipped = 0

for xp in fresh_xmls:
    root = ET.parse(xp).getroot()
    suites = [root] if root.tag == "testsuite" else list(root.findall("testsuite"))
    for ts in suites:
        android_total += int(ts.attrib.get("tests", "0"))
        android_failed += int(ts.attrib.get("failures", "0"))
        android_errors += int(ts.attrib.get("errors", "0"))
        android_skipped += int(ts.attrib.get("skipped", "0"))
        for tc in ts.findall("testcase"):
            cls_name = tc.attrib.get("classname", "")
            tc_name = tc.attrib.get("name", "")
            for tag in ("failure", "error"):
                el = tc.find(tag)
                if el is not None:
                    msg = (el.attrib.get("message") or (el.text or "")).strip().splitlines()
                    first_line = msg[0] if msg else tag
                    failure_details.append(f"[Android] {cls_name}.{tc_name}: {first_line}")

if android_rc != 0:
    errors.append(f"Android Gradle test task exited with non-zero code {android_rc}")
if suites_found != expected_suites:
    errors.append(f"Expected {expected_suites} fresh JUnit XML suite(s), found {suites_found}")
if android_total <= 0:
    errors.append("Android executed 0 tests (empty filter guard)")
if android_failed > 0 or android_errors > 0:
    errors.append(f"Android had {android_failed} failure(s) and {android_errors} error(s)")
if android_skipped > 0:
    errors.append(f"Android had {android_skipped} skipped test(s) (strictly 0 skipped allowed)")

proto_total = 0
proto_passed = 0
proto_failed = 0
proto_skipped = 0

if pw_report_path.is_file():
    rep = json.loads(pw_report_path.read_text(encoding="utf-8"))
    proto_total = int(rep.get("total", 0))
    proto_passed = int(rep.get("passed", 0))
    proto_failed = int(rep.get("failed", 0))
    proto_skipped = int(rep.get("skipped", 0))
    for f in rep.get("failures", []):
        failure_details.append(f"[Prototype] {f.get('id')} ({f.get('name')}): {f.get('error')}")
else:
    errors.append(f"Missing Playwright JSON report at {pw_report_path}")

if proto_rc != 0:
    errors.append(f"Prototype runner exited with non-zero code {proto_rc}")
if proto_total <= 0:
    errors.append("Prototype executed 0 tests")
if proto_failed > 0:
    errors.append(f"Prototype had {proto_failed} failure(s)")
if proto_skipped > 0:
    errors.append(f"Prototype had {proto_skipped} skipped test(s)")

combined_total = android_total + proto_total
if combined_total < tier_min_total:
    errors.append(f"Combined executed tests ({combined_total}) < tier minimum ({tier_min_total})")

print("\n==============================================================================")
print(f"Unified E2E Summary (Tier: {tier_label})")
print(f"  Android   : run={android_total}, fail={android_failed}, err={android_errors}, skip={android_skipped}")
print(f"  Prototype : run={proto_total}, pass={proto_passed}, fail={proto_failed}, skip={proto_skipped}")
print(f"  Combined  : {combined_total} executed (minimum required: {tier_min_total})")
print("==============================================================================")

if failure_details:
    print("\nFailure Diagnostics:", file=sys.stderr)
    for d in failure_details:
        print("  - " + d, file=sys.stderr)

if errors:
    print("\nGate Violations:", file=sys.stderr)
    for e in errors:
        print("  - " + e, file=sys.stderr)
    sys.exit(1)
PY
AGG_RC=$?
set -e

TMP_CLEAN_RC=0
verify_tmp_cleanliness || TMP_CLEAN_RC=$?
cleanup_run_scratch

if [[ "${AGG_RC}" -ne 0 ]] || [[ "${TMP_CLEAN_RC}" -ne 0 ]]; then
  echo "[FAIL] E2E Tier '${TIER}' FAILED." >&2
  exit 1
fi

echo "[PASS] E2E Tier '${TIER}' PASSED 100%."
exit 0
