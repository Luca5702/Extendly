'use strict';

// MUST precede any Date construction so localEpochMs(...) aligns 100% with timezoneId: 'Asia/Shanghai'
process.env.TZ = 'Asia/Shanghai';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const https = require('node:https');
const { chromium } = require('playwright-core');

const DOCS_DIR = process.env.SONGXI_DOCS_DIR || path.resolve(__dirname, '../../docs');
const PATHS = Object.freeze({
  docsDir: DOCS_DIR,
  prototypeHtml: path.join(DOCS_DIR, 'songxi_interactive_prototype.html'),
  showcaseHtml: path.join(DOCS_DIR, 'songxi_skin_gallery_showcase.html'),
  designMd: path.join(DOCS_DIR, 'songxi_product_design.md'),
  docsDesignMd: path.join(DOCS_DIR, 'SONGXI_PRODUCT_DESIGN.md'),
  docsSkinSplashMd: path.join(DOCS_DIR, 'SKIN_AND_SPLASH_SPEC.md'),
});

const ALLOWED_TAILWIND_CDN = 'https://www.gstatic.com/antigravity/web/dev/tailwindcss.min.js';
const CHROMIUM_EXECUTABLE = (() => {
  const candidates = [
    process.env.CHROMIUM_BIN,
    '/usr/lib/chromium/chromium',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser',
    '/usr/bin/google-chrome',
    '/usr/bin/google-chrome-stable',
  ].filter(Boolean);
  return candidates.find((p) => fs.existsSync(p)) || '/usr/bin/google-chrome';
})();

const SKIN_KEYS = Object.freeze(['ink_mist', 'qinglv_celadon', 'dusk_pine', 'spring_blossom']);
const SKIN_CN_LABELS = Object.freeze({
  ink_mist: '水墨',
  qinglv_celadon: '青绿',
  dusk_pine: '暮山',
  spring_blossom: '春涧',
});

