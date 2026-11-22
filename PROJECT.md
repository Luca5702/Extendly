# Project: 松息 (Sōngxī) — Standalone Android App, Illustration × Shan-Shui Theme Engine & Interactive Product Suite

## Architecture

### 1. Product Vision & Core Doctrines
**松息 (Sōngxī)** (*"一松一息，身自舒展"*) is a calm, low-intrusion micro-stretching and sedentary-relief sanctuary designed for desk workers and screen-fatigued professionals. Grounded in `ORIGINAL_REQUEST.md` and authoritative benchmark/regulatory research, the system embodies seven architectural invariants:
1. **Confirmed Identity & Calm Poise**: Official brand **松息 (Sōngxī)** — healing (`治愈系`), low-intrusion (`低打扰`), tranquil (`平静`), approachable (`亲和`), classical elegance (`典雅`), high taste (`高级感`), simplicity (`简洁`), and Low Attention Tax (`不过分消耗注意力`, `≥45%` active whitespace, zero account/login walls).
2. **Illustration × Chinese Shan-Shui Replaceable Skin Architecture (`可替换皮肤 · 系统架构支持主题`)**: Foregrounds warm contemporary editorial illustration (clear, ergonomic stretching posture figures) against layered Chinese Shan-Shui landscape painting atmospheres (`远山 + 云霭 + 草木 + 宣纸/素绢肌理 + 朱砂印章`), driven by a **5-Dimensional `SongxiSkin` Contract** with **4 built-in 100% free skins**:
   - `ink_mist` (**水墨远山 · 素宣** — Default Day: Song ink wash ridges + warm linen line-and-wash figure + `432Hz` singing bowl)
   - `qinglv_celadon` (**青绿山居 · 汝瓷** — Flagship: *千里江山* mineral malachite/azurite gold-rimmed peaks + Ru celadon flat-textured figure + `528Hz` bamboo chime)
   - `dusk_pine` (**暮山听松 · 沉香** — Low-Glare Night: evening indigo ridges + crescent moon + luminous contour figure + `396Hz` guqin harmonic)
   - `spring_blossom` (**春涧花影 · 暖陶** — Warm Healing: warm mulberry paper + stream slopes & wild-peach twig + botanical gouache figure + `440Hz` bell & spring rain)
3. **Standalone Android App First (`先做独立Android app`)**: Native Kotlin `2.4.21` + AGP `9.5.2` (Built-in Kotlin) + Gradle `9.9.2` + JDK `21` + Jetpack Compose (`BOM 2026.11.01`, Compose `1.13.0` / Material3 `1.5.0`) standalone offline-first Android application (`com.songxi.app`, `minSdk = 26`, `targetSdk = 36`, `compileSdk = 37.2`), paired with a 1:1 synchronized Product Design Spec v2 and Standalone Android Interactive High-Fidelity Prototype v2 in the user-facing Brain Artifact directory.
4. **Zero Membership / Zero Paywalls (`不做会员功能`)**: 100% of routines, 6-zone custom prescriptions, 4 curated booklets (`四时雅集`), 4 skins (`山居画境`), soundscapes, reminders, and journals are permanently free. Legacy v1 concepts (`四时席`, `¥16/¥98/¥118`, VIP modals, WeChat Mini-Program-first framing, "零开屏广告" text) are completely purged.
5. **High-Taste Compliant Splash-Ad-Only Monetization (`只以开屏广告盈利 · 开屏雅鉴`)**: Monetizes **exclusively** via a gallery-framed `3s` Splash Ad Frontispiece (`78%` Xuan/silk framed creative + `22%` brand seal sanctuary footer). Enforces MIIT/CAA/PIPL compliance: unmistakable `广告 · 开屏雅鉴` badge, immediate `t=0ms` `≥48dp × 48dp` `跳过 3s` button, bounded CTA pill with safe confirmation sheet (inert backdrop, zero full-screen hot-zone traps), **strictly zero motion sensors (`禁用摇一摇`)**, muted audio, and a **7-Gate Calm Frequency Policy** (`1st-launch grace`, `active-session immunity`, `reminder quick-start bypass`, `<30m warm-switch suppression`, `≤3/day cap`, `≥4h cooldown`, `1200ms fetch timeout`, plus manual `预览开屏雅鉴` trigger). Zero in-app banner, feed, interstitial, or rewarded ads exist anywhere.
6. **3-Second Visual Comprehension → Eyes-Free Parasympathetic Flow (`4s Inhale / 6s Exhale`)**: Users glance at the skin-styled animated figure for 3 seconds, then rest or close their eyes (`闭眼余光态`), guided by procedural PCM harmonic chords and `VibrationEffect` breath waves, with `翻转静默暂存` (flip phone face-down onto desk to pause silently when a colleague approaches).
7. **Zero-Guilt Companionship (`松息手札 & 微风提醒`)**: Monthly water-ripple journal honors rest days (*"留白亦是休息"*, strictly zero streak counters or broken-streak shame), paired with gentle `WorkManager` sedentary reminders respecting lunch, night, and weekend quiet hours.

### 2. Multi-Module Acyclic Android Dependency Graph (`/home/antigravity/songxi-android`)
 Constructor DI (`SongxiAppContainer` + `CompositionLocal` / `ViewModelProvider.Factory`, zero KAPT/Hilt overhead):
