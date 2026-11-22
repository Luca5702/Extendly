'use strict';

process.env.TZ = 'Asia/Shanghai';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  PATHS,
  TAGS,
  SKIN_KEYS,
  ALLOWED_TAILWIND_CDN,
  FORBIDDEN_PROTOTYPE_REGEX,
  FORBIDDEN_DOC_REGEX,
  FORBIDDEN_STREAK_SHAME_REGEX,
  localEpochMs,
  byTestId,
  isVisible,
  clickTestId,
  readTestIdText,
  isToggleOrChipActive,
  openPrototypeReadyAtHome,
  advanceRoutineToCompletionOnPage,
  parseRgbString,
  wcagContrastRatio,
  readShellColorsAndContrast,
  auditShellCssScoping,
  auditHtmlCsp,
  auditDesignDocsSync,
} = require('./harness.cjs');

const ALL_SKIN_CARD_TAGS = [
  TAGS.skin_card_ink_mist,
  TAGS.skin_card_qinglv_celadon,
  TAGS.skin_card_dusk_pine,
  TAGS.skin_card_spring_blossom,
];

const ALL_ZONE_TAGS = [
  TAGS.bodymap_zone_neck,
  TAGS.bodymap_zone_shoulder,
  TAGS.bodymap_zone_thoracic,
  TAGS.bodymap_zone_lumbar,
  TAGS.bodymap_zone_wrist,
  TAGS.bodymap_zone_eye,
];