// Canonical 69 data-testid attributes (PROJECT.md §4 & §5)
const TAGS = Object.freeze({
  onboarding_card: 'onboarding_card',
  onboarding_enter_button: 'onboarding_enter_button',
  bottom_nav_bar: 'bottom_nav_bar',
  nav_tab_home: 'nav_tab_home',
  nav_tab_bodymap: 'nav_tab_bodymap',
  nav_tab_skins: 'nav_tab_skins',
  nav_tab_journal: 'nav_tab_journal',
  nav_tab_settings: 'nav_tab_settings',

  splash_ad_screen: 'splash_ad_screen',
  splash_ad_backdrop: 'splash_ad_backdrop',
  splash_ad_badge: 'splash_ad_badge',
  splash_ad_skip_button: 'splash_ad_skip_button',
  splash_ad_cta_button: 'splash_ad_cta_button',
  splash_ad_confirm_sheet: 'splash_ad_confirm_sheet',
  splash_ad_confirm_dismiss: 'splash_ad_confirm_dismiss',
  splash_ad_brand_footer: 'splash_ad_brand_footer',

  home_screen: 'home_screen',
  home_shanshui_backdrop: 'home_shanshui_backdrop',
  home_active_skin_badge: 'home_active_skin_badge',
  home_scene_desk: 'home_scene_desk',
  home_scene_tea: 'home_scene_tea',
  home_scene_bed: 'home_scene_bed',
  home_hero_card: 'home_hero_card',
  home_start_hero_button: 'home_start_hero_button',
  home_quick_routine_0: 'home_quick_routine_0',
  home_quick_routine_1: 'home_quick_routine_1',
  home_quick_routine_2: 'home_quick_routine_2',
  home_quick_routine_3: 'home_quick_routine_3',

  bodymap_screen: 'bodymap_screen',
  bodymap_canvas: 'bodymap_canvas',
  bodymap_zone_neck: 'bodymap_zone_neck',
  bodymap_zone_shoulder: 'bodymap_zone_shoulder',
  bodymap_zone_thoracic: 'bodymap_zone_thoracic',
  bodymap_zone_lumbar: 'bodymap_zone_lumbar',
  bodymap_zone_wrist: 'bodymap_zone_wrist',
  bodymap_zone_eye: 'bodymap_zone_eye',
  bodymap_duration_2: 'bodymap_duration_2',
  bodymap_duration_4: 'bodymap_duration_4',
  bodymap_duration_6: 'bodymap_duration_6',
  bodymap_compose_start_button: 'bodymap_compose_start_button',
  booklet_card_0: 'booklet_card_0',
  booklet_card_1: 'booklet_card_1',
  booklet_card_2: 'booklet_card_2',
  booklet_card_3: 'booklet_card_3',

  player_screen: 'player_screen',
  player_figure_canvas: 'player_figure_canvas',
  player_breathing_halo: 'player_breathing_halo',
  player_breath_phase_label: 'player_breath_phase_label',
  player_eyes_free_toggle: 'player_eyes_free_toggle',
  player_sound_selector: 'player_sound_selector',
  player_sound_silent: 'player_sound_silent',
  player_pause_resume_button: 'player_pause_resume_button',
  player_prev_pose_button: 'player_prev_pose_button',
  player_next_pose_button: 'player_next_pose_button',
  player_close_button: 'player_close_button',
  player_paused_by_flip_banner: 'player_paused_by_flip_banner',
  player_simulate_flip_button: 'player_simulate_flip_button',

  completion_screen: 'completion_screen',
  completion_ripple_canvas: 'completion_ripple_canvas',
  completion_feeling_light: 'completion_feeling_light',
  completion_feeling_warm: 'completion_feeling_warm',
  completion_feeling_calm: 'completion_feeling_calm',
  completion_finish_button: 'completion_finish_button',

  skins_screen: 'skins_screen',
  skin_card_ink_mist: 'skin_card_ink_mist',
  skin_card_qinglv_celadon: 'skin_card_qinglv_celadon',
  skin_card_dusk_pine: 'skin_card_dusk_pine',
  skin_card_spring_blossom: 'skin_card_spring_blossom',
  skin_chime_preview_button: 'skin_chime_preview_button',
  skin_auto_night_switch: 'skin_auto_night_switch',

  journal_screen: 'journal_screen',
  journal_calendar_grid: 'journal_calendar_grid',
  journal_monthly_summary: 'journal_monthly_summary',
  journal_share_card_button: 'journal_share_card_button',
  journal_share_preview_dialog: 'journal_share_preview_dialog',

  settings_screen: 'settings_screen',
  reminder_enable_switch: 'reminder_enable_switch',
  reminder_interval_45: 'reminder_interval_45',
  reminder_interval_60: 'reminder_interval_60',
  reminder_interval_90: 'reminder_interval_90',
  reminder_interval_120: 'reminder_interval_120',
  reminder_lunch_quiet_switch: 'reminder_lunch_quiet_switch',
  reminder_night_quiet_switch: 'reminder_night_quiet_switch',
  reminder_weekend_quiet_switch: 'reminder_weekend_quiet_switch',
  reminder_mute_today_switch: 'reminder_mute_today_switch',
  reminder_preview_trigger_button: 'reminder_preview_trigger_button',
  reminder_inapp_banner: 'reminder_inapp_banner',
  splash_personalized_switch: 'splash_personalized_switch',
  splash_preview_trigger_button: 'splash_preview_trigger_button',
});

// Negation-safe forbidden legacy v1 patterns (never bans "不做会员功能" or "无任何内购广告")
const FORBIDDEN_PROTOTYPE_REGEX =
  /四时席|¥16|¥88|¥98|¥118|¥168|openMembershipModal|viewport-mp|零开屏广告|微信小程序|UnionID|折笺赠友|开通会员|升级会员|会员专享|付费解锁|连续包月/;
const FORBIDDEN_DOC_REGEX =
  /四时席|¥16|¥88|¥98|¥118|¥168|openMembershipModal|viewport-mp|零开屏广告|UnionID|Freemium\s*订阅|连续包月|四时年席|付费订阅转化率|松息\s*·\s*轻帖/;
const FORBIDDEN_STREAK_SHAME_REGEX = /连续打卡\s*\d+|已中断|断签|打卡失败|击败了?\s*\d+%/;

// Track ephemeral dirs for guaranteed cleanup on both stop() AND process exit
const ACTIVE_EPHEMERAL_DIRS = new Set();

function cleanupEphemeralDirsSync() {
  for (const dirPath of Array.from(ACTIVE_EPHEMERAL_DIRS)) {
    try {
      fs.rmSync(dirPath, { recursive: true, force: true });
    } catch (_) {
      // ignore cleanup race
    }
    ACTIVE_EPHEMERAL_DIRS.delete(dirPath);
  }
}

