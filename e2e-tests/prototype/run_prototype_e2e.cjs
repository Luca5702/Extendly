#!/usr/bin/env node
'use strict';

process.env.TZ = 'Asia/Shanghai';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { PrototypeHarnessSession, byTestId } = require('./harness.cjs');

function parseArgs(argv) {
  const opts = {
    mode: 'run',
    tier: 'all',
    phase: 'full',
    listJsonPath: null,
    jsonReportPath: null,
  };

  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--tier') {
      opts.tier = argv[++i];
    } else if (arg.startsWith('--tier=')) {
      opts.tier = arg.slice('--tier='.length);
    } else if (arg === '--verify-harness') {
      opts.mode = 'verify';
    } else if (arg === '--verify-m1') {
      opts.mode = 'verify';
      opts.phase = 'm1';
    } else if (arg === '--m1') {
      opts.phase = 'm1';
    } else if (arg === '--phase') {
      opts.phase = argv[++i];
    } else if (arg.startsWith('--phase=')) {
      opts.phase = arg.slice('--phase='.length);
    } else if (arg === '--list-json') {
      opts.listJsonPath = argv[++i];
    } else if (arg.startsWith('--list-json=')) {
      opts.listJsonPath = arg.slice('--list-json='.length);
    } else if (arg === '--json-report') {
      opts.jsonReportPath = argv[++i];
    } else if (arg.startsWith('--json-report=')) {
      opts.jsonReportPath = arg.slice('--json-report='.length);
    } else {
      throw new Error(`Unknown CLI argument: ${arg}`);
    }
  }
  return opts;
}

function loadSuiteModule(relName, required) {
  const fullPath = path.join(__dirname, relName);
  if (!fs.existsSync(fullPath)) {
    if (required) {
      return { missing: true, path: fullPath, tests: [] };
    }
    return { missing: false, path: fullPath, tests: [] };
  }
  const exported = require(fullPath);
  const tests = Array.isArray(exported) ? exported : exported.tests || [];
  return { missing: false, path: fullPath, tests };
}

function inspectDescriptor(t) {
  const fnSrc = typeof t.fn === 'function' ? t.fn.toString() : '';
  const bodyLength = fnSrc.trim().length;
  const hasAssertion =
    /\bassert\b|openPrototypeReadyAtHome|advanceRoutineToCompletionOnPage|auditDesignDocsSync|auditHtmlCsp|readShellColorsAndContrast/.test(
      fnSrc
    ) && !/\bassert\s*\(\s*true\s*\)|\bassert\.ok\s*\(\s*true\s*\)/.test(fnSrc);
  return {
    id: t.id,
    tier: t.tier,
    feature: t.feature || (Array.isArray(t.features) ? t.features[0] : null),
    features: Array.isArray(t.features) ? t.features : t.feature ? [t.feature] : [],
    name: t.name,
    bodyLength,
    hasAssertion,
  };
}