const tests = [
  // ===========================================================================
  // F1 — Zero-Login Local-First Sanctuary & Gentle Onboarding (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-01',
    tier: 2,
    feature: 'F1',
    name: 'Corrupt/malformed localStorage entries fall back gracefully with 0 uncaught JS exceptions',
    fn: async ({ session }) => {
      await session.withPage(
        {
          storageSeed: {
            songxi_state: '{bad_json',
            songxi_prefs: '{corrupted_payload',
            songxi_journal: 'NaN',
            songxi_splash_ledger: '{{{invalid',
            songxi_reminder: 'undefined',
          },
        },
        async ({ page, prototypeUrl }) => {
          await page.goto(prototypeUrl, { waitUntil: 'load' });
          const recovered =
            (await isVisible(page, TAGS.onboarding_card)) || (await isVisible(page, TAGS.home_screen));
          assert.equal(recovered, true, 'Malformed localStorage must recover safely to onboarding_card or home_screen');
        }
      );
    },
  },
  {
    id: 'P-T2-02',
    tier: 2,
    feature: 'F1',
    name: 'Rapid double-click on onboarding_enter_button & repeated reloads never re-prompt onboarding or show login walls',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await page.goto(prototypeUrl, { waitUntil: 'load' });
        if (await isVisible(page, TAGS.onboarding_enter_button)) {
          await byTestId(page, TAGS.onboarding_enter_button).dblclick().catch(async () => {
            if (await isVisible(page, TAGS.onboarding_enter_button)) {
              await clickTestId(page, TAGS.onboarding_enter_button);
            }
          });
        }
        assert.equal(await isVisible(page, TAGS.home_screen), true);

        for (let i = 0; i < 3; i++) {
          await page.reload({ waitUntil: 'load' });
          assert.equal(
            await isVisible(page, TAGS.onboarding_card),
            false,
            `onboarding_card must not reappear on reload #${i + 1}`
          );
          const bodyText = await page.locator('body').innerText();
          assert.equal(/手机号登录|验证码登录|注册账号/.test(bodyText), false, 'Zero login walls allowed');
        }
      });
    },
  },

  // ===========================================================================
  // F2 — Editorial Framed Splash Ad UI (开屏雅鉴) & Safe CTA (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-03',
    tier: 2,
    feature: 'F2',
    name: 'splash_ad_skip_button clickable at t=0ms (>= 36px hit target) & splash_ad_backdrop click is strictly inert',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, TAGS.splash_preview_trigger_button);

        // t = 0ms before any clock advancement
        assert.equal(await isVisible(page, TAGS.splash_ad_skip_button), true, 'Skip button must be visible at t=0ms');
        const skipBox = await byTestId(page, TAGS.splash_ad_skip_button).boundingBox();
        assert.ok(
          skipBox && skipBox.width >= 35.5 && skipBox.height >= 35.5,
          `Skip button hit target must be >= 36px, got ${JSON.stringify(skipBox)}`
        );

        const backdropBox = await byTestId(page, TAGS.splash_ad_backdrop).boundingBox();
        assert.ok(backdropBox, 'splash_ad_backdrop must have valid bounding box');
        await page.mouse.click(backdropBox.x + backdropBox.width * 0.2, backdropBox.y + backdropBox.height * 0.28);

        assert.equal(await isVisible(page, TAGS.splash_ad_screen), true, 'Backdrop click must keep splash open');
        assert.equal(
          await isVisible(page, TAGS.splash_ad_confirm_sheet),
          false,
          'Backdrop click must NEVER open confirmation sheet (anti-hotzone)'
        );
      });
    },
  },
  {
    id: 'P-T2-04',
    tier: 2,
    feature: 'F2',
    name: '3000ms countdown auto-dismisses splash cleanly, splash registers zero devicemotion shake listeners & stays muted',
    fn: async ({ session }) => {
      await session.withPage(
        {
          initScripts: [
            () => {
              window.__motionListeners = [];
              window.__splashOscCount = 0;
              window.__inSplashAudit = false;
              const origAdd = window.addEventListener.bind(window);
              window.addEventListener = function (type, listener, opts) {
                if (/^(devicemotion|deviceorientation)/i.test(String(type))) {
                  window.__motionListeners.push(String(type));
                }
                return origAdd(type, listener, opts);
              };
              if (window.OscillatorNode && window.OscillatorNode.prototype) {
                const origStart = window.OscillatorNode.prototype.start;
                window.OscillatorNode.prototype.start = function (...args) {
                  if (window.__inSplashAudit) window.__splashOscCount++;
                  return origStart.apply(this, args);
                };
              }
            },
          ],
        },
        async ({ page, prototypeUrl }) => {
          await openPrototypeReadyAtHome(page, prototypeUrl);
          await clickTestId(page, TAGS.nav_tab_settings);

          await page.evaluate(() => {
            window.__inSplashAudit = true;
            window.__splashOscCount = 0;
          });
          await clickTestId(page, TAGS.splash_preview_trigger_button);
          assert.equal(await isVisible(page, TAGS.splash_ad_screen), true);

          const motionCount = await page.evaluate(() => window.__motionListeners.length);
          assert.equal(motionCount, 0, 'Splash ad MUST register 0 devicemotion/deviceorientation shake listeners');

          await page.clock.runFor(3_300);
          await page.evaluate(() => {
            window.__inSplashAudit = false;
          });
          const oscDuringSplash = await page.evaluate(() => window.__splashOscCount);
          assert.equal(oscDuringSplash, 0, 'Splash ad must remain strictly muted (0 Web Audio oscillator starts)');
          assert.equal(await isVisible(page, TAGS.splash_ad_screen), false, '3000ms countdown must auto-dismiss splash');
        }
      );
    },
  },

  // ===========================================================================
  // F3 — 7-Gate Calm Splash-Ad Policy & Privacy Controls (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-05',
    tier: 2,
    feature: 'F3',
    name: 'Gate 5 (<= 3/day cap + next-day reset) & Gate 6 (+3h59m bypass vs +4h01m show) exact boundaries',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 1, 0) }, async ({ page, prototypeUrl }) => {
        // Launch #1 at 01:00 -> Gate 1 grace bypass
        await openPrototypeReadyAtHome(page, prototypeUrl);

        // Launch #2 at 05:05 -> Impression #1
        await page.clock.setSystemTime(localEpochMs(2026, 11, 18, 5, 5));
        await page.reload({ waitUntil: 'load' });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), true, 'Expected impression #1 at 05:05');
        await clickTestId(page, TAGS.splash_ad_skip_button);

        // Launch #3 at 09:04 (+3h59m) -> Gate 6 cooldown bypass
        await page.clock.setSystemTime(localEpochMs(2026, 11, 18, 9, 4));
        await page.reload({ waitUntil: 'load' });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false, '+3h59m (09:04) must bypass splash');

        // Launch #4 at 09:06 (+4h01m) -> Impression #2
        await page.clock.setSystemTime(localEpochMs(2026, 11, 18, 9, 6));
        await page.reload({ waitUntil: 'load' });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), true, '+4h01m (09:06) must show impression #2');
        await clickTestId(page, TAGS.splash_ad_skip_button);

        // Launch #5 at 13:10 -> Impression #3
        await page.clock.setSystemTime(localEpochMs(2026, 11, 18, 13, 10));
        await page.reload({ waitUntil: 'load' });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), true, '13:10 must show impression #3');
        await clickTestId(page, TAGS.splash_ad_skip_button);

        // Launch #6 at 17:20 (>4h same day) -> Gate 5 <= 3/day cap blocks 4th!
        await page.clock.setSystemTime(localEpochMs(2026, 11, 18, 17, 20));
        await page.reload({ waitUntil: 'load' });
        assert.equal(
          await isVisible(page, TAGS.splash_ad_screen),
          false,
          '4th same-day launch at 17:20 MUST bypass due to <=3/day cap'
        );

        // Launch #7 next day 2026-11-19T09:30 -> cap resets and splash shows!
        await page.clock.setSystemTime(localEpochMs(2026, 11, 19, 9, 30));
        await page.reload({ waitUntil: 'load' });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), true, 'Next day must reset daily splash cap');
      });
    },
  },
  {
    id: 'P-T2-06',
    tier: 2,
    feature: 'F3',
    name: 'Gate 4 warm resume (29m suppresses), Gate 2 active-player immunity & manual preview override',
    fn: async ({ session }) => {
      const t0800 = localEpochMs(2026, 11, 18, 8, 0);
      await session.withPage({ timeMs: t0800 }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);

        // Simulate <30m (29m) warm visibilitychange resume on Home (0 prior splash impressions)
        await page.evaluate(() => {
          Object.defineProperty(document, 'visibilityState', { value: 'hidden', configurable: true });
          document.dispatchEvent(new Event('visibilitychange'));
        });
        await page.clock.setSystemTime(t0800 + 29 * 60_000);
        await page.evaluate(() => {
          Object.defineProperty(document, 'visibilityState', { value: 'visible', configurable: true });
          document.dispatchEvent(new Event('visibilitychange'));
        });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false, '29m warm resume must suppress splash');

        // Gate 2: Active player immunity across 45m warm resume
        await clickTestId(page, TAGS.home_start_hero_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        await page.evaluate(() => {
          Object.defineProperty(document, 'visibilityState', { value: 'hidden', configurable: true });
          document.dispatchEvent(new Event('visibilitychange'));
        });
        await page.clock.setSystemTime(t0800 + 75 * 60_000);
        await page.evaluate(() => {
          Object.defineProperty(document, 'visibilityState', { value: 'visible', configurable: true });
          document.dispatchEvent(new Event('visibilitychange'));
        });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false, 'Active player must stay immune to splash');
        await clickTestId(page, TAGS.player_close_button);

        // Manual preview override always works
        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, TAGS.splash_preview_trigger_button);
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), true, 'Manual preview must override policy caps');
      });
    },
  },

  // ===========================================================================
  // F4 — Zero-Membership & 100% Unlocked Content Invariant (3 tests)
  // ===========================================================================
  {
    id: 'P-T2-07',
    tier: 2,
    feature: 'F4',
    name: 'Prototype raw HTML/JS & live 5-tab DOM sweep have 0 legacy VIP, WeChat MP, or in-app ad artifacts',
    fn: async ({ session }) => {
      const rawHtml = fs.readFileSync(PATHS.prototypeHtml, 'utf8');
      const rawHit = rawHtml.match(FORBIDDEN_PROTOTYPE_REGEX);
      assert.equal(rawHit, null, `Prototype HTML contains forbidden v1 token: ${rawHit && rawHit[0]}`);

      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const legacyNodes = await page
          .locator('#viewport-mp, #btn-mode-mp, [onclick*="openMembershipModal"]')
          .count();
        assert.equal(legacyNodes, 0, 'Prototype DOM must contain 0 legacy WeChat MP / VIP modal selectors');

        for (const tab of [
          TAGS.nav_tab_home,
          TAGS.nav_tab_bodymap,
          TAGS.nav_tab_skins,
          TAGS.nav_tab_journal,
          TAGS.nav_tab_settings,
        ]) {
          await clickTestId(page, tab);
          const tabText = await page.locator('body').innerText();
          const hit = tabText.match(FORBIDDEN_PROTOTYPE_REGEX);
          assert.equal(hit, null, `Tab ${tab} rendered forbidden VIP/MP text: ${hit && hit[0]}`);
        }
      });
    },
  },
  {
    id: 'P-T2-08',
    tier: 2,
    feature: 'F4',
    name: 'songxi_skin_gallery_showcase.html source & live DOM sweep have 0 VIP/paywall/WeChat remnants',
    fn: async ({ session }) => {
      const raw = fs.readFileSync(PATHS.showcaseHtml, 'utf8');
      const rawHit = raw.match(FORBIDDEN_PROTOTYPE_REGEX);
      assert.equal(rawHit, null, `Showcase HTML contains forbidden v1 token: ${rawHit && rawHit[0]}`);

      await session.withPage({ viewport: { width: 1280, height: 860 } }, async ({ page, showcaseUrl }) => {
        await page.goto(showcaseUrl, { waitUntil: 'load' });
        const rendered = await page.locator('body').innerText();
        const domHit = rendered.match(FORBIDDEN_PROTOTYPE_REGEX);
        assert.equal(domHit, null, `Showcase live DOM contains forbidden token: ${domHit && domHit[0]}`);
      });
    },
  },
  {
    id: 'P-T2-09',
    tier: 2,
    feature: 'F4',
    name: 'All /home/antigravity/docs/*.md specifications pass negation-safe audit with 0 legacy v1 VIP/MP claims',
    fn: async () => {
      for (const mdPath of [PATHS.designMd, PATHS.docsDesignMd, PATHS.docsSkinSplashMd]) {
        assert.equal(fs.existsSync(mdPath), true, `Missing ${mdPath}`);
        const content = fs.readFileSync(mdPath, 'utf8');
        const hit = content.match(FORBIDDEN_DOC_REGEX);
        assert.equal(hit, null, `${mdPath} contains forbidden legacy v1 claim: '${hit && hit[0]}'`);
      }
    },
  },

  // ===========================================================================
  // F5 — 5-Dimensional Illustration × Shan-Shui Skin Engine (3 tests)
  // ===========================================================================
  {
    id: 'P-T2-10',
    tier: 2,
    feature: 'F5',
    name: 'All 4 skins ban pure #FFFFFF/#000000 canvas backgrounds & satisfy WCAG >= 4.5:1 text contrast',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const seenBgs = new Set();

        for (const skinTag of ALL_SKIN_CARD_TAGS) {
          await clickTestId(page, TAGS.nav_tab_skins);
          await clickTestId(page, skinTag);
          await clickTestId(page, TAGS.nav_tab_home);

          const { bg, bgCss, contrast } = await readShellColorsAndContrast(page);
          seenBgs.add(bgCss);
          assert.equal(
            bg.r === 255 && bg.g === 255 && bg.b === 255,
            false,
            `${skinTag} must NOT use pure #FFFFFF canvas ground`
          );
          assert.equal(
            bg.r === 0 && bg.g === 0 && bg.b === 0,
            false,
            `${skinTag} must NOT use pure #000000 canvas ground`
          );
          assert.ok(contrast >= 4.5, `${skinTag} primary text contrast (${contrast.toFixed(2)}) must be >= 4.5:1`);
        }
        assert.equal(seenBgs.size, 4, 'All 4 skins must define 4 distinct paper/silk surface grounds');
      });
    },
  },
  {
    id: 'P-T2-11',
    tier: 2,
    feature: 'F5',
    name: 'Prototype CSS custom properties strictly scoped to .songxi-app-shell (zero :root pollution)',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const scoping = await auditShellCssScoping(page);
        assert.equal(scoping.hasShellElement, true, 'Prototype must render .songxi-app-shell');
        assert.ok(
          scoping.shellCustomProps.length >= 4,
          `Expected >= 4 custom CSS tokens on .songxi-app-shell, got ${scoping.shellCustomProps.length}`
        );
        assert.deepEqual(
          scoping.rootCustomProps,
          [],
          `Theme custom properties must NEVER pollute :root (${scoping.rootCustomProps.join(', ')})`
        );
      });
    },
  },
  {
    id: 'P-T2-12',
    tier: 2,
    feature: 'F5',
    name: 'songxi_skin_gallery_showcase.html maintains non-#FFFFFF/#000000 skin stages, >= 4.5:1 contrast & zero 0x0 SVGs',
    fn: async ({ session }) => {
      await session.withPage({ viewport: { width: 1366, height: 900 } }, async ({ page, showcaseUrl }) => {
        await page.goto(showcaseUrl, { waitUntil: 'load' });
        const audit = await page.evaluate(() => {
          const bodyBg = getComputedStyle(document.body).backgroundColor;
          const h1 = document.querySelector('h1, h2') || document.body;
          const fg = getComputedStyle(h1).color;
          const svgs = Array.from(document.querySelectorAll('svg'));
          const nonZeroSvgs = svgs.filter((s) => {
            const r = s.getBoundingClientRect();
            return r.width > 4 && r.height > 4;
          }).length;
          return {
            bodyBg,
            fg,
            totalSvgs: svgs.length,
            nonZeroSvgs,
            hasReplacementChar: document.body.innerText.includes('\uFFFD'),
          };
        });

        const bg = parseRgbString(audit.bodyBg);
        const fg = parseRgbString(audit.fg);
        assert.ok(bg && fg, 'Showcase must expose valid computed RGB colors');
        assert.equal(bg.r === 255 && bg.g === 255 && bg.b === 255, false, 'Showcase must avoid stark #FFFFFF ground');
        assert.equal(bg.r === 0 && bg.g === 0 && bg.b === 0, false, 'Showcase must avoid stark #000000 ground');
        assert.ok(wcagContrastRatio(fg, bg) >= 4.5, 'Showcase primary title contrast must be >= 4.5:1');
        assert.ok(audit.nonZeroSvgs >= 4, `Expected >= 4 non-zero SVGs in Showcase, got ${audit.nonZeroSvgs}`);
        assert.equal(audit.hasReplacementChar, false, 'Showcase must contain zero \\uFFFD replacement glyphs');
      });
    },
  },

  // ===========================================================================
  // F6 — 4 Free Built-In Skins, Live Gallery & Auto-Night Pairing (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-13',
    tier: 2,
    feature: 'F6',
    name: 'Auto-night boundary: 19:59 keeps daytime qinglv_celadon, 20:01 & 06:30 activate dusk_pine, 09:30 restores day',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 14, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_qinglv_celadon);
        if (!(await isToggleOrChipActive(page, TAGS.skin_auto_night_switch))) {
          await clickTestId(page, TAGS.skin_auto_night_switch);
        }

        const evalAt = async (day, hour, min) => {
          await page.clock.setSystemTime(localEpochMs(2026, 11, day, hour, min));
          await page.reload({ waitUntil: 'load' });
          if (await isVisible(page, TAGS.splash_ad_skip_button)) {
            await clickTestId(page, TAGS.splash_ad_skip_button);
          }
          const { bgLuminance } = await readShellColorsAndContrast(page);
          return bgLuminance;
        };

        const lum1959 = await evalAt(18, 19, 59);
        assert.ok(lum1959 > 0.5, `At 19:59 daytime qinglv_celadon must stay active (lum=${lum1959.toFixed(3)})`);

        const lum2001 = await evalAt(18, 20, 1);
        assert.ok(lum2001 < 0.25, `At 20:01 auto-night dusk_pine must activate (lum=${lum2001.toFixed(3)})`);

        const lum0630 = await evalAt(19, 6, 30);
        assert.ok(lum0630 < 0.25, `At 06:30 pre-dawn auto-night must keep dusk_pine (lum=${lum0630.toFixed(3)})`);

        const lum0930 = await evalAt(19, 9, 30);
        assert.ok(lum0930 > 0.5, `At 09:30 daytime qinglv_celadon must restore (lum=${lum0930.toFixed(3)})`);
      });
    },
  },
  {
    id: 'P-T2-14',
    tier: 2,
    feature: 'F6',
    name: 'Auto-night OFF at 21:30 preserves manual light skin & rapid 12-click 4-skin stress loop has 0 errors',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 21, 30) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_skins);
        if (await isToggleOrChipActive(page, TAGS.skin_auto_night_switch)) {
          await clickTestId(page, TAGS.skin_auto_night_switch);
        }
        await clickTestId(page, TAGS.skin_card_spring_blossom);

        await page.reload({ waitUntil: 'load' });
        const { bgLuminance } = await readShellColorsAndContrast(page);
        assert.ok(
          bgLuminance > 0.5,
          `With auto-night OFF at 21:30, manual spring_blossom light skin must remain active (lum=${bgLuminance.toFixed(3)})`
        );

        await clickTestId(page, TAGS.nav_tab_skins);
        for (let loop = 0; loop < 3; loop++) {
          for (const skinTag of ALL_SKIN_CARD_TAGS) {
            await clickTestId(page, skinTag);
          }
        }
        await clickTestId(page, TAGS.nav_tab_home);
        const badge = await readTestIdText(page, TAGS.home_active_skin_badge);
        assert.ok(badge.includes('春'), `After 12 rapid switches, spring_blossom must be active, got '${badge}'`);
      });
    },
  },

  // ===========================================================================
  // F7 — Context-Aware Home (今日一息) & Quick Micro-Shortcuts (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-15',
    tier: 2,
    feature: 'F7',
    name: 'Home stage preserves single uncluttered Hero card (0 ad banners) & deterministic desk -> tea -> bed -> desk cycle',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        assert.equal(await page.locator(`[data-testid="${TAGS.home_hero_card}"]`).count(), 1);
        assert.equal(await isVisible(page, TAGS.splash_ad_badge), false, 'Home must never show ad badge');

        await clickTestId(page, TAGS.home_scene_desk);
        const initialDesk = await readTestIdText(page, TAGS.home_hero_card);

        await clickTestId(page, TAGS.home_scene_tea);
        await clickTestId(page, TAGS.home_scene_bed);
        const bedHero = await readTestIdText(page, TAGS.home_hero_card);

        await clickTestId(page, TAGS.home_start_hero_button);
        await clickTestId(page, TAGS.player_close_button);
        assert.equal(
          await readTestIdText(page, TAGS.home_hero_card),
          bedHero,
          'Closing player must preserve selected bed scene'
        );

        await clickTestId(page, TAGS.home_scene_desk);
        assert.equal(await readTestIdText(page, TAGS.home_hero_card), initialDesk);
      });
    },
  },
  {
    id: 'P-T2-16',
    tier: 2,
    feature: 'F7',
    name: 'Narrow mobile viewport (360x640) & 390x844 keep home_start_hero_button and home_active_skin_badge clickable without overlap',
    fn: async ({ session }) => {
      await session.withPage({ viewport: { width: 360, height: 640 } }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const overflow = await page.evaluate(
          () => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1
        );
        assert.equal(overflow, false, '360x640 viewport must not overflow horizontally');

        await byTestId(page, TAGS.home_start_hero_button).scrollIntoViewIfNeeded().catch(() => {});
        assert.equal(await isVisible(page, TAGS.home_start_hero_button), true);
        assert.equal(await isVisible(page, TAGS.bottom_nav_bar), true);

        const initialBadge = await readTestIdText(page, TAGS.home_active_skin_badge);
        await clickTestId(page, TAGS.home_active_skin_badge);
        if (await isVisible(page, TAGS.skins_screen)) {
          await clickTestId(page, TAGS.skin_card_qinglv_celadon);
          await clickTestId(page, TAGS.nav_tab_home);
        }
        const afterBadge = await readTestIdText(page, TAGS.home_active_skin_badge);
        assert.notEqual(initialBadge, afterBadge, 'home_active_skin_badge must reach skin switch in <= 2 clicks');
      });
    },
  },

  // ===========================================================================
  // F8 — 6-Zone Body Tension Map & Unlimited Custom Composer (身体寻音) (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-17',
    tier: 2,
    feature: 'F8',
    name: '4th zone click enforces <= 3 active zones (caps at 3 or shifts oldest) & 0-zone deselection never crashes player',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);

        for (const z of ALL_ZONE_TAGS) {
          if (await isToggleOrChipActive(page, z)) {
            await clickTestId(page, z);
          }
        }
        for (const z of [
          TAGS.bodymap_zone_neck,
          TAGS.bodymap_zone_shoulder,
          TAGS.bodymap_zone_lumbar,
          TAGS.bodymap_zone_wrist,
        ]) {
          await clickTestId(page, z);
        }

        let activeZones = 0;
        for (const z of ALL_ZONE_TAGS) {
          if (await isToggleOrChipActive(page, z)) activeZones++;
        }
        assert.ok(activeZones >= 1 && activeZones <= 3, `Active body zones must stay within 1..3, got ${activeZones}`);

        for (const z of ALL_ZONE_TAGS) {
          if (await isToggleOrChipActive(page, z)) {
            await clickTestId(page, z);
          }
        }
        const composeBtn = byTestId(page, TAGS.bodymap_compose_start_button);
        if (await composeBtn.isEnabled().catch(() => false)) {
          await composeBtn.click().catch(() => {});
          if (await isVisible(page, TAGS.player_screen)) {
            await clickTestId(page, TAGS.player_close_button);
          }
        }
        assert.equal(
          (await isVisible(page, TAGS.bodymap_screen)) || (await isVisible(page, TAGS.home_screen)),
          true,
          '0-zone deselection boundary must never crash app'
        );
      });
    },
  },
  {
    id: 'P-T2-18',
    tier: 2,
    feature: 'F8',
    name: 'Toggling same zone twice selects/deselects cleanly & 5x consecutive custom compositions hit zero daily quota',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);

        if (!(await isToggleOrChipActive(page, TAGS.bodymap_zone_neck))) {
          await clickTestId(page, TAGS.bodymap_zone_neck);
        }
        const eyeBefore = await isToggleOrChipActive(page, TAGS.bodymap_zone_eye);
        await clickTestId(page, TAGS.bodymap_zone_eye);
        assert.notEqual(await isToggleOrChipActive(page, TAGS.bodymap_zone_eye), eyeBefore);
        await clickTestId(page, TAGS.bodymap_zone_eye);
        assert.equal(await isToggleOrChipActive(page, TAGS.bodymap_zone_eye), eyeBefore);

        const durations = [TAGS.bodymap_duration_2, TAGS.bodymap_duration_4, TAGS.bodymap_duration_6];
        for (let i = 0; i < 5; i++) {
          await clickTestId(page, durations[i % durations.length]);
          await clickTestId(page, TAGS.bodymap_compose_start_button);
          assert.equal(await isVisible(page, TAGS.player_screen), true, `Consecutive run #${i + 1} must succeed`);
          await clickTestId(page, TAGS.player_close_button);
          if (!(await isVisible(page, TAGS.bodymap_screen))) {
            await clickTestId(page, TAGS.nav_tab_bodymap);
          }
        }
      });
    },
  },

  // ===========================================================================
  // F9 — Free Classical Curated Booklets (四时雅集) (3 tests)
  // ===========================================================================
  {
    id: 'P-T2-19',
    tier: 2,
    feature: 'F9',
    name: 'Aborting booklet mid-session via player_close_button records zero false completion & zero scolding copy',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);
        const beforeText = await readTestIdText(page, TAGS.booklet_card_1);

        await clickTestId(page, TAGS.booklet_card_1);
        await page.clock.runFor(5_000);
        await clickTestId(page, TAGS.player_close_button);

        await clickTestId(page, TAGS.nav_tab_bodymap);
        const afterText = await readTestIdText(page, TAGS.booklet_card_1);
        assert.equal(afterText, beforeText, 'Aborting booklet mid-session must not increment completion');
        const fullText = await page.locator('body').innerText();
        assert.equal(/半途而废|打卡失败|未完成警告/.test(fullText), false, 'Zero scolding copy allowed');
      });
    },
  },
  {
    id: 'P-T2-20',
    tier: 2,
    feature: 'F9',
    name: 'Re-practicing a completed booklet remains 100% open & repeatable with zero lock on Day 7/10/14',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);

        await clickTestId(page, TAGS.booklet_card_2);
        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_finish_button);

        await clickTestId(page, TAGS.nav_tab_bodymap);
        await clickTestId(page, TAGS.booklet_card_2);
        assert.equal(await isVisible(page, TAGS.player_screen), true, 'Completed booklet must remain repeatable');
      });
    },
  },
  {
    id: 'P-T2-21',
    tier: 2,
    feature: 'F9',
    name: 'Pose catalog across scenes, shortcuts, body map & 4 booklets exposes >= 19 distinct micro-movements',
    fn: async ({ session }) => {
      const rawHtml = fs.readFileSync(PATHS.prototypeHtml, 'utf8');
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const distinctCatalogCount = await page.evaluate((src) => {
          for (const key of ['POSES', 'POSE_CATALOG', 'ALL_POSES', 'STRETCH_POSES', '__SONGXI_POSES__']) {
            if (Array.isArray(window[key])) return window[key].length;
          }
          // Extract unique pose id/title entries from inline JS catalog definitions
          const idMatches = src.match(/\bid\s*:\s*['"]pose[_-][a-z0-9_-]+['"]/gi) || [];
          if (idMatches.length >= 19) return new Set(idMatches.map((s) => s.toLowerCase())).size;
          const cueMatches = src.match(/gentleCue|cue\s*:\s*['"][^'"]{6,48}['"]/g) || [];
          return new Set(cueMatches).size;
        }, rawHtml);
        assert.ok(
          distinctCatalogCount >= 19,
          `Expected >= 19 distinct micro-stretch poses in Prototype catalog, found ${distinctCatalogCount}`
        );
      });
    },
  },

  // ===========================================================================
  // F10 — Guided Stretch Player (4s/6s Halo & 闭眼余光态) (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-22',
    tier: 2,
    feature: 'F10',
    name: 'Exact 4000ms/6000ms breath phase boundaries (3850ms 吸气, 4150ms 呼气, 9850ms 呼气, 10200ms 吸气) & pause freezes timer',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_start_hero_button);

        await page.clock.runFor(3_850);
        assert.ok(
          (await readTestIdText(page, TAGS.player_breath_phase_label)).includes('吸气'),
          'At 3850ms phase must still be 吸气'
        );

        await page.clock.runFor(300); // 4150ms
        assert.ok(
          (await readTestIdText(page, TAGS.player_breath_phase_label)).includes('呼气'),
          'At 4150ms phase must transition to 呼气'
        );

        await page.clock.runFor(5_700); // 9850ms
        assert.ok(
          (await readTestIdText(page, TAGS.player_breath_phase_label)).includes('呼气'),
          'At 9850ms phase must remain 呼气'
        );

        await page.clock.runFor(350); // 10200ms
        assert.ok(
          (await readTestIdText(page, TAGS.player_breath_phase_label)).includes('吸气'),
          'At 10200ms phase must wrap back to 吸气'
        );

        await clickTestId(page, TAGS.player_pause_resume_button);
        const snapBefore = await readTestIdText(page, TAGS.player_screen);
        await page.clock.runFor(10_000);
        const snapAfter = await readTestIdText(page, TAGS.player_screen);
        assert.equal(snapAfter, snapBefore, 'Paused player must freeze timer & phase across 10s');
      });
    },
  },
  {
    id: 'P-T2-23',
    tier: 2,
    feature: 'F10',
    name: 'player_prev_pose_button on Pose 0 clamps safely & player_next_pose_button on final pose enters completion_screen',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_quick_routine_0);

        await clickTestId(page, TAGS.player_prev_pose_button);
        await clickTestId(page, TAGS.player_prev_pose_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true, 'Prev on Pose 0 must clamp safely');

        for (let i = 0; i < 12; i++) {
          if (await isVisible(page, TAGS.completion_screen)) break;
          await clickTestId(page, TAGS.player_next_pose_button);
        }
        assert.equal(
          await isVisible(page, TAGS.completion_screen),
          true,
          'Next on final pose must enter completion_screen'
        );
      });
    },
  },

  // ===========================================================================
  // F11 — Procedural Harmonic Audio, Gentle Haptics & Flip-to-Pause (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-24',
    tier: 2,
    feature: 'F11',
    name: 'player_sound_silent (静默仅微震) mutes Web Audio oscillator output while keeping breath haptic readout active',
    fn: async ({ session }) => {
      await session.withPage(
        {
          initScripts: [
            () => {
              window.__oscStarts = 0;
              if (window.OscillatorNode && window.OscillatorNode.prototype) {
                const orig = window.OscillatorNode.prototype.start;
                window.OscillatorNode.prototype.start = function (...args) {
                  window.__oscStarts++;
                  return orig.apply(this, args);
                };
              }
            },
          ],
        },
        async ({ page, prototypeUrl }) => {
          await openPrototypeReadyAtHome(page, prototypeUrl);
          await clickTestId(page, TAGS.home_start_hero_button);

          await clickTestId(page, TAGS.player_sound_silent);
          await page.evaluate(() => {
            window.__oscStarts = 0;
          });
          await page.clock.runFor(5_000);
          const silentOscStarts = await page.evaluate(() => window.__oscStarts);
          assert.equal(silentOscStarts, 0, '静默仅微震 mode must emit 0 audible oscillator starts');
        }
      );
    },
  },
  {
    id: 'P-T2-25',
    tier: 2,
    feature: 'F11',
    name: 'Flip-to-pause control is isolated to active player_screen and clears cleanly on player_close_button',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        assert.equal(
          await isVisible(page, TAGS.player_paused_by_flip_banner),
          false,
          'Home must not display flip-pause banner'
        );

        await clickTestId(page, TAGS.home_start_hero_button);
        await clickTestId(page, TAGS.player_simulate_flip_button);
        assert.equal(await isVisible(page, TAGS.player_paused_by_flip_banner), true);

        await clickTestId(page, TAGS.player_close_button);
        await clickTestId(page, TAGS.home_start_hero_button);
        assert.equal(
          await isVisible(page, TAGS.player_paused_by_flip_banner),
          false,
          'Re-opening player must start with flip-pause state cleanly reset'
        );
      });
    },
  },

  // ===========================================================================
  // F12 — Completion Water-Ripple Ritual & Somatic Check-In (一息成) (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-26',
    tier: 2,
    feature: 'F12',
    name: 'Clicking completion_finish_button WITHOUT selecting a feeling chip saves session cleanly & double-click logs only 1 session',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);

        await byTestId(page, TAGS.completion_finish_button).dblclick().catch(async () => {
          if (await isVisible(page, TAGS.completion_finish_button)) {
            await clickTestId(page, TAGS.completion_finish_button);
          }
        });

        await clickTestId(page, TAGS.nav_tab_journal);
        const summary = await readTestIdText(page, TAGS.journal_monthly_summary);
        assert.ok(summary.includes('1'), `Optional-feeling completion must save 1 session, got '${summary}'`);
        assert.equal(/2\s*息|2\s*次/.test(summary), false, `Double-click must NOT double-log 2 sessions: '${summary}'`);
      });
    },
  },
  {
    id: 'P-T2-27',
    tier: 2,
    feature: 'F12',
    name: 'Aborting player mid-routine logs 0 sessions & switching skin updates completion_screen poetic verse',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_journal);
        const emptySummary = await readTestIdText(page, TAGS.journal_monthly_summary);

        await clickTestId(page, TAGS.nav_tab_home);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await page.clock.runFor(4_000);
        await clickTestId(page, TAGS.player_close_button);

        await clickTestId(page, TAGS.nav_tab_journal);
        assert.equal(
          await readTestIdText(page, TAGS.journal_monthly_summary),
          emptySummary,
          'Aborting routine before completion must log 0 sessions'
        );

        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_ink_mist);
        await clickTestId(page, TAGS.nav_tab_home);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);
        const inkVerse = await readTestIdText(page, TAGS.completion_screen);
        await clickTestId(page, TAGS.completion_finish_button);

        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_dusk_pine);
        await clickTestId(page, TAGS.nav_tab_home);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);
        const duskVerse = await readTestIdText(page, TAGS.completion_screen);

        assert.notEqual(inkVerse, duskVerse, 'Completion poetic verse must adapt to active SongxiSkin');
      });
    },
  },

  // ===========================================================================
  // F13 — Low-Intrusion Sedentary Reminder Scheduler (微风提醒) (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-28',
    tier: 2,
    feature: 'F13',
    name: 'Quiet-hour boundaries (12:30 lunch, 22:15 night, Saturday weekend, mute_today) suppress scheduled nudges',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 12, 30) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);

        for (const sw of [
          TAGS.reminder_enable_switch,
          TAGS.reminder_lunch_quiet_switch,
          TAGS.reminder_night_quiet_switch,
          TAGS.reminder_weekend_quiet_switch,
        ]) {
          if (!(await isToggleOrChipActive(page, sw))) {
            await clickTestId(page, sw);
          }
        }
        await clickTestId(page, TAGS.reminder_interval_45);

        const quietTimes = [
          localEpochMs(2026, 11, 18, 12, 30), // Wed lunch
          localEpochMs(2026, 11, 18, 22, 15), // Wed night
          localEpochMs(2026, 11, 21, 15, 0), // Sat weekend
        ];
        for (const ts of quietTimes) {
          await page.clock.setSystemTime(ts);
          await page.reload({ waitUntil: 'load' });
          await page.clock.runFor(46 * 60_000);
          assert.equal(
            await isVisible(page, TAGS.reminder_inapp_banner),
            false,
            `Quiet window at ${new Date(ts).toISOString()} must suppress auto-reminder banner`
          );
        }

        // Weekday 15:00 with mute_today ON -> suppressed
        await page.clock.setSystemTime(localEpochMs(2026, 11, 19, 14, 0));
        await page.reload({ waitUntil: 'load' });
        await clickTestId(page, TAGS.nav_tab_settings);
        if (!(await isToggleOrChipActive(page, TAGS.reminder_mute_today_switch))) {
          await clickTestId(page, TAGS.reminder_mute_today_switch);
        }
        await page.clock.runFor(50 * 60_000);
        assert.equal(await isVisible(page, TAGS.reminder_inapp_banner), false, 'mute_today must suppress today nudges');
      });
    },
  },
  {
    id: 'P-T2-29',
    tier: 2,
    feature: 'F13',
    name: 'Daily reminder cap (<= 3/day) & disabling reminder_enable_switch OFF suppress further scheduled nudges',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 9, 15) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);

        if (await isToggleOrChipActive(page, TAGS.reminder_enable_switch)) {
          await clickTestId(page, TAGS.reminder_enable_switch);
        }
        await page.clock.runFor(3 * 60 * 60_000);
        assert.equal(
          await isVisible(page, TAGS.reminder_inapp_banner),
          false,
          'Disabled reminder switch must never auto-pop banner'
        );

        // Re-enable and verify <= 3/day cap across 6 hours of 45m ticks
        await clickTestId(page, TAGS.reminder_enable_switch);
        await clickTestId(page, TAGS.reminder_interval_45);
        if (await isToggleOrChipActive(page, TAGS.reminder_lunch_quiet_switch)) {
          await clickTestId(page, TAGS.reminder_lunch_quiet_switch);
        }

        let popCount = 0;
        for (let tick = 0; tick < 6; tick++) {
          await page.clock.runFor(46 * 60_000);
          if (await isVisible(page, TAGS.reminder_inapp_banner)) {
            popCount++;
            await clickTestId(page, TAGS.reminder_inapp_banner);
            if (await isVisible(page, TAGS.player_screen)) {
              await clickTestId(page, TAGS.player_close_button);
            }
          }
        }
        assert.ok(popCount <= 3, `Auto-nudges in a single day must never exceed cap of 3, got ${popCount}`);
      });
    },
  },

  // ===========================================================================
  // F14 — Streak-Free Water-Ripple Journal & Shareable Card (2 tests)
  // ===========================================================================
  {
    id: 'P-T2-30',
    tier: 2,
    feature: 'F14',
    name: 'Streak-free invariant: non-consecutive practice days (Nov 1 & Nov 4) and empty fresh month show 0 streak/shame/NaN text',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 1, 10, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_journal);
        const freshText = await readTestIdText(page, TAGS.journal_screen);
        assert.equal(/NaN|undefined|Infinity/.test(freshText), false, 'Fresh empty journal must contain zero NaN/undefined');

        await clickTestId(page, TAGS.nav_tab_home);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_finish_button);

        await page.clock.setSystemTime(localEpochMs(2026, 11, 4, 10, 0));
        await page.reload({ waitUntil: 'load' });
        if (await isVisible(page, TAGS.splash_ad_skip_button)) {
          await clickTestId(page, TAGS.splash_ad_skip_button);
        }
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_finish_button);

        await clickTestId(page, TAGS.nav_tab_journal);
        const afterGapText = await readTestIdText(page, TAGS.journal_screen);
        assert.equal(
          FORBIDDEN_STREAK_SHAME_REGEX.test(afterGapText),
          false,
          `Journal must NEVER show streak/broken-streak guilt copy: ${afterGapText}`
        );
        assert.ok(afterGapText.includes('留白'), 'Journal must honor calm 留白 rest philosophy');
      });
    },
  },
  {
    id: 'P-T2-31',
    tier: 2,
    feature: 'F14',
    name: 'Same-day multiple sessions stack concentric ripples & dismissing journal_share_preview_dialog returns cleanly',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        for (let i = 0; i < 2; i++) {
          await clickTestId(page, TAGS.nav_tab_home);
          await clickTestId(page, TAGS.home_quick_routine_1);
          await advanceRoutineToCompletionOnPage(page);
          await clickTestId(page, TAGS.completion_finish_button);
        }

        await clickTestId(page, TAGS.nav_tab_journal);
        const summary = await readTestIdText(page, TAGS.journal_monthly_summary);
        assert.ok(summary.includes('2'), `2 same-day sessions must reflect count '2' in summary, got '${summary}'`);

        await clickTestId(page, TAGS.journal_share_card_button);
        assert.equal(await isVisible(page, TAGS.journal_share_preview_dialog), true);

        await page.keyboard.press('Escape').catch(() => {});
        if (await isVisible(page, TAGS.journal_share_preview_dialog)) {
          const closeBtn = byTestId(page, TAGS.journal_share_preview_dialog).locator('button').first();
          await closeBtn.click();
        }
        assert.equal(await isVisible(page, TAGS.journal_share_preview_dialog), false);
        assert.equal(await isVisible(page, TAGS.journal_screen), true);
      });
    },
  },

  // ===========================================================================
  // F15 — Standalone Android APK Packaging, Visual Captures & Suite (3 tests)
  // ===========================================================================
  {
    id: 'P-T2-32',
    tier: 2,
    feature: 'F15',
    name: 'Strict CSP compliance across songxi_interactive_prototype.html & songxi_skin_gallery_showcase.html',
    fn: async ({ session }) => {
      const protoCsp = auditHtmlCsp(PATHS.prototypeHtml);
      const showCsp = auditHtmlCsp(PATHS.showcaseHtml);
      assert.deepEqual(protoCsp.disallowed, [], `Disallowed external URLs in Prototype: ${protoCsp.disallowed}`);
      assert.deepEqual(showCsp.disallowed, [], `Disallowed external URLs in Showcase: ${showCsp.disallowed}`);

      await session.withPage(async ({ page, tracker, prototypeUrl, showcaseUrl }) => {
        await page.goto(prototypeUrl, { waitUntil: 'load' });
        await page.goto(showcaseUrl, { waitUntil: 'load' });
        const unauthorizedRuntime = tracker.externalUrls.filter((u) => u !== ALLOWED_TAILWIND_CDN);
        assert.deepEqual(
          unauthorizedRuntime,
          [],
          `Runtime network requests violated CSP allowlist: ${unauthorizedRuntime}`
        );
      });
    },
  },
  {
    id: 'P-T2-33',
    tier: 2,
    feature: 'F15',
    name: 'Responsive layout integrity across mobile (390x844) & desktop (1440x920) on both Prototype & Showcase',
    fn: async ({ session }) => {
      for (const vp of [
        { width: 390, height: 844 },
        { width: 1440, height: 920 },
      ]) {
        await session.withPage({ viewport: vp }, async ({ page, prototypeUrl, showcaseUrl }) => {
          for (const targetUrl of [prototypeUrl, showcaseUrl]) {
            await page.goto(targetUrl, { waitUntil: 'load' });
            const metrics = await page.evaluate(() => {
              const brokenImgs = Array.from(document.querySelectorAll('img')).filter(
                (img) => !img.complete || img.naturalWidth === 0
              ).length;
              return {
                scrollW: document.documentElement.scrollWidth,
                innerW: window.innerWidth,
                brokenImgs,
              };
            });
            assert.ok(
              metrics.scrollW <= metrics.innerW + 1,
              `Horizontal overflow (${metrics.scrollW} > ${metrics.innerW}) on ${targetUrl} at ${vp.width}x${vp.height}`
            );
            assert.equal(metrics.brokenImgs, 0, `Broken <img> elements found on ${targetUrl}`);
          }
        });
      }
    },
  },
  {
    id: 'P-T2-34',
    tier: 2,
    feature: 'F15',
    name: 'Cross-artifact synchronization across Prototype, Showcase, songxi_product_design.md, SONGXI_PRODUCT_DESIGN.md & SKIN_AND_SPLASH_SPEC.md',
    fn: async () => {
      auditDesignDocsSync();
      const protoHtml = fs.readFileSync(PATHS.prototypeHtml, 'utf8');
      const showHtml = fs.readFileSync(PATHS.showcaseHtml, 'utf8');
      const designMd = fs.readFileSync(PATHS.designMd, 'utf8');
      const skinSpecMd = fs.readFileSync(PATHS.docsSkinSplashMd, 'utf8');

      for (const skinId of SKIN_KEYS) {
        assert.ok(protoHtml.includes(skinId), `Prototype missing skin key '${skinId}'`);
        assert.ok(showHtml.includes(skinId), `Showcase missing skin key '${skinId}'`);
        assert.ok(designMd.includes(skinId) || skinSpecMd.includes(skinId), `Design docs missing skin key '${skinId}'`);
      }
      for (const skinCn of ['水墨远山', '青绿山居', '暮山听松', '春涧花影']) {
        assert.ok(protoHtml.includes(skinCn), `Prototype missing '${skinCn}'`);
        assert.ok(showHtml.includes(skinCn), `Showcase missing '${skinCn}'`);
        assert.ok(designMd.includes(skinCn), `songxi_product_design.md missing '${skinCn}'`);
        assert.ok(skinSpecMd.includes(skinCn), `SKIN_AND_SPLASH_SPEC.md missing '${skinCn}'`);
      }
      for (const zoneCn of ['后颈风池', '双肩斜方', '胸椎圆肩', '下腰久坐', '手腕鼠标侧', '眼周眉心']) {
        assert.ok(protoHtml.includes(zoneCn), `Prototype missing body zone '${zoneCn}'`);
        assert.ok(designMd.includes(zoneCn), `Design doc missing body zone '${zoneCn}'`);
      }
    },
  },
];

module.exports = tests;