- `:app` → `:feature:splashad`, `:feature:stretch`, `:feature:sanctuary`, `:core:designsystem`, `:core:data`, `:core:sensory`, `:core:model`
- `:feature:splashad` → `:core:designsystem`, `:core:data`, `:core:model` *(MUST NOT depend on `:core:sensory` or `SensorManager` — structurally enforces anti-`摇一摇` and silent splash invariants!)*
- `:feature:stretch` → `:core:designsystem`, `:core:data`, `:core:sensory`, `:core:model`
- `:feature:sanctuary` → `:core:designsystem`, `:core:data`, `:core:sensory`, `:core:model`
- `:core:designsystem`, `:core:data`, `:core:sensory` → `:core:model`
- `:core:model` → pure Kotlin + `kotlinx-serialization` (`java.time` via `minSdk 26`)

### 3. VM Environment, Disk Hygiene & Build Concurrency Rules (MANDATORY)
- **Strict `/tmp` Prohibition**: NEVER place builds, virtualenvs, temp files, Chromium profiles, or JVM temp files in `/tmp`. Every command running `sdkmanager`, `gradle`, `gradlew`, `java`, `node`, `npm`, or `chromium` MUST export:
  ```bash
  export SCRATCH_TMP="/home/antigravity/.gemini/jetski/scratch/teamwork_tmp/<agent_id>/tmp"
  mkdir -p "$SCRATCH_TMP"
  export TMPDIR="$SCRATCH_TMP" TMP="$SCRATCH_TMP" TEMP="$SCRATCH_TMP"
  export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
  export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=${SCRATCH_TMP}"
  export npm_config_tmp="$SCRATCH_TMP"
  ```
- **Shared Persistent Toolchain Paths (XFS `/dev/vdb`, 19 GB free)**:
  - `ANDROID_HOME=/home/antigravity/.android-sdk` (`ANDROID_SDK_ROOT=/home/antigravity/.android-sdk`)
  - `GRADLE_USER_HOME=/home/antigravity/.gradle`
  - Persisted idempotently in `/home/antigravity/env-setup.sh` and `/home/antigravity/.bashrc`.
- **2-vCPU / 7.8 GiB RAM / 0B Swap Protection, Sequential Milestones & Build/Browser Locks**:
  - Do **NOT** install or run Android Emulator (`/dev/kvm` is absent; `0B` swap). Use **Robolectric `4.14.1` (`@GraphicsMode(NATIVE)`, `sdk = [34]`) + Compose UI Test `1.7.6` + Roborazzi `1.39.0` + `work-testing:2.10.0`** for Android verification, and **`/usr/lib/chromium/chromium` + `playwright-core`** for HTML prototype verification.
  - **Strictly Sequential Sub-Orchestrators (Max 1 Active Sub-Orchestrator at a Time)**: Execute milestones strictly one at a time (`M2` → `M3` → `M4` → `M5` → `E2E` → `M6`) so headless Chromium and Gradle/Kotlin/Robolectric JVMs never coexist in RAM on `0B` swap.
  - **Tightened JVM Memory Caps (Enforced from `M3` Onward)**: Cap heaps in `gradle.properties` & root `build.gradle.kts`: `org.gradle.jvmargs=-Xmx1280m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8`, `kotlin.daemon.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=256m`, `org.gradle.workers.max=1`, `org.gradle.parallel=false`, `org.gradle.vfs.watch=false`, `org.gradle.daemon.idletimeout=120000` (or `--no-daemon`), test `maxHeapSize = "1024m"`, `maxParallelForks = 1`, keeping peak JVM RSS well below `4.5 GiB`.
  - **Exclusive Gradle & Chromium Locks + Strict Ban on Standalone `kotlinc`/`java`**: All agents invoking Gradle or running Kotlin/JVM/Robolectric compilations/tests on `/home/antigravity/songxi-android` MUST serialize execution exclusively via `/home/antigravity/songxi-android/scripts/locked_gradlew.sh` (`flock /home/antigravity/.gemini/jetski/scratch/teamwork_tmp/songxi_gradle.lock ./gradlew --max-workers=1 "$@"`). Challengers, Workers, Reviewers, Auditors, and Test Writers are **STRICTLY FORBIDDEN** from spawning standalone `kotlinc`, `K2JVMCompiler`, or `java` processes outside `locked_gradlew.sh` (out-of-tree Challenger probes MUST run inside `locked_gradlew.sh` via `-I "$PROBE_DIR/probe.init.gradle"` and clean up `$PROBE_DIR` before reporting). All agents launching headless `/usr/lib/chromium/chromium` during parallel verification waves MUST serialize browser execution via `flock /home/antigravity/.gemini/jetski/scratch/teamwork_tmp/songxi_chromium.lock` and close the browser immediately after checks.

---

## Code Layout

