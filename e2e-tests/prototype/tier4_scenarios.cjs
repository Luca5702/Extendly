'use strict';

process.env.TZ = 'Asia/Shanghai';

const assert = require('node:assert/strict');
const {
  PATHS,
  TAGS,
  SKIN_KEYS,
  FORBIDDEN_PROTOTYPE_REGEX,
  FORBIDDEN_STREAK_SHAME_REGEX,
  localEpochMs,
  isVisible,
  clickTestId,
  readTestIdText,
  openPrototypeReadyAtHome,
  advanceRoutineToCompletionOnPage,
  readShellColorsAndContrast,
  auditShellCssScoping,
  auditHtmlCsp,
  auditDesignDocsSync,
} = require('./harness.cjs');

const tests = [
  {
    id: 'P-T4-01',
    tier: 4,
    features: ['F1', 'F6', 'F8', 'F10', 'F11', 'F12', 'F14'],
    name: 'First-time desk worker journey: Onboarding -> Qinglv Skin -> 2m Custom Stretch -> Flip Pause -> Journal',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 9, 30) }, async ({ page, prototypeUrl }) => {
        await page.goto(prototypeUrl, { waitUntil: 'load' });
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false);
        if (await isVisible(page, TAGS.onboarding_enter_button)) {
          await clickTestId(page, TAGS.onboarding_enter_button);
        }
        assert.equal(await isVisible(page, TAGS.home_screen), true);

        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_qinglv_celadon);
        await clickTestId(page, TAGS.nav_tab_bodymap);
        await clickTestId(page, TAGS.bodymap_zone_neck);
        await clickTestId(page, TAGS.bodymap_duration_2);
        await clickTestId(page, TAGS.bodymap_compose_start_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true);

        if (await isVisible(page, TAGS.player_simulate_flip_button)) {
          await clickTestId(page, TAGS.player_simulate_flip_button);
          assert.equal(await isVisible(page, TAGS.player_paused_by_flip_banner), true);
          await clickTestId(page, TAGS.player_simulate_flip_button);
        }

        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_feeling_light);
        await clickTestId(page, TAGS.completion_finish_button);

        await clickTestId(page, TAGS.nav_tab_journal);
        const summary = await readTestIdText(page, TAGS.journal_monthly_summary);
        assert.ok(summary.includes('1'), `Expected 1 completed session in summary, got '${summary}'`);
      });
    },
  },
  {
    id: 'P-T4-02',
    tier: 4,
    features: ['F2', 'F3', 'F10'],
    name: 'Splash Ad compliance journey: manual preview -> safe CTA sheet -> dismiss -> 3s countdown to Sanctuary',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 14, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, TAGS.splash_preview_trigger_button);
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), true);
        const badgeText = await readTestIdText(page, TAGS.splash_ad_badge);
        assert.ok(badgeText.includes('广告'), `Expected 广告 disclosure, got '${badgeText}'`);

        await clickTestId(page, TAGS.splash_ad_cta_button);
        assert.equal(await isVisible(page, TAGS.splash_ad_confirm_sheet), true);
        await clickTestId(page, TAGS.splash_ad_confirm_dismiss);
        assert.equal(await isVisible(page, TAGS.splash_ad_confirm_sheet), false);

        await clickTestId(page, TAGS.splash_ad_skip_button);
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false);
      });
    },
  },
  {
    id: 'P-T4-03',
    tier: 4,
    features: ['F13', 'F3', 'F7'],
    name: 'Sedentary reminder governance journey: configure quiet hours & interval -> trigger preview -> quick-start',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 11, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, TAGS.reminder_interval_90);
        await clickTestId(page, TAGS.reminder_preview_trigger_button);
        assert.equal(await isVisible(page, TAGS.reminder_inapp_banner), true);
        await clickTestId(page, TAGS.reminder_inapp_banner);
        assert.equal(await isVisible(page, TAGS.player_screen), true);
      });
    },
  },
  {
    id: 'P-T4-04',
    tier: 4,
    features: ['F5', 'F6', 'F9', 'F14'],
    name: 'Four-skin visual & contrast tour across Prototype and 4-Skin Style Reference Showcase',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 10, 0) }, async ({ page, prototypeUrl, showcaseUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const bgSet = new Set();
        for (const skinKey of SKIN_KEYS) {
          await clickTestId(page, TAGS.nav_tab_skins);
          await clickTestId(page, `skin_card_${skinKey}`);
          await clickTestId(page, TAGS.nav_tab_home);
          const colors = await readShellColorsAndContrast(page);
          assert.ok(colors.contrast >= 4.5, `Skin ${skinKey} contrast ${colors.contrast} < 4.5`);
          bgSet.add(colors.bgCss);
        }
        assert.equal(bgSet.size, 4, 'All 4 skins must render distinct background colors');

        await page.goto(showcaseUrl, { waitUntil: 'load' });
        const showcaseCopy = await page.locator('body').innerText();
        for (const label of ['水墨远山', '青绿山居', '暮山听松', '春涧花影']) {
          assert.ok(showcaseCopy.includes(label), `Showcase must display '${label}'`);
        }
      });
    },
  },
  {
    id: 'P-T4-05',
    tier: 4,
    features: ['F14', 'F12'],
    name: 'Multi-session accumulation & poetic share card workflow with zero streak-shame copy',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 16, 30) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        for (const shortcutTag of [TAGS.home_quick_routine_0, TAGS.home_quick_routine_1]) {
          await clickTestId(page, TAGS.nav_tab_home);
          await clickTestId(page, shortcutTag);
          await advanceRoutineToCompletionOnPage(page);
          await clickTestId(page, TAGS.completion_finish_button);
        }
        await clickTestId(page, TAGS.nav_tab_journal);
        const summary = await readTestIdText(page, TAGS.journal_monthly_summary);
        assert.ok(summary.includes('2'), `Expected 2 accumulated sessions in journal, got '${summary}'`);
        const fullText = await page.locator('body').innerText();
        assert.equal(FORBIDDEN_STREAK_SHAME_REGEX.test(fullText), false);
      });
    },
  },
  {
    id: 'P-T4-06',
    tier: 4,
    features: ['F4', 'F5', 'F15'],
    name: 'Cross-artifact synchronization, CSP hygiene, CSS scoping & zero-VIP audit across Docs & HTML Suite',
    fn: async ({ session }) => {
      const syncStats = auditDesignDocsSync();
      assert.ok(syncStats.designBytes > 2000 && syncStats.skinSpecBytes > 2000);

      const protoCsp = auditHtmlCsp(PATHS.prototypeHtml);
      const showCsp = auditHtmlCsp(PATHS.showcaseHtml);
      assert.deepEqual(protoCsp.disallowed, [], `Disallowed external URLs in prototype: ${protoCsp.disallowed}`);
      assert.deepEqual(showCsp.disallowed, [], `Disallowed external URLs in showcase: ${showCsp.disallowed}`);

      await session.withPage({ viewport: { width: 390, height: 844 } }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        const scoping = await auditShellCssScoping(page);
        assert.equal(scoping.hasShellElement, true, 'Prototype must render .songxi-app-shell');
        assert.deepEqual(scoping.rootCustomProps, [], 'Prototype must not pollute host :root CSS custom properties');
        const allText = await page.locator('body').innerText();
        assert.equal(FORBIDDEN_PROTOTYPE_REGEX.test(allText), false);
      });
    },
  },
];

module.exports = { tests };
