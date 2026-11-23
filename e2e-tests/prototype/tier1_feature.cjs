'use strict';

process.env.TZ = 'Asia/Shanghai';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  PATHS,
  TAGS,
  SKIN_KEYS,
  SKIN_CN_LABELS,
  FORBIDDEN_PROTOTYPE_REGEX,
  FORBIDDEN_STREAK_SHAME_REGEX,
  localEpochMs,
  byTestId,
  isVisible,
  clickTestId,
  readTestIdText,
  isToggleOrChipActive,
  openPrototypeReadyAtHome,
  advanceRoutineToCompletionOnPage,
  readShellColorsAndContrast,
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
    id: 'P-T1-01',
    tier: 1,
    feature: 'F1',
    name: 'Fresh prototype launch displays gentle onboarding card & enters zero-login Home sanctuary',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 9, 0) }, async ({ page, prototypeUrl }) => {
        await page.goto(prototypeUrl, { waitUntil: 'load' });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false, 'First launch must bypass splash ad');
        assert.equal(await isVisible(page, TAGS.onboarding_card), true, 'Fresh launch must show onboarding_card');
        assert.equal(
          await isVisible(page, TAGS.onboarding_enter_button),
          true,
          'Fresh launch must show onboarding_enter_button'
        );

        const loginInputCount = await page.locator('input[type="tel"], input[type="email"], input[type="password"]').count();
        assert.equal(loginInputCount, 0, 'Prototype must contain zero phone/email/password login inputs');

        await clickTestId(page, TAGS.onboarding_enter_button);
        assert.equal(await isVisible(page, TAGS.onboarding_card), false, 'onboarding_card must dismiss on enter');
        assert.equal(await isVisible(page, TAGS.home_screen), true, 'home_screen must become visible');
      });
    },
  },
  {
    id: 'P-T1-02',
    tier: 1,
    feature: 'F1',
    name: 'Onboarding state persists in localStorage across reload and all 5 nav_tab_* tabs navigate in 1 click',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 9, 10) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await page.reload({ waitUntil: 'load' });
        assert.equal(
          await isVisible(page, TAGS.onboarding_card),
          false,
          'onboarding_card must stay dismissed across page.reload()'
        );

        const navPairs = [
          [TAGS.nav_tab_bodymap, TAGS.bodymap_screen],
          [TAGS.nav_tab_skins, TAGS.skins_screen],
          [TAGS.nav_tab_journal, TAGS.journal_screen],
          [TAGS.nav_tab_settings, TAGS.settings_screen],
          [TAGS.nav_tab_home, TAGS.home_screen],
        ];
        for (const [tabTag, screenTag] of navPairs) {
          await clickTestId(page, tabTag);
          assert.equal(await isVisible(page, screenTag), true, `Clicking ${tabTag} must display ${screenTag}`);
        }
      });
    },
  },

  // ===========================================================================
  // F2 — Editorial Framed Splash Ad UI (开屏雅鉴) & Safe CTA (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-03',
    tier: 1,
    feature: 'F2',
    name: 'Manual splash preview renders framed 开屏雅鉴 stage, 广告 badge, countdown skip & seal footer',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, TAGS.splash_preview_trigger_button);

        for (const tag of [
          TAGS.splash_ad_screen,
          TAGS.splash_ad_backdrop,
          TAGS.splash_ad_badge,
          TAGS.splash_ad_skip_button,
          TAGS.splash_ad_brand_footer,
        ]) {
          assert.equal(await isVisible(page, tag), true, `Expected ${tag} visible on splash stage`);
        }

        const badgeText = await readTestIdText(page, TAGS.splash_ad_badge);
        const skipText = await readTestIdText(page, TAGS.splash_ad_skip_button);
        const footerText = await readTestIdText(page, TAGS.splash_ad_brand_footer);
        assert.ok(badgeText.includes('广告'), `splash_ad_badge must include '广告', got '${badgeText}'`);
        assert.ok(skipText.includes('跳过'), `splash_ad_skip_button must include '跳过', got '${skipText}'`);
        assert.ok(footerText.includes('松息'), `splash_ad_brand_footer must include '松息', got '${footerText}'`);

        const shellBox = await byTestId(page, TAGS.splash_ad_screen).boundingBox();
        const footerBox = await byTestId(page, TAGS.splash_ad_brand_footer).boundingBox();
        assert.ok(shellBox && footerBox, 'Splash screen and footer must have measurable boxes');
        const ratio = footerBox.height / shellBox.height;
        assert.ok(ratio >= 0.14 && ratio <= 0.32, `Footer height ratio (${ratio.toFixed(3)}) must be ~22% [0.14..0.32]`);
      });
    },
  },
  {
    id: 'P-T1-04',
    tier: 1,
    feature: 'F2',
    name: 'Splash 跳过 button dismisses immediately and bounded CTA opens safe confirmation sheet',
    fn: async ({ session }) => {
      await session.withPage(
        {
          initScripts: [
            () => {
              window.__openedWindows = [];
              window.open = (url) => {
                window.__openedWindows.push(url);
                return null;
              };
            },
          ],
        },
        async ({ page, prototypeUrl }) => {
          await openPrototypeReadyAtHome(page, prototypeUrl);
          await clickTestId(page, TAGS.nav_tab_settings);
          await clickTestId(page, TAGS.splash_preview_trigger_button);

          await clickTestId(page, TAGS.splash_ad_cta_button);
          assert.equal(await isVisible(page, TAGS.splash_ad_confirm_sheet), true, 'CTA must open confirmation sheet');
          assert.equal(await isVisible(page, TAGS.splash_ad_confirm_dismiss), true, 'Dismiss control must be visible');

          const autoOpened = await page.evaluate(() => window.__openedWindows.length);
          assert.equal(autoOpened, 0, 'Clicking CTA must NOT auto-open external window before confirmation');

          await clickTestId(page, TAGS.splash_ad_confirm_dismiss);
          assert.equal(await isVisible(page, TAGS.splash_ad_confirm_sheet), false, 'Confirm sheet must close on dismiss');

          await clickTestId(page, TAGS.splash_ad_skip_button);
          assert.equal(await isVisible(page, TAGS.splash_ad_screen), false, 'Skip button must dismiss splash immediately');
        }
      );
    },
  },

  // ===========================================================================
  // F3 — 7-Gate Calm Splash-Ad Policy & Privacy Controls (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-05',
    tier: 1,
    feature: 'F3',
    name: 'Gate 1 first-launch grace bypasses splash while 2nd cold launch (+4h05m) displays splash',
    fn: async ({ session }) => {
      const t0900 = localEpochMs(2026, 11, 18, 9, 0);
      await session.withPage({ timeMs: t0900 }, async ({ page, prototypeUrl }) => {
        await page.goto(prototypeUrl, { waitUntil: 'load' });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false, 'Gate 1 grace must bypass splash on Launch #1');
        if (await isVisible(page, TAGS.onboarding_enter_button)) {
          await clickTestId(page, TAGS.onboarding_enter_button);
        }
        assert.equal(await isVisible(page, TAGS.home_screen), true);

        await page.clock.setSystemTime(t0900 + 245 * 60_000);
        await page.reload({ waitUntil: 'load' });
        assert.equal(
          await isVisible(page, TAGS.splash_ad_screen),
          true,
          '2nd cold launch after +4h05m must display splash_ad_screen'
        );
      });
    },
  },
  {
    id: 'P-T1-06',
    tier: 1,
    feature: 'F3',
    name: 'Splash personalization switch persists in localStorage and reminder quick-start bypasses splash',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);

        const before = await isToggleOrChipActive(page, TAGS.splash_personalized_switch);
        await clickTestId(page, TAGS.splash_personalized_switch);
        const after = await isToggleOrChipActive(page, TAGS.splash_personalized_switch);
        assert.notEqual(before, after, 'splash_personalized_switch must toggle');

        await page.reload({ waitUntil: 'load' });
        if (await isVisible(page, TAGS.splash_ad_skip_button)) {
          await clickTestId(page, TAGS.splash_ad_skip_button);
        }
        await clickTestId(page, TAGS.nav_tab_settings);
        assert.equal(
          await isToggleOrChipActive(page, TAGS.splash_personalized_switch),
          after,
          'splash_personalized_switch must persist across reload'
        );

        await clickTestId(page, TAGS.reminder_preview_trigger_button);
        assert.equal(await isVisible(page, TAGS.reminder_inapp_banner), true);
        await clickTestId(page, TAGS.reminder_inapp_banner);
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false, 'Reminder quick-start must bypass splash');
        assert.equal(await isVisible(page, TAGS.player_screen), true, 'Reminder quick-start must open player_screen');
      });
    },
  },

  // ===========================================================================
  // F4 — Zero-Membership & 100% Unlocked Content Invariant (3 tests)
  // ===========================================================================
  {
    id: 'P-T1-07',
    tier: 1,
    feature: 'F4',
    name: 'Prototype unlocks all 4 skins, 4 booklets, soundscapes & repeated custom compositions free',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);

        await clickTestId(page, TAGS.nav_tab_skins);
        for (const skinTag of ALL_SKIN_CARD_TAGS) {
          await clickTestId(page, skinTag);
        }

        await clickTestId(page, TAGS.nav_tab_bodymap);
        await clickTestId(page, TAGS.booklet_card_0);
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        await clickTestId(page, TAGS.player_sound_selector);
        await clickTestId(page, TAGS.player_sound_silent);
        await clickTestId(page, TAGS.player_close_button);

        await clickTestId(page, TAGS.nav_tab_bodymap);
        await clickTestId(page, TAGS.bodymap_zone_neck);
        for (let i = 0; i < 4; i++) {
          await clickTestId(page, TAGS.bodymap_compose_start_button);
          assert.equal(await isVisible(page, TAGS.player_screen), true, `Custom run #${i + 1} must open player free`);
          await clickTestId(page, TAGS.player_close_button);
          if (!(await isVisible(page, TAGS.bodymap_screen))) {
            await clickTestId(page, TAGS.nav_tab_bodymap);
          }
        }
      });
    },
  },
  {
    id: 'P-T1-08',
    tier: 1,
    feature: 'F4',
    name: 'songxi_skin_gallery_showcase.html presents all 4 skins as 100% free built-in themes',
    fn: async ({ session }) => {
      await session.withPage({ viewport: { width: 1280, height: 860 } }, async ({ page, showcaseUrl }) => {
        await page.goto(showcaseUrl, { waitUntil: 'load' });
        const bodyText = await page.locator('body').innerText();
        for (const label of ['水墨远山', '青绿山居', '暮山听松', '春涧花影']) {
          assert.ok(bodyText.includes(label), `Showcase must display free built-in skin '${label}'`);
        }
        assert.equal(
          FORBIDDEN_PROTOTYPE_REGEX.test(bodyText),
          false,
          'Showcase must contain zero VIP/paywall badges'
        );
      });
    },
  },
  {
    id: 'P-T1-09',
    tier: 1,
    feature: 'F4',
    name: 'songxi_product_design.md v2, SONGXI_PRODUCT_DESIGN.md & SKIN_AND_SPLASH_SPEC.md establish zero-membership splash-ad-only model',
    fn: async () => {
      auditDesignDocsSync();
      for (const docPath of [PATHS.designMd, PATHS.docsDesignMd, PATHS.docsSkinSplashMd]) {
        const txt = fs.readFileSync(docPath, 'utf8');
        assert.ok(
          /不做会员|零会员|无会员|全功能免费|100%\s*免费/.test(txt),
          `${docPath} must explicitly declare zero-membership / 100% free functionality`
        );
        assert.ok(txt.includes('开屏雅鉴'), `${docPath} must explicitly document 开屏雅鉴 splash monetization`);
      }
    },
  },

  // ===========================================================================
  // F5 — 5-Dimensional Illustration × Shan-Shui Skin Engine (3 tests)
  // ===========================================================================
  {
    id: 'P-T1-10',
    tier: 1,
    feature: 'F5',
    name: 'Prototype renders layered Shan-Shui SVG/Canvas backdrop & animated editorial stretch figure',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const backdropBox = await byTestId(page, TAGS.home_shanshui_backdrop).boundingBox();
        assert.ok(
          backdropBox && backdropBox.width >= 80 && backdropBox.height >= 80,
          `home_shanshui_backdrop must have non-trivial dimensions, got ${JSON.stringify(backdropBox)}`
        );
        const backdropRich = await byTestId(page, TAGS.home_shanshui_backdrop).evaluate((el) => {
          const svgElements = el.querySelectorAll('svg, path, circle, ellipse, linearGradient, canvas').length;
          return svgElements > 0 || el.tagName.toLowerCase() === 'svg' || el.tagName.toLowerCase() === 'canvas';
        });
        assert.equal(backdropRich, true, 'home_shanshui_backdrop must contain real SVG/Canvas artwork layers');

        await clickTestId(page, TAGS.home_start_hero_button);
        const figBox = await byTestId(page, TAGS.player_figure_canvas).boundingBox();
        assert.ok(
          figBox && figBox.width >= 60 && figBox.height >= 60,
          `player_figure_canvas must have non-trivial dimensions, got ${JSON.stringify(figBox)}`
        );
      });
    },
  },
  {
    id: 'P-T1-11',
    tier: 1,
    feature: 'F5',
    name: 'Switching skins in Prototype updates all 5 dimensions (palette, Shan-Shui layer, figure/seal, poetic greeting, audio profile)',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);

        const captureSkinSnapshot = async (skinCardTag) => {
          await clickTestId(page, TAGS.nav_tab_skins);
          await clickTestId(page, skinCardTag);
          await clickTestId(page, TAGS.nav_tab_home);
          const colors = await readShellColorsAndContrast(page);
          const backdropMarkup = await byTestId(page, TAGS.home_shanshui_backdrop).innerHTML();
          const homeCopy = await readTestIdText(page, TAGS.home_screen);
          await clickTestId(page, TAGS.home_start_hero_button);
          const soundLabel = await readTestIdText(page, TAGS.player_sound_selector);
          await clickTestId(page, TAGS.player_close_button);
          return { bgCss: colors.bgCss, backdropMarkup, homeCopy, soundLabel };
        };

        const ink = await captureSkinSnapshot(TAGS.skin_card_ink_mist);
        const qinglv = await captureSkinSnapshot(TAGS.skin_card_qinglv_celadon);
        const dusk = await captureSkinSnapshot(TAGS.skin_card_dusk_pine);

        assert.equal(new Set([ink.bgCss, qinglv.bgCss, dusk.bgCss]).size, 3, 'Surface palettes must update per skin');
        assert.notEqual(ink.homeCopy, qinglv.homeCopy, 'Editorial Home greeting/badge must update per skin');
        assert.ok(
          ink.backdropMarkup !== qinglv.backdropMarkup || ink.soundLabel !== qinglv.soundLabel,
          'Shan-Shui backdrop or sensory profile must update on skin switch'
        );
      });
    },
  },
  {
    id: 'P-T1-12',
    tier: 1,
    feature: 'F5',
    name: 'songxi_skin_gallery_showcase.html renders 5-dimensional token boards & live Shan-Shui × Illustration previews',
    fn: async ({ session }) => {
      await session.withPage({ viewport: { width: 1366, height: 900 } }, async ({ page, showcaseUrl }) => {
        await page.goto(showcaseUrl, { waitUntil: 'load' });
        const rawHtml = await page.content();
        const text = await page.locator('body').innerText();

        for (const k of SKIN_KEYS) {
          assert.ok(rawHtml.includes(k) || text.includes(SKIN_CN_LABELS[k]), `Showcase must include skin ${k}`);
        }
        for (const hz of ['432', '528', '396', '440']) {
          assert.ok(text.includes(hz) || rawHtml.includes(hz), `Showcase must present harmonic frequency ${hz}Hz`);
        }
        const graphicCount = await page.locator('svg, canvas').count();
        assert.ok(graphicCount >= 4, `Showcase must render >= 4 SVG/Canvas illustrations, found ${graphicCount}`);
      });
    },
  },

  // ===========================================================================
  // F6 — 4 Free Built-In Skins, Live Gallery & Auto-Night Pairing (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-13',
    tier: 1,
    feature: 'F6',
    name: '山居画境 gallery hot-swaps all 4 skins live without page reload and auditions harmonic chime',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await page.evaluate(() => {
          window.__liveSessionToken = 'same_dom_instance';
        });

        await clickTestId(page, TAGS.nav_tab_skins);
        const bgSet = new Set();
        for (const skinTag of ALL_SKIN_CARD_TAGS) {
          await clickTestId(page, skinTag);
          const { bgCss } = await readShellColorsAndContrast(page);
          bgSet.add(bgCss);
        }
        assert.equal(bgSet.size, 4, 'All 4 skins must hot-swap distinct background colors live');

        await clickTestId(page, TAGS.skin_chime_preview_button);
        const tokenAfter = await page.evaluate(() => window.__liveSessionToken);
        assert.equal(tokenAfter, 'same_dom_instance', 'Hot-swapping skins must never trigger full page reload');
      });
    },
  },
  {
    id: 'P-T1-14',
    tier: 1,
    feature: 'F6',
    name: 'Selected skin and skin_auto_night_switch persist across browser reload via localStorage',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 11, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_spring_blossom);

        if (!(await isToggleOrChipActive(page, TAGS.skin_auto_night_switch))) {
          await clickTestId(page, TAGS.skin_auto_night_switch);
        }
        assert.equal(await isToggleOrChipActive(page, TAGS.skin_auto_night_switch), true);

        await page.reload({ waitUntil: 'load' });
        const badge = await readTestIdText(page, TAGS.home_active_skin_badge);
        assert.ok(badge.includes('春'), `Expected spring_blossom ('春') persisted on Home badge, got '${badge}'`);

        await clickTestId(page, TAGS.nav_tab_skins);
        assert.equal(
          await isToggleOrChipActive(page, TAGS.skin_auto_night_switch),
          true,
          'skin_auto_night_switch must stay ON after reload'
        );
      });
    },
  },

  // ===========================================================================
  // F7 — Context-Aware Home (今日一息) & Quick Micro-Shortcuts (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-15',
    tier: 1,
    feature: 'F7',
    name: '今日一息 Home switches desk / tea / bed posture scenes & starts Hero routine in 1 click',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        assert.equal(await isVisible(page, TAGS.home_active_skin_badge), true);
        assert.equal(await isVisible(page, TAGS.home_hero_card), true);

        await clickTestId(page, TAGS.home_scene_desk);
        const deskText = await readTestIdText(page, TAGS.home_hero_card);
        await clickTestId(page, TAGS.home_scene_tea);
        const teaText = await readTestIdText(page, TAGS.home_hero_card);
        await clickTestId(page, TAGS.home_scene_bed);
        const bedText = await readTestIdText(page, TAGS.home_hero_card);

        assert.equal(new Set([deskText, teaText, bedText]).size, 3, '3 posture scenes must update hero prescription');

        await clickTestId(page, TAGS.home_start_hero_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true, '1-click Hero start must open player_screen');
      });
    },
  },
  {
    id: 'P-T1-16',
    tier: 1,
    feature: 'F7',
    name: 'All 4 片刻救急轻帖 micro-shortcuts (home_quick_routine_0..3) launch targeted routines',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const playerSnapshots = new Set();
        for (const tag of [
          TAGS.home_quick_routine_0,
          TAGS.home_quick_routine_1,
          TAGS.home_quick_routine_2,
          TAGS.home_quick_routine_3,
        ]) {
          await clickTestId(page, tag);
          assert.equal(await isVisible(page, TAGS.player_screen), true, `${tag} must launch player_screen`);
          playerSnapshots.add(await readTestIdText(page, TAGS.player_screen));
          await clickTestId(page, TAGS.player_close_button);
        }
        assert.ok(playerSnapshots.size >= 3, 'Quick relief shortcuts must launch distinct targeted micro-routines');
      });
    },
  },

  // ===========================================================================
  // F8 — 6-Zone Body Tension Map & Unlimited Custom Composer (身体寻音) (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-17',
    tier: 1,
    feature: 'F8',
    name: '身体寻音 renders bodymap_canvas and all 6 interactive tension zones with multi-select feedback',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);

        const canvasBox = await byTestId(page, TAGS.bodymap_canvas).boundingBox();
        assert.ok(
          canvasBox && canvasBox.width > 80 && canvasBox.height > 80,
          `bodymap_canvas must render measurable body silhouette, got ${JSON.stringify(canvasBox)}`
        );

        for (const zTag of ALL_ZONE_TAGS) {
          assert.equal(await isVisible(page, zTag), true, `Zone ${zTag} must be visible`);
        }
        await clickTestId(page, TAGS.bodymap_zone_shoulder);
        assert.equal(await isVisible(page, TAGS.bodymap_compose_start_button), true);
      });
    },
  },
  {
    id: 'P-T1-18',
    tier: 1,
    feature: 'F8',
    name: 'Selecting zones + 2 / 4 / 6 min duration pills (bodymap_duration_2/4/6) composes & launches custom flow',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);

        await clickTestId(page, TAGS.bodymap_zone_neck);
        await clickTestId(page, TAGS.bodymap_zone_wrist);
        for (const durTag of [TAGS.bodymap_duration_2, TAGS.bodymap_duration_4, TAGS.bodymap_duration_6]) {
          await clickTestId(page, durTag);
        }

        await clickTestId(page, TAGS.bodymap_compose_start_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true, 'Custom composer must launch player_screen');
      });
    },
  },

  // ===========================================================================
  // F9 — Free Classical Curated Booklets (四时雅集) (3 tests)
  // ===========================================================================
  {
    id: 'P-T1-19',
    tier: 1,
    feature: 'F9',
    name: 'All 4 四时雅集 classical booklets (booklet_card_0..3) render unlocked with series titles',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);

        const combinedTitles = [];
        for (const bTag of [TAGS.booklet_card_0, TAGS.booklet_card_1, TAGS.booklet_card_2, TAGS.booklet_card_3]) {
          await byTestId(page, bTag).scrollIntoViewIfNeeded().catch(() => {});
          assert.equal(await isVisible(page, bTag), true, `${bTag} must be visible`);
          const txt = await readTestIdText(page, bTag);
          assert.ok(txt.length >= 4, `${bTag} must have non-empty title`);
          assert.equal(FORBIDDEN_PROTOTYPE_REGEX.test(txt), false, `${bTag} must have zero VIP lock copy`);
          combinedTitles.push(txt);
        }
        assert.equal(new Set(combinedTitles).size, 4, 'All 4 booklets must have distinct titles');
      });
    },
  },
  {
    id: 'P-T1-20',
    tier: 1,
    feature: 'F9',
    name: 'Clicking each of booklet_card_0..3 launches its guided routine in player_screen',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        for (const bTag of [TAGS.booklet_card_0, TAGS.booklet_card_1, TAGS.booklet_card_2, TAGS.booklet_card_3]) {
          await clickTestId(page, TAGS.nav_tab_bodymap);
          await clickTestId(page, bTag);
          assert.equal(await isVisible(page, TAGS.player_screen), true, `${bTag} must start player_screen`);
          await clickTestId(page, TAGS.player_close_button);
        }
      });
    },
  },
  {
    id: 'P-T1-21',
    tier: 1,
    feature: 'F9',
    name: 'Completing a booklet session persists calm local progress across reload',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);
        const beforeText = await readTestIdText(page, TAGS.booklet_card_0);

        await clickTestId(page, TAGS.booklet_card_0);
        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_finish_button);

        await page.reload({ waitUntil: 'load' });
        await clickTestId(page, TAGS.nav_tab_bodymap);
        const afterText = await readTestIdText(page, TAGS.booklet_card_0);
        assert.notEqual(beforeText, afterText, 'Completed booklet progress must persist across reload');
      });
    },
  },

  // ===========================================================================
  // F10 — Guided Stretch Player (4s/6s Halo & 闭眼余光态) (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-22',
    tier: 1,
    feature: 'F10',
    name: 'Guided Player transitions 4s inhale (吸气) -> 6s exhale (呼气) and toggles 闭眼余光态',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_start_hero_button);

        assert.equal(await isVisible(page, TAGS.player_figure_canvas), true);
        assert.equal(await isVisible(page, TAGS.player_breathing_halo), true);
        const t0Phase = await readTestIdText(page, TAGS.player_breath_phase_label);
        assert.ok(t0Phase.includes('吸气'), `Expected '吸气' at t=0, got '${t0Phase}'`);

        await page.clock.runFor(4_300);
        const exhalePhase = await readTestIdText(page, TAGS.player_breath_phase_label);
        assert.ok(exhalePhase.includes('呼气'), `Expected '呼气' at t=4.3s, got '${exhalePhase}'`);

        const beforeClass = await byTestId(page, TAGS.player_screen).evaluate((el) => el.outerHTML);
        await clickTestId(page, TAGS.player_eyes_free_toggle);
        const afterClass = await byTestId(page, TAGS.player_screen).evaluate((el) => el.outerHTML);
        assert.notEqual(beforeClass, afterClass, 'Toggling player_eyes_free_toggle must activate 闭眼余光态 state');
        await clickTestId(page, TAGS.player_eyes_free_toggle);
      });
    },
  },
  {
    id: 'P-T1-23',
    tier: 1,
    feature: 'F10',
    name: 'Player pause/resume, next, prev, and close controls operate deterministically',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_start_hero_button);

        await clickTestId(page, TAGS.player_pause_resume_button);
        await clickTestId(page, TAGS.player_pause_resume_button);
        await clickTestId(page, TAGS.player_next_pose_button);
        await clickTestId(page, TAGS.player_prev_pose_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true);

        await clickTestId(page, TAGS.player_close_button);
        assert.equal(await isVisible(page, TAGS.player_screen), false, 'player_close_button must exit player');
      });
    },
  },

  // ===========================================================================
  // F11 — Procedural Harmonic Audio, Gentle Haptics & Flip-to-Pause (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-24',
    tier: 1,
    feature: 'F11',
    name: 'Web Audio harmonic soundscape selector & 静默仅微震 mode switch cleanly without autoplay errors',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_start_hero_button);

        await clickTestId(page, TAGS.player_sound_selector);
        await clickTestId(page, TAGS.player_sound_silent);
        const txt = await readTestIdText(page, TAGS.player_screen);
        assert.ok(
          txt.includes('静默') || (await isToggleOrChipActive(page, TAGS.player_sound_silent)),
          'player_sound_silent must activate silent haptic-only mode'
        );
      });
    },
  },
  {
    id: 'P-T1-25',
    tier: 1,
    feature: 'F11',
    name: 'player_simulate_flip_button triggers 翻转静默暂存 banner, pauses timer, and resumes cleanly',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_start_hero_button);

        await clickTestId(page, TAGS.player_simulate_flip_button);
        assert.equal(
          await isVisible(page, TAGS.player_paused_by_flip_banner),
          true,
          'Flip button must display player_paused_by_flip_banner'
        );

        const frozenPhase = await readTestIdText(page, TAGS.player_breath_phase_label);
        await page.clock.runFor(4_500);
        const afterRunPhase = await readTestIdText(page, TAGS.player_breath_phase_label);
        assert.equal(afterRunPhase, frozenPhase, 'Breath timer must remain frozen during face-down pause');

        await clickTestId(page, TAGS.player_simulate_flip_button);
        if (await isVisible(page, TAGS.player_paused_by_flip_banner)) {
          await clickTestId(page, TAGS.player_pause_resume_button);
        }
        assert.equal(await isVisible(page, TAGS.player_paused_by_flip_banner), false, 'Flip banner must hide on resume');
      });
    },
  },

  // ===========================================================================
  // F12 — Completion Water-Ripple Ritual & Somatic Check-In (一息成) (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-26',
    tier: 1,
    feature: 'F12',
    name: 'Completing final pose opens 一息成 water-ripple stage with skin verse & 3 somatic feeling chips',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);

        for (const tag of [
          TAGS.completion_screen,
          TAGS.completion_ripple_canvas,
          TAGS.completion_feeling_light,
          TAGS.completion_feeling_warm,
          TAGS.completion_feeling_calm,
          TAGS.completion_finish_button,
        ]) {
          assert.equal(await isVisible(page, tag), true, `Expected ${tag} visible on completion_screen`);
        }
      });
    },
  },
  {
    id: 'P-T1-27',
    tier: 1,
    feature: 'F12',
    name: 'Selecting somatic feeling and clicking completion_finish_button persists record to 松息手札',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);

        await clickTestId(page, TAGS.completion_feeling_warm);
        await clickTestId(page, TAGS.completion_finish_button);

        await clickTestId(page, TAGS.nav_tab_journal);
        const journalText = await readTestIdText(page, TAGS.journal_screen);
        assert.ok(journalText.includes('温热舒展') || journalText.includes('舒展'), 'Journal must record somatic feeling');
      });
    },
  },

  // ===========================================================================
  // F13 — Low-Intrusion Sedentary Reminder Scheduler (微风提醒) (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-28',
    tier: 1,
    feature: 'F13',
    name: '微风设置 exposes enable switch, 45/60/90/120 intervals & quiet switches with localStorage persistence',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);

        for (const tag of [
          TAGS.reminder_enable_switch,
          TAGS.reminder_interval_45,
          TAGS.reminder_interval_60,
          TAGS.reminder_interval_90,
          TAGS.reminder_interval_120,
          TAGS.reminder_lunch_quiet_switch,
          TAGS.reminder_night_quiet_switch,
          TAGS.reminder_weekend_quiet_switch,
          TAGS.reminder_mute_today_switch,
        ]) {
          await byTestId(page, tag).scrollIntoViewIfNeeded().catch(() => {});
          assert.equal(await isVisible(page, tag), true, `Expected ${tag} visible in settings_screen`);
        }

        await clickTestId(page, TAGS.reminder_interval_90);
        const beforeWeekend = await isToggleOrChipActive(page, TAGS.reminder_weekend_quiet_switch);
        await clickTestId(page, TAGS.reminder_weekend_quiet_switch);
        const afterWeekend = await isToggleOrChipActive(page, TAGS.reminder_weekend_quiet_switch);
        assert.notEqual(beforeWeekend, afterWeekend);

        await page.reload({ waitUntil: 'load' });
        await clickTestId(page, TAGS.nav_tab_settings);
        assert.equal(await isToggleOrChipActive(page, TAGS.reminder_weekend_quiet_switch), afterWeekend);
      });
    },
  },
  {
    id: 'P-T1-29',
    tier: 1,
    feature: 'F13',
    name: 'reminder_preview_trigger_button displays gentle reminder_inapp_banner launching 90s seated relief',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, TAGS.reminder_preview_trigger_button);

        assert.equal(await isVisible(page, TAGS.reminder_inapp_banner), true, 'Preview must show reminder_inapp_banner');
        await clickTestId(page, TAGS.reminder_inapp_banner);
        assert.equal(await isVisible(page, TAGS.player_screen), true, 'Banner quick-start must enter player_screen');
      });
    },
  },

  // ===========================================================================
  // F14 — Streak-Free Water-Ripple Journal & Shareable Card (2 tests)
  // ===========================================================================
  {
    id: 'P-T1-30',
    tier: 1,
    feature: 'F14',
    name: '松息手札 renders Xuan-paper monthly grid, calm summary & concentric session ripples',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_finish_button);

        await clickTestId(page, TAGS.nav_tab_journal);
        assert.equal(await isVisible(page, TAGS.journal_calendar_grid), true);
        assert.equal(await isVisible(page, TAGS.journal_monthly_summary), true);

        const summary = await readTestIdText(page, TAGS.journal_monthly_summary);
        assert.ok(summary.includes('1'), `Monthly summary must reflect 1 completed session, got '${summary}'`);
        assert.equal(FORBIDDEN_STREAK_SHAME_REGEX.test(summary), false);
      });
    },
  },
  {
    id: 'P-T1-31',
    tier: 1,
    feature: 'F14',
    name: 'journal_share_card_button opens 松息日笺 poetic share card preview dialog',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_journal);
        await clickTestId(page, TAGS.journal_share_card_button);

        assert.equal(
          await isVisible(page, TAGS.journal_share_preview_dialog),
          true,
          'Share button must open journal_share_preview_dialog'
        );
        const dialogCopy = await readTestIdText(page, TAGS.journal_share_preview_dialog);
        assert.ok(dialogCopy.includes('松息'), `Share card preview must display 松息 brand seal, got '${dialogCopy}'`);
      });
    },
  },

  // ===========================================================================
  // F15 — Standalone Android APK Packaging, Visual Captures & Suite (3 tests)
  // ===========================================================================
  {
    id: 'P-T1-32',
    tier: 1,
    feature: 'F15',
    name: 'Canonical /home/antigravity/docs/songxi_interactive_prototype.html exists, loads with 0 errors & uses Android chrome',
    fn: async ({ session }) => {
      assert.equal(fs.existsSync(PATHS.prototypeHtml), true, `Missing ${PATHS.prototypeHtml}`);
      const raw = fs.readFileSync(PATHS.prototypeHtml, 'utf8');
      assert.ok(raw.length > 15_000, `Expected non-trivial v2 prototype HTML (>15KB), got ${raw.length}B`);
      assert.match(raw, /<!doctype html>/i);
      assert.equal(/dynamic-island|灵动岛/i.test(raw), false, 'Android prototype must not use iOS Dynamic Island');

      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const hasShell = await page.locator('.songxi-app-shell').count();
        assert.ok(hasShell >= 1, 'Prototype must expose .songxi-app-shell container');
      });
    },
  },
  {
    id: 'P-T1-33',
    tier: 1,
    feature: 'F15',
    name: 'Canonical /home/antigravity/docs/songxi_skin_gallery_showcase.html exists & loads over HTTP with 0 errors',
    fn: async ({ session }) => {
      assert.equal(fs.existsSync(PATHS.showcaseHtml), true, `Missing ${PATHS.showcaseHtml}`);
      const raw = fs.readFileSync(PATHS.showcaseHtml, 'utf8');
      assert.ok(raw.length > 8_000, `Expected non-trivial showcase HTML (>8KB), got ${raw.length}B`);
      assert.match(raw, /<!doctype html>/i);

      await session.withPage({ viewport: { width: 1280, height: 840 } }, async ({ page, showcaseUrl }) => {
        await page.goto(showcaseUrl, { waitUntil: 'load' });
        const titleText = await page.locator('body').innerText();
        assert.ok(titleText.includes('松息'), 'Showcase body must render 松息 brand');
      });
    },
  },
  {
    id: 'P-T1-34',
    tier: 1,
    feature: 'F15',
    name: 'Canonical /home/antigravity/docs/ design specifications exist (>2KB) and match 1:1',
    fn: async () => {
      const stats = auditDesignDocsSync();
      assert.ok(stats.designBytes > 2000 && stats.mirrorBytes > 2000 && stats.skinSpecBytes > 2000);
    },
  },
  {
    id: 'P-T1-35',
    tier: 1,
    feature: 'F8',
    name: 'Clinical 3-stage prescription, asymmetry 6:4, sagittal/biomechanics overlay, 式法精要 sheet & Body Relief Atlas work end-to-end',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);

        await clickTestId(page, TAGS.bodymap_asymmetry_right);
        const preview = await readTestIdText(page, TAGS.bodymap_prescription_preview);
        assert.ok(preview.includes('三阶处方序'), `Expected 3-stage prescription preview, got '${preview}'`);

        await clickTestId(page, TAGS.bodymap_compose_start_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        assert.equal(await isVisible(page, TAGS.player_timeline_stage_badge), true);

        // neck_chin_tuck is warmup #1 and uses sagittal side-profile view + biomechanical guide
        const planeAttr = await byTestId(page, TAGS.player_figure_canvas).getAttribute('data-view-plane');
        assert.equal(planeAttr, 'sagittal', 'First warmup pose neck_chin_tuck must render in sagittal side-profile plane');

        await clickTestId(page, TAGS.player_biomechanics_toggle);
        assert.equal(await byTestId(page, TAGS.player_figure_canvas).getAttribute('data-biomechanics'), 'false');
        await clickTestId(page, TAGS.player_biomechanics_toggle);
        assert.equal(await byTestId(page, TAGS.player_figure_canvas).getAttribute('data-biomechanics'), 'true');

        await clickTestId(page, TAGS.player_clinical_guide_button);
        assert.equal(await isVisible(page, TAGS.player_clinical_guide_sheet), true);
        const guideText = await readTestIdText(page, TAGS.player_clinical_guide_sheet);
        assert.ok(guideText.includes('靶向肌群') && guideText.includes('RPE'), `Expected clinical details, got '${guideText}'`);
        await clickTestId(page, TAGS.player_clinical_guide_close);
        assert.equal(await isVisible(page, TAGS.player_clinical_guide_sheet), false);

        for (let step = 0; step < 8; step++) {
          if (await isVisible(page, TAGS.completion_screen)) break;
          await clickTestId(page, TAGS.player_next_pose_button);
        }
        assert.equal(await isVisible(page, TAGS.completion_screen), true);
        await clickTestId(page, TAGS.completion_feeling_light);
        await clickTestId(page, TAGS.completion_finish_button);

        await clickTestId(page, TAGS.nav_tab_journal);
        assert.equal(await isVisible(page, TAGS.journal_body_relief_atlas), true);
        const atlasText = await readTestIdText(page, TAGS.journal_body_relief_atlas);
        assert.ok(atlasText.includes('月度身体释压图谱') && atlasText.includes('重点温养'), `Unexpected atlas text: '${atlasText}'`);
        assert.equal(FORBIDDEN_STREAK_SHAME_REGEX.test(atlasText), false);
      });
    },
  },
  {
    id: 'P-T1-36',
    tier: 1,
    feature: 'F9',
    name: 'Saved custom presets (我的常备方) &《四时雅集》chapter directory sheet launch targeted routines',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);

        await clickTestId(page, TAGS.bodymap_duration_2);
        await clickTestId(page, 'bodymap_save_preset_button');
        assert.equal(await isVisible(page, 'bodymap_saved_presets_row'), true);
        assert.equal(await isVisible(page, 'bodymap_preset_chip_0'), true);

        await clickTestId(page, TAGS.nav_tab_home);
        assert.equal(await isVisible(page, 'home_saved_presets_section'), true);
        await clickTestId(page, 'home_saved_preset_0');
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        await clickTestId(page, TAGS.player_close_button);

        await clickTestId(page, TAGS.nav_tab_bodymap);
        await clickTestId(page, 'booklet_chapters_button_0');
        assert.equal(await isVisible(page, 'booklet_detail_sheet'), true);
        await clickTestId(page, 'booklet_day_item_2');
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        const pTxt = await readTestIdText(page, TAGS.player_screen);
        assert.ok(pTxt.includes('三息'), `Expected Day 3 chapter routine in player, got '${pTxt}'`);
      });
    },
  },
  {
    id: 'P-T1-37',
    tier: 1,
    feature: 'F11',
    name: 'Offline voice coaching cue toggle & 3s settle-in prep buffer banner operate cleanly in player',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);

        await clickTestId(page, 'settings_voice_guide_switch');
        await clickTestId(page, 'settings_prep_buffer_switch');
        assert.equal(await isToggleOrChipActive(page, 'settings_voice_guide_switch'), true);
        assert.equal(await isToggleOrChipActive(page, 'settings_prep_buffer_switch'), true);

        await clickTestId(page, TAGS.nav_tab_home);
        await clickTestId(page, TAGS.home_quick_routine_0);
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        assert.equal(await isVisible(page, 'player_prep_buffer_banner'), true);
        assert.equal(await isToggleOrChipActive(page, 'player_voice_cue_toggle'), true);

        const spoken = await page.evaluate(() => window.__SONGXI_BRIDGE__.getSpokenVoiceCues());
        assert.ok(Array.isArray(spoken) && spoken.length >= 1, 'Voice engine must speak setup cue when enabled');

        await clickTestId(page, 'player_skip_prep_button');
        assert.equal(await isVisible(page, 'player_prep_buffer_banner'), false);
        await clickTestId(page, 'player_voice_cue_toggle');
        assert.equal(await isToggleOrChipActive(page, 'player_voice_cue_toggle'), false);
      });
    },
  },
  {
    id: 'P-T1-38',
    tier: 1,
    feature: 'F14',
    name: 'Cross-month journal navigation, clickable day filter, 720x1040 PNG poster canvas & JSON backup/restore work end-to-end',
    fn: async ({ session }) => {
      await session.withPage(async ({ page, prototypeUrl }) => {
        const nov18Ms = localEpochMs(2026, 11, 18, 10, 0);
        await openPrototypeReadyAtHome(page, `${prototypeUrl}?SIMULATED_EPOCH_MS=${nov18Ms}`);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_feeling_calm);
        await clickTestId(page, TAGS.completion_finish_button);

        await clickTestId(page, TAGS.nav_tab_journal);
        const initNotesTitle = await readTestIdText(page, 'journal_notes_section_title');
        assert.ok(initNotesTitle.includes('本月中旬'), `Expected middle-dekad title on Nov 18, got '${initNotesTitle}'`);

        await clickTestId(page, 'journal_day_cell_18');
        const dayTitle = await readTestIdText(page, 'journal_notes_section_title');
        assert.ok(dayTitle.includes('11月18日'), `Expected 11月18日 filter title, got '${dayTitle}'`);
        await clickTestId(page, 'journal_clear_day_filter');

        await clickTestId(page, 'journal_prev_month_button');
        assert.equal(await isVisible(page, 'journal_current_month_button'), true);
        await clickTestId(page, 'journal_current_month_button');
        assert.equal(await isVisible(page, 'journal_current_month_button'), false);

        await clickTestId(page, TAGS.journal_share_card_button);
        assert.equal(await isVisible(page, 'journal_share_poster_preview'), true);
        const posterDims = await page.evaluate(() => {
          const c = document.querySelector('[data-testid="journal_share_poster_preview"]');
          return { w: c.width, h: c.height, urlPrefix: c.toDataURL('image/png').slice(0, 22) };
        });
        assert.equal(posterDims.w, 720);
        assert.equal(posterDims.h, 1040);
        assert.equal(posterDims.urlPrefix, 'data:image/png;base64,');

        await clickTestId(page, 'journal_save_poster_button');
        assert.equal(await isVisible(page, 'journal_poster_save_feedback'), true);

        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, 'settings_export_backup_button');
        assert.equal(await isVisible(page, 'settings_backup_dialog'), true);
        await clickTestId(page, 'settings_backup_confirm_restore_button');
        const statusTxt = await readTestIdText(page, 'settings_backup_status_text');
        assert.ok(statusTxt.includes('恢复'), `Expected backup restore status, got '${statusTxt}'`);
      });
    },
  },
];

module.exports = tests;