```text
/home/antigravity/
├── PROJECT.md                                  # Project Orchestrator — Global architecture, features, milestones, contracts
├── TEST_INFRA.md                               # E2E Testing Orchestrator — Opaque-box E2E methodology & scenario matrix
├── TEST_READY.md                               # E2E Testing Orchestrator — E2E suite completion signal & coverage report
├── env-setup.sh                                # M1 — Idempotent non-/tmp JDK/Android SDK/Gradle environment setup
├── .android-sdk/                               # M1 — Bootstrapped Android cmdline-tools, platforms;android-35 & 34, build-tools;35.0.0
├── docs/                                       # M2 — Canonical workspace Product Design v2, Style Board & Interactive Prototype v2
│   ├── SONGXI_PRODUCT_DESIGN.md                # M2 — Upgraded v2 Product Design Plan
│   ├── songxi_product_design.md                # M2 — Exact filename mirror for root brain publication & E2E verification
│   ├── SKIN_AND_SPLASH_SPEC.md                 # M2 — 5D SongxiSkin & 7-Gate Splash-Ad Specification
│   ├── songxi_skin_gallery_showcase.html       # M2 — Interactive Illustration × Shan-Shui 4-Skin Style Reference Board
│   └── songxi_interactive_prototype.html       # M2 — Upgraded v2 Standalone Android Interactive Prototype (4 skins + splash ad)
├── songxi-android/                             # M1, M3, M4, M5, M6 — Standalone Native Android Project (com.songxi.app)
│   ├── .gitignore                              # M1, M3 — Git ignore rules (including .kotlin/)
│   ├── settings.gradle.kts
│   ├── build.gradle.kts                        # M1, M3 — Root convention & tightened test maxHeapSize="1024m"
│   ├── gradle.properties                       # M1, M3 — Tightened JVM/Kotlin daemon heap & single-worker OOM caps
│   ├── gradle/libs.versions.toml
│   ├── scripts/locked_gradlew.sh               # M1, M3 — flock-serialized non-/tmp --max-workers=1 Gradle wrapper
│   ├── core/
│   │   ├── model/                              # M1 — Domain entities, enums, keyframes, ClockProvider
│   │   ├── data/                               # M1 — DataStore repositories, 19+ poses catalog, booklets, custom composer
│   │   ├── sensory/                            # M1 — PCM HarmonicAudioEngine, BreathingHapticEngine, FaceDownSensorMonitor
│   │   └── designsystem/                       # M3 — 5D SongxiSkin, 4 built-in skins, Shan-Shui & Figure Compose Canvas renderers
│   ├── feature/
│   │   ├── splashad/                           # M3 — 7-gate SplashAdPolicyEngine, CuratedHouseSplashAdProvider, SplashAdScreen
│   │   ├── stretch/                            # M4 — HomeScreen, BodyMapScreen, Booklets, StretchPlayerScreen, CompletionScreen
│   │   └── sanctuary/                          # M5 — SkinGalleryScreen, SedentaryReminderWorker/Screen, PracticeJournalScreen
│   └── app/                                    # M1 (stub) -> M5 (full) — MainActivity, SongxiAppContainer, NavHost, APK signing
└── e2e-tests/                                  # E2E Track — Opaque-box requirement-driven E2E test suite (Tiers 1–4)
    ├── run_e2e.sh                              # Unified E2E runner (--tier 1|2|3|4|all) returning exit code 0 on 100% pass
    ├── android/src/test/kotlin/com/songxi/e2e/ # Activated ONLY when `-Psongxi.e2e=true` is passed to Gradle
    └── prototype/                              # Playwright-core + /usr/lib/chromium/chromium E2E tests against /home/antigravity/docs/

User-Facing Cortex Brain Artifacts (canonical v2 files in `/home/antigravity/docs/*`, mirrored byte-identically with `.metadata.json` to `/home/antigravity/.gemini/jetski/brain/3d22de3812df3380df3042f3a0dcd19b/` in M2 AND POSIX-synced in M5 F15 to `/home/antigravity/.gemini/jetski/brain/v1/default/jetski_remote_agents_3p/83048e5b-9e9e-4924-8625-abfde348bd48/` alongside `songxi_brand_moodboard_1794840158556.jpg`; all E2E & Auditor checks target `/home/antigravity/docs/*`):
├── songxi_product_design.md
├── songxi_interactive_prototype.html
└── songxi_skin_gallery_showcase.html
```

---

## Feature Inventory

Every feature discovered during Step 0 Survey (`ORIGINAL_REQUEST.md` + Explorers 1–3) is cataloged below and assigned to explicit implementation milestones (**0 unassigned features**).

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | Zero-Login Local-First Sanctuary & Gentle Onboarding | Instant offline launch without account/login walls; transparent local-storage & splash-ad-only sanctuary note on first launch; persistent state via `DataStore`. | M1, M2, M5 | ORIGINAL_REQUEST, Survey 1–3 |
| F2 | Editorial Framed Splash Ad UI (`开屏雅鉴`) & Safe CTA | Exclusive revenue surface (`78%` Xuan/silk framed stage + `22%` brand seal footer), unmistakable `广告 · 开屏雅鉴` badge, `t=0ms` `≥48dp` `跳过 3s` button, muted audio, bounded CTA pill + safe external confirmation sheet (inert backdrop), **zero motion sensors (`禁用摇一摇`)**, `CuratedHouseSplashAdProvider` (3 artworks) + `SdkSplashAdBridgeProvider`. | M2, M3, M5 | ORIGINAL_REQUEST, Survey 1–3 |
| F3 | 7-Gate Calm Splash-Ad Policy & Privacy Controls | `SplashAdPolicyEngine` enforcing: (1) 1st-launch grace bypass, (2) active/paused session immunity, (3) reminder notification deep-link bypass, (4) `<30m` warm resume suppression, (5) `≤3/day` cap, (6) `≥4h` cooldown, (7) `1200ms` timeout fallback, plus PIPL personalization toggle & 1-tap `预览开屏雅鉴` manual trigger. | M2, M3, M5 | ORIGINAL_REQUEST, Survey 2–3 |
| F4 | Zero-Membership & 100% Unlocked Content Invariant | Complete architectural elimination of VIP/subscriptions (`四时席`, `¥16/¥98/¥118`), locked skins, locked booklets, daily custom limits, WeChat Mini-Program upsells, and in-app banner/interstitial/rewarded ads across Android APK, Design Doc v2, and Prototype v2. | M1, M2, M3, M4, M5 | ORIGINAL_REQUEST, Survey 1–3 |
| F5 | 5-Dimensional Illustration × Shan-Shui Skin Engine | Pluggable `SongxiSkin` + `LocalSongxiSkin` + `SongxiTheme` unifying (A) Paper/Silk Palette & Procedural Grain, (B) Layered `ShanShuiBackdropCanvas` (`远山 + 云霭 + 草木 + 日月`), (C) `EditorialStretchFigureCanvas` + `BreathingHaloCanvas` + `ClassicalSealBadge`, (D) Solar-Term Serif Copy, and (E) Harmonic Audio/Haptic Tokens. | M2, M3 | ORIGINAL_REQUEST, Survey 1–3 |
| F6 | 4 Free Built-In Skins, Live Gallery & Auto-Night Pairing | Four distinct free skins (`水墨远山 ink_mist`, `青绿山居 qinglv_celadon`, `暮山听松 dusk_pine`, `春涧花影 spring_blossom`), dedicated `山居画境` (`SkinGalleryScreen`) with instant zero-restart hot-swap, harmonic chime preview, `DataStore` persistence, and optional `昼夜随顺换境` evening pairing. | M2, M3, M5 | ORIGINAL_REQUEST, Survey 1–3 |
| F7 | Context-Aware Home (`今日一息`) & Quick Micro-Shortcuts | Dynamic Shan-Shui stage (`≥45%` whitespace), solar-term/time poetic greeting, quick skin switcher pill, 3 posture scene filters (`工位坐姿 / 桌边茶歇 / 晚间床沿`), 1-tap `开始一息` Hero Prescription Card, and 4 `片刻救急轻帖` shortcuts (`90s 连会叹息`, `2m 温目`, `2m 护腕`, `3m 松腰`). | M1, M2, M4 | ORIGINAL_REQUEST, Survey 1–2 |
| F8 | 6-Zone Body Tension Map & Unlimited Custom Composer (`身体寻音`) | Interactive Eastern illustrated body map (`BodyTensionMapCanvas` + accessible chips) for `后颈风池`, `双肩斜方`, `胸椎圆肩`, `下腰久坐`, `手腕鼠标侧`, `眼周眉心` (`1–3` multi-select) + `2 / 4 / 6 分钟` duration selector → instant custom routine (`一秒成方`), 100% free & unlimited. | M1, M2, M3, M4 | Survey 1–3 |
| F9 | Free Classical Curated Booklets (`四时雅集`) | 4 bound-booklet gentle care series (`《伏案颈胸开阔记》`, `《久坐腰骶归位帖》`, `《晚间呼吸解绑》`, `《屏幕温目护腕册》`) built from 19+ seated/desk micro-movements with pressure-free local progress tracking and zero VIP gates. | M1, M2, M4 | Survey 1–3 |
| F10 | Guided Stretch Player (`4s/6s` Halo & `闭眼余光态`) | Animated skin-styled `EditorialStretchFigureCanvas` interpolating `inhaleKeyframe ↔ exhaleKeyframe` across `4000ms` inhale (`吸气 · 轻展`) / `6000ms` exhale (`呼气 · 沉肩`), `BreathingHaloCanvas`, warm ≤36-char cues, 1-tap `闭眼余光态` peripheral glow mode, prev/next/pause/resume controls, and `FLAG_KEEP_SCREEN_ON`. | M2, M4 | Survey 1–3 |
| F11 | Procedural Harmonic Audio, Gentle Haptics & Flip-to-Pause | Offline synthesized `AudioTrack` chords (`432Hz 颂钵 / 528Hz 竹磬 / 396Hz 古琴 / 440Hz 春雨` + `静默仅微震` office mute), `VibrationEffect` breath waveforms, and player-isolated `FaceDownSensorMonitor` (`翻转静默暂存`) pausing silently when phone is placed face-down. | M1, M2, M4 | Survey 1–3 |
| F12 | Completion Water-Ripple Ritual & Somatic Check-In (`一息成`) | Concentric water-ripple completion stage displaying skin-specific poetic verse and 1-tap somatic feeling tag (`肩颈轻快 / 温热舒展 / 心神安宁`) persisted into `松息手札`. | M1, M2, M4 | Survey 1–3 |
| F13 | Low-Intrusion Sedentary Reminder Scheduler (`微风提醒`) | `WorkManager` periodic reminder (`45/60/90/120m`), gentle daily cap (`1–3/day`), `午休勿扰 (12:00–13:30)`, `夜间勿扰 (20:00–09:00)`, weekend auto-silence, `今日勿扰`, empathetic notification with `EXTRA_FROM_REMINDER_QUICK_START` splash-bypass deep-link, and `模拟微风提醒` instant preview. | M1, M2, M5 | ORIGINAL_REQUEST, Survey 1–3 |
| F14 | Streak-Free Water-Ripple Journal & Shareable Card (`松息手札 & 松息日笺`) | Xuan-paper monthly calendar rendering concentric ink/mineral ripples per session, calm monthly summary (`本月留白 X 息 · 舒展 Y 分钟`), **zero streak counters** (*"留白亦是休息"*), feeling notes, and native Android `松息日笺` shareable poetic image card (`Intent.ACTION_SEND`). | M1, M2, M5 | Survey 1–3 |
| F15 | Standalone Android APK Packaging, Visual Captures & Synchronized Brain Suite | Verified `assembleDebug`, signed `assembleRelease`, `lintDebug`, Roborazzi 4-skin PNG renders, clean permissions (`VIBRATE`, `POST_NOTIFICATIONS` only), and 1:1 synchronized `songxi_product_design.md` v2, `songxi_interactive_prototype.html` v2, and `songxi_skin_gallery_showcase.html`. | M1, M2, M5, M6 | ORIGINAL_REQUEST, Survey 1–3 |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Android Toolchain, Multi-Module Scaffold & Core Domain/Data/Sensory | Bootstrap `/home/antigravity/.android-sdk` + Gradle `8.10.2` + `env-setup.sh` + `locked_gradlew.sh` (strictly zero `/tmp`); initialize git repo & 8-module `/home/antigravity/songxi-android` with `-Psongxi.e2e=true` test hook; implement full `:core:model`, `:core:data` (24 poses, scenes, shortcuts, 4 booklets, custom composer, DataStore repos), `:core:sensory` (PCM `AudioTrack` synth, `VibrationEffect` haptics, isolated `FaceDownSensorMonitor`), and compilable stubs for remaining modules. | none | DONE (commit `84d3f40`, `23/23` tests pass, `5/5` Gate PASS) |
| M2 | Product Design Spec v2, 4-Skin Style Board & Standalone Android Interactive Prototype v2 | Research-grounded v2 rewrite of `songxi_product_design.md` (+ `/home/antigravity/docs/` mirror), creation of `songxi_skin_gallery_showcase.html`, and v2 upgrade of `songxi_interactive_prototype.html` (purging all WeChat MP & `四时席` VIP UI; implementing 4 live Illustration × Shan-Shui skins, 3s `开屏雅鉴` splash ad + policy controls, 6-zone body map, `4s/6s` eyes-free Web Audio player, reminders, and streak-free ripple journal), verified via headless `/usr/lib/chromium/chromium`. | none | DONE (`5/5` Gate PASS; `/home/antigravity/docs/*` + Brain `3d22de3812df3380df3042f3a0dcd19b`) |
| M3 | 5D Illustration × Shan-Shui Skin Engine & Compliant Splash-Ad Module | Tighten JVM/Gradle memory caps in `gradle.properties`/`build.gradle.kts`/`locked_gradlew.sh`; inspect & polish on-disk `:core:designsystem` (`SongxiSkin`, all 4 free built-in skins, `SongxiTheme`, `ShanShuiBackdropCanvas`, `EditorialStretchFigureCanvas`, `BodyTensionMapCanvas`, `BreathingHaloCanvas`, `ClassicalSealBadge`, Roborazzi PNG tests) and implement `:feature:splashad` (`SplashAdPolicyEngine` 7 gates, `CuratedHouseSplashAdProvider`, `SdkSplashAdBridgeProvider`, `SplashAdScreen` with `≥48dp` `跳过 3s`, bounded CTA sheet, zero motion sensors). | M1 (queued after M2) | DONE (commit `2e8ee26`, `52/52` tests pass, `0` lint errors, `5/5` Gate PASS in `sub_orch_m3_5`) |
| M4 | Guided Micro-Stretch, Body Tension Map & Booklets Module | Implement `:feature:stretch`: `HomeScreen` (`今日一息` scene filter, hero card, 4 quick relief shortcuts, quick skin badge), `BodyMapScreen` (`身体寻音` 6-zone interactive canvas + unlimited `一秒成方` composer + `四时雅集` 4 free booklets), and `StretchPlayerScreen` (`4s/6s` halo + animated figure, `闭眼余光态`, 4 synthesized soundscapes + mute, haptic waves, `翻转静默` face-down pause, `一息成` completion ripple & somatic check-in). | M1, M3 | DONE (`:feature:stretch:testDebugUnitTest` PASS, full 6-zone composer + `4s/6s` player + `一息成` verified) |
| M5 | Sanctuary Module (`画境/提醒/手札`) & Standalone Android App Integration | Implement `:feature:sanctuary` (`SkinGalleryScreen` + auto-night pairing, `SedentaryReminderWorker` + quiet-hours settings + preview notification, `PracticeJournalScreen` streak-free ripple calendar + `松息日笺` share card, splash privacy & manual preview controls) and `:app` (`MainActivity`, `SongxiAppContainer`, root navigation, splash gate & deep-link routing, release signing, `assembleDebug`/`assembleRelease`/`lintDebug`, Roborazzi 4-skin visual suite, Brain artifact sync). | M3, M4 | DONE (`app-debug.apk` + signed `app-release.apk` verified, `:app:lintDebug` `0` errors, `:feature:sanctuary` & `:app` unit tests PASS) |
| M6 | Final Milestone: 100% E2E Pass (Tiers 1–4) & Adversarial Coverage Hardening (Tier 5) | **Phase 1**: Consume `TEST_READY.md`, execute Tiers 1 → 2 → 3 → 4 via `e2e-tests/run_e2e.sh --tier all` across Android APK/UI and HTML Prototype, fixing all defects until 100% pass (`exit 0`). **Phase 2**: White-box & binary/manifest/lint verification across all 8 Gradle modules (`0` lint errors, `0` `/tmp` leaks). | M2, M5, E2E | DONE (`216/216` E2E tests PASS `100%` — `134` Android + `82` Prototype, `0` failed, `0` skipped, `:app:lintDebug` `0` errors) |
| E2E | E2E Testing Track (Opaque-Box Requirement-Driven Suite) | Requirement-driven opaque-box track owning `TEST_INFRA.md`, `TEST_READY.md`, and `e2e-tests/**`. Implements `216` automated opaque-box test cases across Tiers 1–4 covering all 15 features (`F1`–`F15`) for both Android (`-Psongxi.e2e=true` Robolectric/Compose UI + APK inspection) and Interactive Prototype (`playwright-core` + Headless Chrome), publishing `TEST_READY.md`. | none | DONE (`run_e2e.sh --verify-harness` PASS & `run_e2e.sh --tier all` `216/216` PASS; `TEST_READY.md` published) |

---

## Interface Contracts

### 1. `:core:model` ↔ All Modules (`com.songxi.app.core.model`)
```kotlin
package com.songxi.app.core.model

fun interface ClockProvider {
    fun nowEpochMillis(): Long
    fun currentLocalDateTime(): java.time.LocalDateTime =
        java.time.Instant.ofEpochMilli(nowEpochMillis())
            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
}

enum class SkinId(val storageKey: String) {
    INK_MIST("ink_mist"),               // 水墨远山
    QINGLV_CELADON("qinglv_celadon"),   // 青绿山居
    DUSK_PINE("dusk_pine"),             // 暮山听松
    SPRING_BLOSSOM("spring_blossom")    // 春涧花影
}

enum class PostureScene(val key: String) {
    DESK_SEATED("desk"), TEA_BREAK_STANDING("tea"), EVENING_BEDSIDE("bed")
}

enum class BodyRegion(val key: String, val labelCn: String) {
    POSTERIOR_NECK("neck", "后颈风池"),
    TRAPEZIUS_SHOULDER("shoulder", "双肩斜方"),
    THORACIC_CHEST("thoracic", "胸椎圆肩"),
    LOWER_LUMBAR("lumbar", "下腰久坐"),
    MOUSE_WRIST("wrist", "手腕鼠标侧"),
    EYE_BROW("eye", "眼周眉心")
}

enum class SoundProfileId(val key: String, val labelCn: String) {
    SINGING_BOWL_432("bowl", "温润颂钵"),
    BAMBOO_CHIME_528("bamboo", "空谷竹磬"),
    GUQIN_PINE_396("guqin", "松风琴韵"),
    SPRING_RAIN_440("rain", "春涧细雨"),
    SILENT_HAPTIC_ONLY("silent", "静默仅微震")
}

enum class SomaticFeeling(val key: String, val labelCn: String) {
    LIGHT_NECK_SHOULDER("light", "肩颈轻快"),
    WARM_OPEN("warm", "温热舒展"),
    CALM_PEACEFUL("calm", "心神安宁")
}

data class PoseKeyframe(
    val headTiltDeg: Float,
    val chinTuckRatio: Float,
    val leftShoulderDropRatio: Float,
    val rightShoulderDropRatio: Float,
    val spineArcRatio: Float,
    val leftArmAngleDeg: Float,
    val rightArmAngleDeg: Float,
    val wristFlexRatio: Float = 0f,
    val eyesClosed: Boolean = true
)

data class StretchPose(
    val id: String,
    val title: String,
    val gentleCue: String,
    val targetRegions: Set<BodyRegion>,
    val durationSeconds: Int = 45,
    val inhaleKeyframe: PoseKeyframe,
    val exhaleKeyframe: PoseKeyframe,
    val bilateralSwitchAtHalfway: Boolean = false
)

data class StretchRoutine(
    val id: String,
    val title: String,
    val subtitle: String,
    val scene: PostureScene,
    val eyesFreeCapable: Boolean = true,
    val seatedDiscreet: Boolean = true,
    val poses: List<StretchPose>
) {
    val totalDurationSeconds: Int get() = poses.sumOf { it.durationSeconds }
}

data class BookletSeries(
    val id: String,
    val title: String,
    val subtitle: String,
    val totalDays: Int,
    val dailyMinutes: Int,
    val routines: List<StretchRoutine>
)

data class PracticeSessionRecord(
    val id: String,
    val completedAtEpochMs: Long,
    val dateIso: String,                // YYYY-MM-DD
    val routineId: String,
    val routineTitle: String,
    val durationSeconds: Int,
    val skinId: SkinId,
    val feeling: SomaticFeeling?
)

data class ReminderPreferences(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 60,      // 45 | 60 | 90 | 120
    val dailyMaxCount: Int = 2,         // 1..3
    val lunchQuietEnabled: Boolean = true,  // 12:00..13:30
    val nightQuietEnabled: Boolean = true,  // 20:00..09:00
    val weekendSilent: Boolean = true,
    val muteTodayDateIso: String? = null
)

enum class SplashArtworkStyle { CELADON_TEA, PINE_INCENSE, WOOD_DESK_STAND }

data class SplashAdCreative(
    val id: String,
    val sponsorName: String,
    val title: String,
    val subtitle: String,
    val ctaLabel: String,
    val landingSummary: String,
    val landingUrl: String,
    val artworkTheme: SplashArtworkStyle,
    val durationSeconds: Int = 3
)
```

### 2. `:core:data` & `:core:sensory` ↔ Feature Modules
```kotlin
package com.songxi.app.core.data
// Exposed repositories (all backed by DataStore<Preferences> with in-memory/test factory support):
// - ThemePreferencesRepository: selectedSkinId, autoNightSkinEnabled, onboardingSeen, setSelectedSkin, setAutoNightSkin, markOnboardingSeen
// - StretchCatalogRepository: allPoses (≥19), heroRoutineFor(scene), quickReliefRoutines (4), curatedBooklets (4), bookletCompletedDays, markBookletDayCompleted, composeCustomRoutine(regions, targetMinutes)
// - PracticeJournalRepository: allRecords, observeMonthRipples(YearMonth), recordCompletedSession(PracticeSessionRecord)
// - ReminderPreferencesRepository: preferences, updatePreferences, notificationsSentOn(dateIso), recordNotificationSent(dateIso)
// - SplashAdLedgerRepository: totalAppLaunches, personalizedAdsEnabled, impressionsOn(dateIso), lastImpressionEpochMs, incrementAppLaunch, recordImpression, recordSkip, recordCtaClick, setPersonalizedAdsEnabled
```
```kotlin
package com.songxi.app.core.sensory
// - HarmonicAudioEngine: playInhaleChord(skinId, soundProfile, muted), playExhaleChord(...), playPoseTransitionChime(...), playSkinPreviewNote(skinId), release()
// - BreathingHapticEngine: pulseInhaleWave(maxAmplitude, enabled), pulseExhaleWave(maxAmplitude, enabled), cancel()
// - FaceDownSensorMonitor: isFaceDown: StateFlow<Boolean>, startMonitoring(), stopMonitoring(), simulateFaceDownForTest(faceDown: Boolean)
```

### 3. `:core:designsystem` & `:feature:splashad` ↔ Feature Modules & `:app`
- `:core:designsystem` (`com.songxi.app.core.designsystem`):
  - `SongxiSkin` (5 token groups: `meta: SkinEditorialTokens`, `palette: SkinPaletteTokens`, `landscape: SkinLandscapeTokens`, `illustration: SkinIllustrationTokens`, `sensory: SkinSensoryTokens`)
  - `SkinPackRegistry` (`BuiltInSkinRegistry` providing all 4 free `SongxiSkin` instances + `resolveEffectiveSkin(selected, autoNightDuskPine, hourOfDay)`)
  - `LocalSongxiSkin` & `SongxiTheme(skin: SongxiSkin, content: @Composable () -> Unit)`
  - Shared Composables: `ShanShuiBackdropCanvas`, `EditorialStretchFigureCanvas`, `BodyTensionMapCanvas`, `BreathingHaloCanvas`, `ClassicalSealBadge`
- `:feature:splashad` (`com.songxi.app.feature.splashad`):
  - `SplashGateDecision` (`Show(creative, countdownSeconds = 3)` | `Bypass(reason: BypassReason)`)
  - `SplashAdProvider` (`CuratedHouseSplashAdProvider` default + `SdkSplashAdBridgeProvider`)
  - `SplashAdPolicyEngine.evaluateLaunch(isReminderDeepLink, hasActiveOrPausedStretch, warmBackgroundDurationMs, forcePreview)`
  - `SplashAdScreen(creative, countdownSeconds, onSkipOrFinish, onExternalUrlConfirmed, modifier)`

### 4. Android Entry Points, Intent Extras & Opaque-Box UI Semantics (`testTag` Contract for E2E)
- **Launcher Activity**: `com.songxi.app.MainActivity`
- **Public `Intent` Extra Keys on `MainActivity` (`com.songxi.app.SongxiIntents` / literal strings)**:
  - `"com.songxi.app.extra.FROM_REMINDER_QUICK_START"` (`Boolean`) — bypasses splash ad and starts the 90s seated relief routine directly.
  - `"com.songxi.app.extra.FORCE_SPLASH_PREVIEW"` (`Boolean`) — forces `3s` `开屏雅鉴` splash ad display for user/E2E preview.
  - `"com.songxi.app.extra.SIMULATED_EPOCH_MS"` (`Long`) — optional test clock seed when running deterministic E2E flows.
  - `"com.songxi.app.extra.RESET_STATE"` (`Boolean`) — optional clean-slate state reset for isolated E2E test cases.
- **Mandatory Compose `Modifier.testTag(...)` Semantic Contract**:
  - **Onboarding & Root Nav**: `onboarding_card`, `onboarding_enter_button`, `bottom_nav_bar`, `nav_tab_home` (`今日一息`), `nav_tab_bodymap` (`寻音雅集`), `nav_tab_skins` (`山居画境`), `nav_tab_journal` (`松息手札`), `nav_tab_settings` (`微风设置`) *(or 4-tab layout with integrated settings/skin sheet — all listed `nav_tab_*` tags MUST be reachable on the persistent navigation chrome or top bar so opaque-box E2E tests navigate reliably in 1 tap!)*. Specifically, provide persistent bottom navigation or top-level quick-entry buttons exposing all 5 tags: `nav_tab_home`, `nav_tab_bodymap`, `nav_tab_skins`, `nav_tab_journal`, `nav_tab_settings`.
  - **Splash Ad (`开屏雅鉴`)**: `splash_ad_screen`, `splash_ad_backdrop`, `splash_ad_badge` (contains `"广告"`), `splash_ad_skip_button` (`≥48dp × 48dp`, contains `"跳过"`), `splash_ad_cta_button`, `splash_ad_confirm_sheet`, `splash_ad_confirm_dismiss`, `splash_ad_brand_footer`.
  - **Home (`今日一息`)**: `home_screen`, `home_shanshui_backdrop`, `home_active_skin_badge`, `home_scene_desk`, `home_scene_tea`, `home_scene_bed`, `home_hero_card`, `home_start_hero_button`, `home_quick_routine_0`, `home_quick_routine_1`, `home_quick_routine_2`, `home_quick_routine_3`.
  - **Body Map & Booklets (`寻音雅集`)**: `bodymap_screen`, `bodymap_canvas`, `bodymap_zone_neck`, `bodymap_zone_shoulder`, `bodymap_zone_thoracic`, `bodymap_zone_lumbar`, `bodymap_zone_wrist`, `bodymap_zone_eye`, `bodymap_duration_2`, `bodymap_duration_4`, `bodymap_duration_6`, `bodymap_compose_start_button`, `booklet_card_0`, `booklet_card_1`, `booklet_card_2`, `booklet_card_3`.
  - **Stretch Player & Completion (`闭眼跟练器 & 一息成`)**: `player_screen`, `player_figure_canvas`, `player_breathing_halo`, `player_breath_phase_label` (`吸气` / `呼气`), `player_eyes_free_toggle`, `player_sound_selector`, `player_sound_silent`, `player_pause_resume_button`, `player_prev_pose_button`, `player_next_pose_button`, `player_close_button`, `player_paused_by_flip_banner`, `completion_screen`, `completion_ripple_canvas`, `completion_feeling_light`, `completion_feeling_warm`, `completion_feeling_calm`, `completion_finish_button`.
  - **Skin Gallery (`山居画境`)**: `skins_screen`, `skin_card_ink_mist`, `skin_card_qinglv_celadon`, `skin_card_dusk_pine`, `skin_card_spring_blossom`, `skin_chime_preview_button`, `skin_auto_night_switch`.
  - **Journal (`松息手札`)**: `journal_screen`, `journal_calendar_grid`, `journal_monthly_summary`, `journal_share_card_button`, `journal_share_preview_dialog`.
  - **Reminders & Splash Settings (`微风设置`)**: `settings_screen`, `reminder_enable_switch`, `reminder_interval_45`, `reminder_interval_60`, `reminder_interval_90`, `reminder_interval_120`, `reminder_lunch_quiet_switch`, `reminder_night_quiet_switch`, `reminder_weekend_quiet_switch`, `reminder_mute_today_switch`, `reminder_preview_trigger_button`, `reminder_inapp_banner`, `splash_personalized_switch`, `splash_preview_trigger_button`.

### 5. Interactive HTML Prototype & Design Suite Entry Points (`data-testid` Contract for E2E)
- **Canonical Target Files (in `/home/antigravity/docs/`)**:
  - `/home/antigravity/docs/songxi_interactive_prototype.html`
  - `/home/antigravity/docs/songxi_skin_gallery_showcase.html`
  - `/home/antigravity/docs/songxi_product_design.md` (and `/home/antigravity/docs/SONGXI_PRODUCT_DESIGN.md`, `/home/antigravity/docs/SKIN_AND_SPLASH_SPEC.md`)
  - *(Note: Per Cortex per-conversation Brain directory isolation, `83048e5b-...` holds the read-only v1 historical input draft; M2 writes canonical v2 deliverables to `/home/antigravity/docs/*` AND byte-identical `ArtifactMetadata` copies to M2 Worker's own sanctioned `/home/antigravity/.gemini/jetski/brain/<worker-conv-id>/` directory. All E2E & Auditor checks target `/home/antigravity/docs/*`.)*
- **CSP Rule**: Standalone HTML; only `https://www.gstatic.com/antigravity/web/dev/tailwindcss.min.js` external script allowed; inline SVG/Canvas/CSS + Web Audio API; scope CSS theme tokens to `.songxi-app-shell` (never override host `:root`).
- **Mirror `data-testid` Attributes**: Expose matching `data-testid="<tag>"` attributes for every tag listed in §4 above (`splash_ad_screen`, `splash_ad_badge`, `splash_ad_skip_button`, `splash_ad_cta_button`, `nav_tab_home`, `nav_tab_bodymap`, `nav_tab_skins`, `nav_tab_journal`, `nav_tab_settings`, `skin_card_ink_mist`, `skin_card_qinglv_celadon`, `skin_card_dusk_pine`, `skin_card_spring_blossom`, `home_start_hero_button`, `bodymap_zone_neck`, `bodymap_compose_start_button`, `player_screen`, `player_eyes_free_toggle`, `player_pause_resume_button`, `completion_screen`, `journal_screen`, `reminder_preview_trigger_button`, `splash_preview_trigger_button`, plus `player_simulate_flip_button` in Director/Player controls).