process.once('exit', cleanupEphemeralDirsSync);
for (const sig of ['SIGINT', 'SIGTERM']) {
  process.once(sig, () => {
    cleanupEphemeralDirsSync();
    process.exit(130);
  });
}

function resolveScratchTmp() {
  const envTmp = process.env.TMPDIR || process.env.TMP || process.env.TEMP;
  if (envTmp && !envTmp.startsWith('/tmp')) return envTmp;
  return '/home/antigravity/.gemini/jetski/scratch/teamwork_tmp/e2e_runner/tmp';
}

function localEpochMs(year, month1Based, day, hour = 10, minute = 0, second = 0) {
  return new Date(year, month1Based - 1, day, hour, minute, second, 0).getTime();
}

function fetchHttpsBuffer(url) {
  const doGet = (rejectUnauthorized) =>
    new Promise((resolve, reject) => {
      const req = https.get(url, { timeout: 8000, rejectUnauthorized }, (res) => {
        if (res.statusCode !== 200) {
          res.resume();
          return reject(new Error(`HTTPS GET ${url} returned HTTP ${res.statusCode}`));
        }
        const chunks = [];
        res.on('data', (c) => chunks.push(c));
        res.on('end', () => resolve(Buffer.concat(chunks)));
      });
      req.on('error', reject);
      req.on('timeout', () => req.destroy(new Error(`Timeout fetching ${url}`)));
    });
  return doGet(true).catch(() => doGet(false));
}

const MIME_BY_EXT = Object.freeze({
  '.html': 'text/html; charset=utf-8',
  '.md': 'text/markdown; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
});

class PrototypeHarnessSession {
  constructor() {
    this.server = null;
    this.baseUrl = null;
    this.browser = null;
    this.tailwindBuffer = null;
    this.scratchRunDir = null;
    this.shmSockDir = null;
  }

  async start({ smokeOnly = false } = {}) {
    const scratchTmp = resolveScratchTmp();
    const suffix = `${process.pid}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
    this.scratchRunDir = path.join(scratchTmp, `pw_run_${suffix}`);
    const crashDir = path.join(this.scratchRunDir, 'crash');
    const cacheDir = path.join(this.scratchRunDir, 'cache');
    const configDir = path.join(this.scratchRunDir, 'config');
    // POSIX 0700 socket directory in /dev/shm prevents Chromium ProcessSingleton SIGTRAP on virtiofs (0777/nobody)
    this.shmSockDir = `/dev/shm/songxi_chr_sock_${suffix}`;

    fs.mkdirSync(crashDir, { recursive: true });
    fs.mkdirSync(cacheDir, { recursive: true });
    fs.mkdirSync(configDir, { recursive: true });
    fs.mkdirSync(this.shmSockDir, { recursive: true, mode: 0o700 });
    ACTIVE_EPHEMERAL_DIRS.add(this.scratchRunDir);
    ACTIVE_EPHEMERAL_DIRS.add(this.shmSockDir);

    if (!smokeOnly) {
      this.tailwindBuffer = await fetchHttpsBuffer(ALLOWED_TAILWIND_CDN).catch(() => null);
    }

    this.server = http.createServer((req, res) => {
      const parsed = new URL(req.url, 'http://127.0.0.1');
      if (parsed.pathname === '/__smoke') {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
        res.end('<!doctype html><html lang="zh-CN"><body><div data-testid="smoke_ok">松息</div></body></html>');
        return;
      }
      if (parsed.pathname === '/favicon.ico') {
        res.writeHead(204);
        res.end();
        return;
      }
      const rel = decodeURIComponent(
        parsed.pathname === '/' ? 'songxi_interactive_prototype.html' : parsed.pathname.replace(/^\/+/, '')
      );
      const candidate = path.resolve(DOCS_DIR, rel);
      if (
        !candidate.startsWith(DOCS_DIR + path.sep) ||
        !fs.existsSync(candidate) ||
        !fs.statSync(candidate).isFile()
      ) {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end(`Not found in ${DOCS_DIR}: ${parsed.pathname}`);
        return;
      }
      const mime = MIME_BY_EXT[path.extname(candidate).toLowerCase()] || 'application/octet-stream';
      res.writeHead(200, { 'Content-Type': mime, 'Cache-Control': 'no-store' });
      res.end(fs.readFileSync(candidate));
    });

    await new Promise((resolve, reject) => {
      this.server.once('error', reject);
      this.server.listen(0, '127.0.0.1', resolve);
    });
    this.baseUrl = `http://127.0.0.1:${this.server.address().port}`;

    this.browser = await chromium.launch({
      executablePath: CHROMIUM_EXECUTABLE,
      headless: true,
      env: {
        ...process.env,
        TMPDIR: this.shmSockDir,
        TMP: this.shmSockDir,
        TEMP: this.shmSockDir,
        XDG_CONFIG_HOME: configDir,
        XDG_CACHE_HOME: cacheDir,
      },
      args: [
        '--no-sandbox',
        '--disable-gpu',
        '--ignore-certificate-errors',
        '--autoplay-policy=no-user-gesture-required',
        '--disable-crash-reporter',
        `--crash-dumps-dir=${crashDir}`,
        `--disk-cache-dir=${cacheDir}`,
      ],
    });
  }

