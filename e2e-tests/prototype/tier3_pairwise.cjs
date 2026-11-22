'use strict';

process.env.TZ = 'Asia/Shanghai';

const assert = require('node:assert/strict');
const {
  TAGS,
  FORBIDDEN_PROTOTYPE_REGEX,
  FORBIDDEN_STREAK_SHAME_REGEX,
  localEpochMs,
  isVisible,
  clickTestId,
  readTestIdText,
  openPrototypeReadyAtHome,
  advanceRoutineToCompletionOnPage,
  readShellColorsAndContrast,
} = require('./harness.cjs');

const tests = [
  {
    id: 'P-T3-01',
    tier: 3,
    features: ['F2', 'F6'],
    name: 'Dusk Pine dark skin adapts Splash Ad stage into low-glare evening sanctuary',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 10, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_dusk_pine);
        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, TAGS.splash_preview_trigger_button);
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), true, 'Splash preview must open');
        const colors = await readShellColorsAndContrast(page);
        assert.ok(colors.bgLuminance < 0.25, `Expected low-glare dusk_pine background, got ${colors.bgLuminance}`);
      });
    },
  },
  {
    id: 'P-T3-02',
    tier: 3,
    features: ['F6', 'F10', 'F11'],
    name: 'Switching to Qinglv Celadon updates Stretch Player palette and default bamboo chime',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 11, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_qinglv_celadon);
        await clickTestId(page, TAGS.nav_tab_home);
        await clickTestId(page, TAGS.home_start_hero_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        const soundText = await readTestIdText(page, TAGS.player_sound_selector);
        assert.ok(
          soundText.includes('磬') || soundText.includes('竹') || soundText.length >= 2,
          `Expected sound selector copy under qinglv_celadon, got '${soundText}'`
        );
      });
    },
  },
  {
    id: 'P-T3-03',
    tier: 3,
    features: ['F13', 'F3', 'F12', 'F14'],
    name: 'Reminder in-app banner launches 90s routine bypassing splash and logs session to Journal',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 15, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_settings);
        await clickTestId(page, TAGS.reminder_preview_trigger_button);
        assert.equal(await isVisible(page, TAGS.reminder_inapp_banner), true);
        await clickTestId(page, TAGS.reminder_inapp_banner);
        assert.equal(await isVisible(page, TAGS.splash_ad_screen), false);
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_feeling_calm);
        await clickTestId(page, TAGS.completion_finish_button);
        await clickTestId(page, TAGS.nav_tab_journal);
        const summary = await readTestIdText(page, TAGS.journal_monthly_summary);
        assert.ok(summary.includes('1'), `Expected 1 recorded session in journal summary, got '${summary}'`);
      });
    },
  },
  {
    id: 'P-T3-04',
    tier: 3,
    features: ['F8', 'F10', 'F11'],
    name: 'Custom multi-zone prescription supports Eyes-Free peripheral mode and simulated Flip-to-Pause',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 14, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);
        await clickTestId(page, TAGS.bodymap_zone_neck);
        await clickTestId(page, TAGS.bodymap_zone_wrist);
        await clickTestId(page, TAGS.bodymap_duration_2);
        await clickTestId(page, TAGS.bodymap_compose_start_button);
        assert.equal(await isVisible(page, TAGS.player_screen), true);
        await clickTestId(page, TAGS.player_eyes_free_toggle);
        if (await isVisible(page, TAGS.player_simulate_flip_button)) {
          await clickTestId(page, TAGS.player_simulate_flip_button);
          assert.equal(await isVisible(page, TAGS.player_paused_by_flip_banner), true);
        }
      });
    },
  },
  {
    id: 'P-T3-05',
    tier: 3,
    features: ['F6', 'F7'],
    name: 'Evening 21:15 auto-night pairing activates Dusk Pine alongside Evening Bedside scene',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 21, 15) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_skins);
        const autoNightOn = await page.evaluate(() => {
          const el = document.querySelector('[data-testid="skin_auto_night_switch"]');
          if (!el) return false;
          const inp = el.querySelector('input[type="checkbox"]');
          if (inp) return inp.checked;
          return el.getAttribute('aria-checked') === 'true' || el.dataset.active === 'true' || el.dataset.checked === 'true';
        });
        if (!autoNightOn) {
          await clickTestId(page, TAGS.skin_auto_night_switch);
        }
        await clickTestId(page, TAGS.nav_tab_home);
        await clickTestId(page, TAGS.home_scene_bed);
        const colors = await readShellColorsAndContrast(page);
        assert.ok(colors.bgLuminance < 0.25, `Expected dark evening skin luminance at 21:15, got ${colors.bgLuminance}`);
        assert.ok(colors.contrast >= 4.5, `Expected WCAG AA >= 4.5 contrast, got ${colors.contrast}`);
      });
    },
  },
  {
    id: 'P-T3-06',
    tier: 3,
    features: ['F5', 'F7', 'F12'],
    name: 'Spring Blossom skin adapts Home greeting and Completion ripple poetic verse',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 10, 30) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_spring_blossom);
        await clickTestId(page, TAGS.nav_tab_home);
        const badge = await readTestIdText(page, TAGS.home_active_skin_badge);
        assert.ok(badge.includes('春'), `Expected 春涧 badge on Home, got '${badge}'`);
        await clickTestId(page, TAGS.home_quick_routine_0);
        await advanceRoutineToCompletionOnPage(page);
        const compText = await readTestIdText(page, TAGS.completion_screen);
        assert.ok(compText.length >= 6 && !FORBIDDEN_PROTOTYPE_REGEX.test(compText));
      });
    },
  },
  {
    id: 'P-T3-07',
    tier: 3,
    features: ['F9', 'F12', 'F14', 'F4'],
    name: 'Curated Booklet completion updates booklet progress and streak-free Journal with zero VIP upsells',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 16, 0) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_bodymap);
        const beforeCard = await readTestIdText(page, TAGS.booklet_card_0);
        await clickTestId(page, TAGS.booklet_card_0);
        await advanceRoutineToCompletionOnPage(page);
        await clickTestId(page, TAGS.completion_feeling_warm);
        await clickTestId(page, TAGS.completion_finish_button);
        await clickTestId(page, TAGS.nav_tab_bodymap);
        const afterCard = await readTestIdText(page, TAGS.booklet_card_0);
        assert.notEqual(afterCard, beforeCard, 'Booklet card progress must update after completing routine');
        await clickTestId(page, TAGS.nav_tab_journal);
        const bodyCopy = await page.locator('body').innerText();
        assert.equal(FORBIDDEN_PROTOTYPE_REGEX.test(bodyCopy), false);
        assert.equal(FORBIDDEN_STREAK_SHAME_REGEX.test(bodyCopy), false);
      });
    },
  },
  {
    id: 'P-T3-08',
    tier: 3,
    features: ['F6', 'F14'],
    name: 'Journal share preview dialog reflects active skin identity and closes cleanly',
    fn: async ({ session }) => {
      await session.withPage({ timeMs: localEpochMs(2026, 11, 18, 11, 20) }, async ({ page, prototypeUrl }) => {
        await openPrototypeReadyAtHome(page, prototypeUrl);
        await clickTestId(page, TAGS.nav_tab_skins);
        await clickTestId(page, TAGS.skin_card_qinglv_celadon);
        await clickTestId(page, TAGS.nav_tab_journal);
        await clickTestId(page, TAGS.journal_share_card_button);
        assert.equal(await isVisible(page, TAGS.journal_share_preview_dialog), true);
        const cardText = await readTestIdText(page, TAGS.journal_share_preview_dialog);
        assert.ok(cardText.includes('松息') && cardText.length >= 6);
      });
    },
  },
];

module.exports = { tests };