async function runVerifyHarness(opts) {
  const errors = [];
  const requireAllFour = opts.phase === 'full';

  const m1 = loadSuiteModule('tier1_feature.cjs', true);
  const m2 = loadSuiteModule('tier2_boundary.cjs', true);
  const m3 = loadSuiteModule('tier3_pairwise.cjs', requireAllFour);
  const m4 = loadSuiteModule('tier4_scenarios.cjs', requireAllFour);

  for (const mod of [m1, m2, m3, m4]) {
    if (mod.missing) {
      errors.push(`Missing required Prototype suite module: ${mod.path}`);
    }
  }

  const allTests = [...m1.tests, ...m2.tests, ...m3.tests, ...m4.tests];
  const descriptors = allTests.map(inspectDescriptor);

  if (opts.listJsonPath) {
    fs.mkdirSync(path.dirname(opts.listJsonPath), { recursive: true });
    fs.writeFileSync(opts.listJsonPath, JSON.stringify(descriptors, null, 2), 'utf8');
  }

  const seenIds = new Set();
  for (const d of descriptors) {
    if (!d.id || seenIds.has(d.id)) {
      errors.push(`Duplicate or missing Prototype test id: ${d.id}`);
    }
    seenIds.add(d.id);
    if (d.bodyLength < 60 || !d.hasAssertion) {
      errors.push(`Prototype test ${d.id} failed non-vacuity body/assertion audit (len=${d.bodyLength})`);
    }
  }

  const t1Count = descriptors.filter((d) => d.tier === 1).length;
  const t2Count = descriptors.filter((d) => d.tier === 2).length;
  const t3Count = descriptors.filter((d) => d.tier === 3).length;
  const t4Count = descriptors.filter((d) => d.tier === 4).length;

  if (t1Count < 34) errors.push(`Expected >= 34 Prototype Tier 1 tests, found ${t1Count}`);
  if (t2Count < 34) errors.push(`Expected >= 34 Prototype Tier 2 tests, found ${t2Count}`);

  for (let fi = 1; fi <= 15; fi++) {
    const fk = `F${fi}`;
    if (!descriptors.some((d) => d.tier === 1 && d.feature === fk)) {
      errors.push(`Prototype Tier 1 missing coverage for ${fk}`);
    }
    if (!descriptors.some((d) => d.tier === 2 && d.feature === fk)) {
      errors.push(`Prototype Tier 2 missing coverage for ${fk}`);
    }
  }

  if (requireAllFour) {
    if (t3Count < 8) errors.push(`Expected >= 8 Prototype Tier 3 tests in full gate, found ${t3Count}`);
    if (t4Count < 6) errors.push(`Expected >= 6 Prototype Tier 4 tests in full gate, found ${t4Count}`);
  }

  // Real Headless Chromium + 127.0.0.1:0 HTTP smoke launch
  const session = new PrototypeHarnessSession();
  try {
    await session.start({ smokeOnly: true });
    await session.withPage({ installClock: false }, async ({ page, baseUrl }) => {
      await page.goto(`${baseUrl}/__smoke`, { waitUntil: 'load' });
      const text = await byTestId(page, 'smoke_ok').innerText();
      assert.equal(text.trim(), '松息', 'Headless Chromium smoke DOM text mismatch');
    });
    console.log(
      `[OK] Playwright + Headless Chromium smoke launch verified on ${session.baseUrl || '127.0.0.1:0'} ` +
        `(T1=${t1Count}, T2=${t2Count}, T3=${t3Count}, T4=${t4Count}).`
    );
  } catch (err) {
    errors.push(`Headless Chromium smoke launch failed: ${err.stack || err.message || String(err)}`);
  } finally {
    await session.stop();
  }

  if (errors.length > 0) {
    for (const e of errors) {
      console.error(`[FAIL] ${e}`);
    }
    process.exit(1);
  }
}

async function runTestsForTier(opts) {
  const tierMap = {
    '1': ['tier1_feature.cjs'],
    '2': ['tier2_boundary.cjs'],
    '3': ['tier3_pairwise.cjs'],
    '4': ['tier4_scenarios.cjs'],
    all: ['tier1_feature.cjs', 'tier2_boundary.cjs', 'tier3_pairwise.cjs', 'tier4_scenarios.cjs'],
  };
  const moduleNames = tierMap[String(opts.tier)];
  if (!moduleNames) {
    throw new Error(`Invalid --tier '${opts.tier}' (expected 1|2|3|4|all)`);
  }

  const selectedTests = [];
  for (const modName of moduleNames) {
    const mod = loadSuiteModule(modName, true);
    if (mod.missing) {
      throw new Error(`Missing required Prototype test module: ${mod.path}`);
    }
    selectedTests.push(...mod.tests);
  }

  const startedAt = Date.now();
  const session = new PrototypeHarnessSession();
  const perFeatureCounts = {};
  const failures = [];
  let passed = 0;

  try {
    await session.start({ smokeOnly: false });
    for (const testCase of selectedTests) {
      const fk = testCase.feature || 'MULTI';
      perFeatureCounts[fk] = (perFeatureCounts[fk] || 0) + 1;
      try {
        await testCase.fn({ session });
        passed++;
        console.log(`  [PASS] ${testCase.id}: ${testCase.name}`);
      } catch (err) {
        const msg = err && (err.message || String(err));
        failures.push({
          id: testCase.id,
          feature: fk,
          name: testCase.name,
          error: String(msg).split('\n')[0],
        });
        console.error(`  [FAIL] ${testCase.id}: ${testCase.name}\n         ${msg}`);
      }
    }
  } finally {
    await session.stop();
  }

  const report = {
    tier: opts.tier,
    total: selectedTests.length,
    passed,
    failed: failures.length,
    skipped: 0,
    durationMs: Date.now() - startedAt,
    perFeatureCounts,
    failures,
  };

  if (opts.jsonReportPath) {
    fs.mkdirSync(path.dirname(opts.jsonReportPath), { recursive: true });
    fs.writeFileSync(opts.jsonReportPath, JSON.stringify(report, null, 2), 'utf8');
  }

  if (report.total === 0 || report.failed > 0) {
    process.exit(1);
  }
}

(async () => {
  try {
    const opts = parseArgs(process.argv.slice(2));
    if (opts.mode === 'verify') {
      await runVerifyHarness(opts);
    } else {
      await runTestsForTier(opts);
    }
  } catch (err) {
    console.error(`[FATAL] ${err.stack || err.message || String(err)}`);
    process.exit(1);
  }
})();