  async withPage(options, testBody) {
    if (typeof options === 'function') {
      testBody = options;
      options = {};
    }
    const {
      viewport = { width: 412, height: 915 },
      timeMs = localEpochMs(2026, 11, 18, 10, 0),
      installClock = true,
      storageSeed = null,
      initScripts = [],
    } = options;

    const context = await this.browser.newContext({
      viewport,
      locale: 'zh-CN',
      timezoneId: 'Asia/Shanghai',
      ignoreHTTPSErrors: true,
    });

    const tracker = {
      pageErrors: [],
      consoleErrors: [],
      requestFailures: [],
      badHttpStatuses: [],
      externalUrls: [],
      assertClean() {
        assert.deepEqual(this.pageErrors, [], `Uncaught pageerror(s): ${this.pageErrors.join(' | ')}`);
        assert.deepEqual(this.consoleErrors, [], `console.error(s): ${this.consoleErrors.join(' | ')}`);
        assert.deepEqual(this.requestFailures, [], `Failed requests: ${this.requestFailures.join(' | ')}`);
        assert.deepEqual(this.badHttpStatuses, [], `HTTP >=400 responses: ${this.badHttpStatuses.join(' | ')}`);
      },
    };

    await context.route('**/*', async (route) => {
      const url = route.request().url();
      if (!url.startsWith(this.baseUrl) && !url.startsWith('data:') && !url.startsWith('blob:') && !url.startsWith('about:')) {
        tracker.externalUrls.push(url);
        if (url === ALLOWED_TAILWIND_CDN && this.tailwindBuffer) {
          await route.fulfill({
            status: 200,
            contentType: 'application/javascript; charset=utf-8',
            body: this.tailwindBuffer,
          });
          return;
        }
      }
      await route.continue();
    });

    if (storageSeed) {
      await context.addInitScript((seed) => {
        if (!sessionStorage.getItem('__e2e_seeded')) {
          for (const [k, v] of Object.entries(seed)) {
            localStorage.setItem(k, v);
          }
          sessionStorage.setItem('__e2e_seeded', '1');
        }
      }, storageSeed);
    }

    for (const scriptFn of initScripts) {
      await context.addInitScript(scriptFn);
    }

    const page = await context.newPage();
    page.on('pageerror', (err) => tracker.pageErrors.push(err.stack || err.message || String(err)));
    page.on('console', (msg) => {
      if (msg.type() === 'error') tracker.consoleErrors.push(msg.text());
    });
    page.on('requestfailed', (req) => {
      const errText = req.failure() ? req.failure().errorText : 'unknown';
      if (!String(errText).includes('ERR_ABORTED')) {
        tracker.requestFailures.push(`${req.method()} ${req.url()} -> ${errText}`);
      }
    });
    page.on('response', (res) => {
      if (res.status() >= 400) tracker.badHttpStatuses.push(`${res.status()} ${res.url()}`);
    });

    if (installClock && timeMs != null) {
      await page.clock.install({ time: timeMs });
    }

    try {
      const result = await testBody({
        page,
        context,
        tracker,
        baseUrl: this.baseUrl,
        prototypeUrl: `${this.baseUrl}/songxi_interactive_prototype.html`,
        showcaseUrl: `${this.baseUrl}/songxi_skin_gallery_showcase.html`,
      });
      tracker.assertClean();
      return result;
    } finally {
      await context.close().catch(() => {});
    }
  }

  async stop() {
    if (this.browser) {
      await this.browser.close().catch(() => {});
      this.browser = null;
    }
    if (this.server) {
      if (typeof this.server.closeAllConnections === 'function') {
        this.server.closeAllConnections();
      }
      await new Promise((resolve) => this.server.close(() => resolve()));
      this.server = null;
    }
    if (this.shmSockDir) {
      try {
        fs.rmSync(this.shmSockDir, { recursive: true, force: true });
      } catch (_) {}
      ACTIVE_EPHEMERAL_DIRS.delete(this.shmSockDir);
      this.shmSockDir = null;
    }
    if (this.scratchRunDir) {
      try {
        fs.rmSync(this.scratchRunDir, { recursive: true, force: true });
      } catch (_) {}
      ACTIVE_EPHEMERAL_DIRS.delete(this.scratchRunDir);
      this.scratchRunDir = null;
    }
  }
}

// ==============================================================================
// Opaque-Box UI, Timing, Contrast, Scoping & Static Audit Helpers
// ==============================================================================

function byTestId(page, tag) {
  return page.locator(`[data-testid="${tag}"]`).first();
}

async function isVisible(page, tag) {
  const loc = page.locator(`[data-testid="${tag}"]`);
  const count = await loc.count();
  if (count === 0) return false;
  for (let i = 0; i < count; i++) {
    const item = loc.nth(i);
    if (await item.isVisible().catch(() => false)) {
      const box = await item.boundingBox().catch(() => null);
      if (box && box.width > 0.5 && box.height > 0.5) return true;
    }
  }
  return false;
}

async function clickTestId(page, tag) {
  const loc = byTestId(page, tag);
  await loc.scrollIntoViewIfNeeded().catch(() => {});
  await loc.click();
}

async function readTestIdText(page, tag) {
  const loc = byTestId(page, tag);
  return ((await loc.innerText().catch(() => '')) || (await loc.textContent().catch(() => '')) || '').trim();
}

async function isToggleOrChipActive(page, tag) {
  const loc = byTestId(page, tag);
  return loc.evaluate((el) => {
    if (el instanceof HTMLInputElement && (el.type === 'checkbox' || el.type === 'radio')) {
      return el.checked;
    }
    const innerInput = el.querySelector('input[type="checkbox"], input[type="radio"]');
    if (innerInput && 'checked' in innerInput) return Boolean(innerInput.checked);
    const ariaChecked = el.getAttribute('aria-checked');
    if (ariaChecked === 'true') return true;
    if (ariaChecked === 'false') return false;
    const ariaPressed = el.getAttribute('aria-pressed');
    if (ariaPressed === 'true') return true;
    if (ariaPressed === 'false') return false;
    const ariaSelected = el.getAttribute('aria-selected');
    if (ariaSelected === 'true') return true;
    if (ariaSelected === 'false') return false;
    const ds = el.dataset || {};
    if (ds.active === 'true' || ds.selected === 'true' || ds.checked === 'true' || ds.state === 'on') return true;
    if (ds.active === 'false' || ds.selected === 'false' || ds.checked === 'false' || ds.state === 'off') return false;
    const cls = String(el.className || '');
    return /\b(active|selected|is-active|is-selected|checked|on)\b/i.test(cls);
  });
}

async function openPrototypeReadyAtHome(page, prototypeUrl) {
  await page.goto(prototypeUrl, { waitUntil: 'load' });
  if (await isVisible(page, TAGS.splash_ad_skip_button)) {
    await clickTestId(page, TAGS.splash_ad_skip_button);
  }
  if (await isVisible(page, TAGS.onboarding_enter_button)) {
    await clickTestId(page, TAGS.onboarding_enter_button);
  }
  assert.equal(await isVisible(page, TAGS.home_screen), true, 'Expected home_screen to be visible after entry');
}

async function advanceRoutineToCompletionOnPage(page, maxSteps = 16) {
  for (let i = 0; i < maxSteps; i++) {
    if (await isVisible(page, TAGS.completion_screen)) return;
    if (await isVisible(page, TAGS.player_next_pose_button)) {
      await clickTestId(page, TAGS.player_next_pose_button);
    } else {
      await page.clock.runFor(50_000);
    }
  }
  if (!(await isVisible(page, TAGS.completion_screen))) {
    await page.clock.runFor(65_000);
  }
  assert.equal(await isVisible(page, TAGS.completion_screen), true, 'Expected completion_screen after finishing poses');
}

function parseRgbString(str) {
  const m = str && String(str).match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)/);
  if (!m) return null;
  return { r: Number(m[1]), g: Number(m[2]), b: Number(m[3]), a: m[4] === undefined ? 1 : Number(m[4]) };
}

function relativeLuminance({ r, g, b }) {
  const ch = (v) => {
    const s = v / 255;
    return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b);
}

function wcagContrastRatio(c1, c2) {
  const l1 = relativeLuminance(c1);
  const l2 = relativeLuminance(c2);
  return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
}

async function readShellColorsAndContrast(page) {
  const raw = await page.evaluate(() => {
    const shell =
      document.querySelector('.songxi-app-shell') ||
      document.querySelector('[data-testid="home_screen"]') ||
      document.body;
    const findOpaqueBg = (startEl) => {
      const queue = [startEl, ...Array.from(startEl.children || [])];
      for (const node of queue) {
        if (!node) continue;
        const bg = getComputedStyle(node).backgroundColor;
        if (bg && bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') return bg;
      }
      let cur = startEl;
      while (cur) {
        const bg = getComputedStyle(cur).backgroundColor;
        if (bg && bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') return bg;
        cur = cur.parentElement;
      }
      return getComputedStyle(document.body).backgroundColor;
    };
    const heading =
      shell.querySelector('h1, h2, h3, [data-testid="home_hero_card"] h2, [data-testid="home_hero_card"]') || shell;
    return {
      hasShellClass: Boolean(document.querySelector('.songxi-app-shell')),
      bgCss: findOpaqueBg(shell),
      fgCss: getComputedStyle(heading).color,
    };
  });

  const bg = parseRgbString(raw.bgCss);
  const fg = parseRgbString(raw.fgCss);
  assert.ok(bg, `Failed to parse shell background RGB from '${raw.bgCss}'`);
  assert.ok(fg, `Failed to parse shell foreground RGB from '${raw.fgCss}'`);

  return {
    hasShellClass: raw.hasShellClass,
    bgCss: raw.bgCss,
    fgCss: raw.fgCss,
    bg,
    fg,
    bgLuminance: relativeLuminance(bg),
    contrast: wcagContrastRatio(fg, bg),
  };
}

async function auditShellCssScoping(page) {
  return page.evaluate(() => {
    const shell = document.querySelector('.songxi-app-shell');
    const rootCustomProps = [];
    const shellCustomProps = [];

    for (const sheet of Array.from(document.styleSheets)) {
      let rules;
      try {
        rules = Array.from(sheet.cssRules || []);
      } catch (_) {
        continue;
      }
      for (const rule of rules) {
        if (!rule.selectorText || !rule.style) continue;
        const sel = rule.selectorText.trim();
        const customProps = [];
        for (let i = 0; i < rule.style.length; i++) {
          const prop = rule.style[i];
          if (prop.startsWith('--') && !prop.startsWith('--tw-')) {
            customProps.push(prop);
          }
        }
        if (customProps.length === 0) continue;
        if (/(^|,)\s*(:root|html)\s*(,|$)/i.test(sel) && !sel.includes('.songxi-app-shell')) {
          rootCustomProps.push(...customProps);
        }
        if (sel.includes('.songxi-app-shell')) {
          shellCustomProps.push(...customProps);
        }
      }
    }

    // Also inspect inline style custom properties on .songxi-app-shell
    if (shell && shell.style) {
      for (let i = 0; i < shell.style.length; i++) {
        const p = shell.style[i];
        if (p.startsWith('--') && !p.startsWith('--tw-')) shellCustomProps.push(p);
      }
    }

    return {
      hasShellElement: Boolean(shell),
      rootCustomProps: Array.from(new Set(rootCustomProps)),
      shellCustomProps: Array.from(new Set(shellCustomProps)),
    };
  });
}

function auditHtmlCsp(filePath) {
  assert.equal(fs.existsSync(filePath), true, `HTML file missing: ${filePath}`);
  const html = fs.readFileSync(filePath, 'utf8');
  const externalUrls = [];

  const tagAttrPatterns = [
    /<script\b[^>]*\bsrc=["']([^"']+)["']/gi,
    /<link\b[^>]*\bhref=["']([^"']+)["']/gi,
    /<img\b[^>]*\bsrc=["']([^"']+)["']/gi,
    /<audio\b[^>]*\bsrc=["']([^"']+)["']/gi,
    /<video\b[^>]*\bsrc=["']([^"']+)["']/gi,
    /<iframe\b[^>]*\bsrc=["']([^"']+)["']/gi,
    /url\(\s*["']?(https?:\/\/[^)"'\s]+)["']?\s*\)/gi,
  ];

  for (const re of tagAttrPatterns) {
    let match;
    while ((match = re.exec(html)) !== null) {
      const url = match[1].trim();
      if (/^https?:\/\//i.test(url) || url.startsWith('//')) {
        externalUrls.push(url);
      }
    }
  }

  const disallowed = externalUrls.filter((u) => u !== ALLOWED_TAILWIND_CDN);
  return {
    filePath,
    byteLength: Buffer.byteLength(html, 'utf8'),
    externalUrls,
    disallowed,
  };
}

function auditDesignDocsSync() {
  const requiredDocs = [PATHS.designMd, PATHS.docsDesignMd, PATHS.docsSkinSplashMd];
  for (const p of requiredDocs) {
    assert.equal(fs.existsSync(p), true, `Missing required v2 Markdown specification: ${p}`);
    const stat = fs.statSync(p);
    assert.ok(stat.size > 2000, `Expected ${p} to be > 2000 bytes, got ${stat.size}`);
  }

  const designText = fs.readFileSync(PATHS.designMd, 'utf8');
  const mirrorText = fs.readFileSync(PATHS.docsDesignMd, 'utf8');
  const skinSpecText = fs.readFileSync(PATHS.docsSkinSplashMd, 'utf8');

  const norm = (s) => s.replace(/\r\n/g, '\n').trim();
  const extractH2Headings = (s) =>
    norm(s)
      .split('\n')
      .filter((line) => /^#{1,2}\s+/.test(line))
      .map((line) => line.trim());

  const designHeadings = extractH2Headings(designText);
  const mirrorHeadings = extractH2Headings(mirrorText);
  assert.ok(designHeadings.length >= 4, `Expected >= 4 top-level headings in ${PATHS.designMd}`);
  assert.deepEqual(
    mirrorHeadings,
    designHeadings,
    'SONGXI_PRODUCT_DESIGN.md and songxi_product_design.md must stay 1:1 synchronized'
  );

  for (const [label, content] of [
    ['songxi_product_design.md', designText],
    ['SONGXI_PRODUCT_DESIGN.md', mirrorText],
    ['SKIN_AND_SPLASH_SPEC.md', skinSpecText],
  ]) {
    for (const skinToken of ['水墨', '青绿', '暮山', '春涧']) {
      assert.ok(content.includes(skinToken), `${label} must document skin '${skinToken}'`);
    }
    assert.ok(content.includes('开屏'), `${label} must document splash-ad-only monetization ('开屏')`);
    const badMatch = content.match(FORBIDDEN_DOC_REGEX);
    assert.equal(badMatch, null, `${label} contains forbidden legacy v1 token: ${badMatch && badMatch[0]}`);
  }

  return {
    designBytes: Buffer.byteLength(designText, 'utf8'),
    mirrorBytes: Buffer.byteLength(mirrorText, 'utf8'),
    skinSpecBytes: Buffer.byteLength(skinSpecText, 'utf8'),
  };
}

module.exports = {
  PATHS,
  TAGS,
  SKIN_KEYS,
  SKIN_CN_LABELS,
  ALLOWED_TAILWIND_CDN,
  FORBIDDEN_PROTOTYPE_REGEX,
  FORBIDDEN_DOC_REGEX,
  FORBIDDEN_STREAK_SHAME_REGEX,
  PrototypeHarnessSession,
  localEpochMs,
  byTestId,
  isVisible,
  clickTestId,
  readTestIdText,
  isToggleOrChipActive,
  openPrototypeReadyAtHome,
  advanceRoutineToCompletionOnPage,
  parseRgbString,
  relativeLuminance,
  wcagContrastRatio,
  readShellColorsAndContrast,
  auditShellCssScoping,
  auditHtmlCsp,
  auditDesignDocsSync,
};
