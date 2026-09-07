// Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { fixture, harness, send } from './harness.mjs';

test('stores only allowlisted, redacted diagnostics; retries do not overwrite or extend retention', async t => {
  const h = await harness(); t.after(h.close);
  const r = fixture();
  r.crash.message = 'token=topsecret email=test@example.org https://example.org/private /storage/emulated/0/private.txt';
  const first = await send(h.mf, r);
  assert.equal(first.status, 201); assert.equal(first.headers.get('cache-control'), 'no-store');
  const stored = await h.db.prepare('SELECT * FROM crash_reports').first();
  assert.equal(stored.expires_at - stored.received_at, 30 * 86400);
  assert.equal(stored.server_redaction_version, 1);
  assert(!/topsecret|test@example|example.org|private.txt|192\.0\.2/.test(JSON.stringify(stored)));
  assert.equal(JSON.parse(stored.payload).crash.frames[0].line, 42);
  const reordered = Object.fromEntries(Object.entries(r).reverse());
  assert.equal((await send(h.mf, reordered)).status, 200);
  r.crash.message = 'different crash';
  assert.equal((await send(h.mf, r)).status, 409);
  assert.deepEqual(await h.db.prepare('SELECT * FROM crash_reports').first(), stored);
  const usage = await h.db.prepare('SELECT * FROM daily_usage').first();
  assert.equal(usage.reports, 1); assert.equal(usage.bytes, stored.payload_bytes);
});

test('accepts factory nullable fields, detection context, cycles placeholder and oversized fallback', async t => {
  const h = await harness(); t.after(h.close);
  const r = fixture(); delete r.crash.message; delete r.crash.frames[0].file;
  r.crash.detection = { operation: 'TEXT', screenWidth: 1080, screenHeight: 2400,
    areaLeft: 0, areaTop: 0, areaWidth: 500, areaHeight: 300, threshold: 80, textLength: 12, modelId: null };
  r.crash.cause = { truncated: true }; r.truncated = true;
  assert.equal((await send(h.mf, r)).status, 201);
  const fallback = fixture(); fallback.truncated = true; fallback.crash = { type: 'java.lang.Error', message: null };
  assert.equal((await send(h.mf, fallback)).status, 201);
});

test('accepts minimal native exits and rejects tombstone-like additions', async t => {
  const h = await harness(); t.after(h.close);
  const r = fixture();
  r.mainThread = false;
  r.crash = { type: 'android.native_crash', frames: [], suppressed: [],
    nativeExit: { occurredAtMs: 1_700_000_000_000, status: 11, importance: 100, pssKb: 1234, rssKb: 5678 } };
  assert.equal((await send(h.mf, r)).status, 201);
  for (const change of [
    x => { x.crash.message = 'tombstone'; },
    x => { x.crash.nativeExit.trace = 'native backtrace'; },
    x => { x.crash.frames.push({ class: 'native', method: 'x', line: 1, native: true }); },
    x => { x.mainThread = true; },
  ]) {
    const invalid = structuredClone(r); invalid.reportId = crypto.randomUUID(); change(invalid);
    assert.equal((await send(h.mf, invalid)).status, 400);
  }
});

test('rejects unknown fields, malformed graphs, unsupported versions and dangerous shapes without writes', async t => {
  const h = await harness({ sourceLimit: 1000, ingestLimit: 1000 }); t.after(h.close);
  const mutations = [
    r => { r.scenarioName = 'private'; }, r => { r.device.installationId = 'private'; },
    r => { r.crash.detection = { text: 'private' }; }, r => { r.schemaVersion = 2; },
    r => { r.redactionVersion = 2; }, r => { r.reportId = '../../x'; },
    r => { r.recentOperations[0].event = 'SCENARIO_NAME'; }, r => { r.crash.frames[0].line = 1.2; },
    r => { r.crash.frames = Array(257).fill(r.crash.frames[0]); },
    r => { r.crash.message = 'x'.repeat(4097); }, r => { r.recentOperations = Array(51).fill(r.recentOperations[0]); },
    r => { r.crash.cause = { arbitrary: true }; }, r => { r.crash.suppressed = Array(5).fill({ truncated: true }); },
    r => { let e = r.crash; for (let i = 0; i < 12; i++) { e.cause = structuredClone(r.crash); e = e.cause; } },
    r => { r.build.versionCode = Number.MAX_SAFE_INTEGER + 1; },
    r => { r.crash = { truncated: true }; }, r => { delete r.crash.frames; },
    r => { Object.defineProperty(r, '__proto__', { value: { polluted: true }, enumerable: true }); },
  ];
  for (const change of mutations) {
    const r = fixture(); change(r);
    const response = await send(h.mf, r);
    assert.equal(response.status, 400, change.toString());
    assert.deepEqual(await response.json(), { status: 'invalid_report' });
  }
  assert.equal((await h.db.prepare('SELECT count(*) AS n FROM crash_reports').first()).n, 0);
});

test('HTTP boundary handles malformed JSON/UTF8, compression, size limits and private read routes', async t => {
  const h = await harness(); t.after(h.close);
  for (const body of ['{', 'null', '[]', new Uint8Array([0xff, 0xfe])]) assert.equal((await send(h.mf, undefined, { body })).status, 400);
  assert.equal((await send(h.mf, undefined, { body: ' '.repeat(262145) })).status, 413);
  // Chunked/no Content-Length: streaming enforcement is independent of the header.
  const stream = new ReadableStream({ start(c) { c.enqueue(new Uint8Array(131072)); c.enqueue(new Uint8Array(131073)); c.close(); } });
  assert.equal((await send(h.mf, undefined, { body: stream, duplex: 'half' })).status, 413);
  assert.equal((await send(h.mf, undefined, { headers: { 'content-type': 'text/plain' } })).status, 415);
  assert.equal((await send(h.mf, undefined, { headers: { 'content-type': 'application/json', 'content-encoding': 'gzip' } })).status, 415);
  for (const method of ['GET', 'DELETE', 'OPTIONS']) {
    const response = await h.mf.dispatchFetch('http://localhost/v1/reports', { method });
    assert.equal(response.status, 405); assert.equal(response.headers.get('access-control-allow-origin'), null);
  }
  for (const path of ['/reports', '/v1/reports/id', '/v1/reports?secret=x']) assert.equal((await h.mf.dispatchFetch(`http://localhost${path}`)).status, 404);
});

test('concurrent duplicates result in exactly one record and one quota charge', async t => {
  const h = await harness({ sourceLimit: 1000, ingestLimit: 1000 }); t.after(h.close);
  const r = fixture(); const responses = await Promise.all(Array.from({ length: 50 }, () => send(h.mf, r)));
  assert.equal(responses.filter(r => r.status === 201).length, 1);
  assert.equal(responses.filter(r => r.status === 200).length, 49);
  assert.equal((await h.db.prepare('SELECT reports FROM daily_usage').first()).reports, 1);
});

test('hard daily report cap survives concurrency; already accepted retries still succeed', async t => {
  const h = await harness({ sourceLimit: 1000, ingestLimit: 1000 }); t.after(h.close);
  const day = Math.floor(Date.now() / 86400000);
  await h.db.prepare('INSERT INTO daily_usage VALUES (?, 998, 0)').bind(day).run();
  const reports = Array.from({ length: 40 }, fixture);
  const results = await Promise.all(reports.map(r => send(h.mf, r)));
  assert.equal(results.filter(r => r.status === 201).length, 2);
  assert.equal(results.filter(r => r.status === 503).length, 38);
  assert.equal((await h.db.prepare('SELECT reports FROM daily_usage').first()).reports, 1000);
  assert.equal((await send(h.mf, reports[results.findIndex(r => r.status === 201)])).status, 200);
});

test('daily byte cap refuses storage without charging rejected requests', async t => {
  const h = await harness(); t.after(h.close);
  await h.db.prepare('INSERT INTO daily_usage VALUES (?, 0, ?)').bind(Math.floor(Date.now() / 86400000), 33554431).run();
  assert.equal((await send(h.mf)).status, 503);
  assert.equal((await h.db.prepare('SELECT reports FROM daily_usage').first()).reports, 0);
});

test('source and aggregate rate limiting operate without persistent IP metadata', async t => {
  const h = await harness({ sourceLimit: 2, ingestLimit: 4 }); t.after(h.close);
  assert.equal((await send(h.mf)).status, 201);
  assert.equal((await send(h.mf)).status, 201);
  const blocked = await send(h.mf); assert.equal(blocked.status, 429); assert.equal(blocked.headers.get('retry-after'), '60');
  assert.equal((await send(h.mf, undefined, { headers: { 'content-type': 'application/json', 'cf-connecting-ip': '192.0.2.2' } })).status, 201);
  assert.equal((await send(h.mf, undefined, { headers: { 'content-type': 'application/json', 'cf-connecting-ip': '192.0.2.3' } })).status, 429);
  assert(!JSON.stringify(await h.db.prepare('SELECT * FROM crash_reports').all()).includes('192.0.2'));
});

test('missing secret and unavailable DB fail closed with generic errors', async t => {
  const missing = await harness({ secret: '' }); t.after(missing.close);
  assert.equal((await send(missing.mf)).status, 503);
  const broken = await harness({ migrate: false }); t.after(broken.close);
  const response = await send(broken.mf);
  assert.equal(response.status, 503); assert.deepEqual(await response.json(), { status: 'temporarily_unavailable' });
});

test('scheduled cleanup removes expired reports and accounting but retains current data', async t => {
  const h = await harness(); t.after(h.close);
  const old = fixture(), current = fixture();
  assert.equal((await send(h.mf, old)).status, 201); assert.equal((await send(h.mf, current)).status, 201);
  const now = Math.floor(Date.now() / 1000);
  await h.db.prepare('UPDATE crash_reports SET received_at = ?, expires_at = ? WHERE report_id = ?').bind(now - 3000000, now - 1, old.reportId).run();
  await h.db.prepare('INSERT INTO daily_usage VALUES (?, 1, 100)').bind(Math.floor(now / 86400) - 3).run();
  const worker = await h.mf.getWorker();
  await worker.scheduled({ scheduledTime: Date.now(), cron: '17 * * * *' });
  const rows = await h.db.prepare('SELECT report_id FROM crash_reports').all();
  assert.deepEqual(rows.results, [{ report_id: current.reportId }]);
  assert.equal((await h.db.prepare('SELECT count(*) AS n FROM daily_usage').first()).n, 1);
});

test('slow unfinished uploads time out without storing a partial report', { timeout: 20000 }, async t => {
  const h = await harness(); t.after(h.close);
  const stream = new ReadableStream({ start(c) { c.enqueue(new TextEncoder().encode('{')); } });
  const response = await send(h.mf, undefined, { body: stream, duplex: 'half' });
  assert.equal(response.status, 408);
  assert.equal((await h.db.prepare('SELECT count(*) AS n FROM crash_reports').first()).n, 0);
});

test('default configured source limit stays at 30 after high-throughput test overrides', async t => {
  const high = await harness({ sourceLimit: 10000, ingestLimit: 10000 });
  await high.close();
  const h = await harness(); t.after(h.close);
  for (let i = 0; i < 30; i++) assert.equal((await send(h.mf)).status, 201);
  assert.equal((await send(h.mf)).status, 429);
});

test('SQL-like exception details remain data and never execute', async t => {
  const h = await harness(); t.after(h.close);
  const r = fixture(); r.crash.type = "Error'); DROP TABLE crash_reports; --";
  r.crash.message = "Robert'); DROP TABLE daily_usage; --";
  assert.equal((await send(h.mf, r)).status, 201);
  assert.equal((await h.db.prepare('SELECT exception_type FROM crash_reports').first()).exception_type, r.crash.type);
  assert.equal((await h.db.prepare('SELECT reports FROM daily_usage').first()).reports, 1);
});

test('Android redaction expansion remains compatible with the ingestion contract', async t => {
  const h = await harness(); t.after(h.close);
  const r = fixture();
  // CrashRedactor.take(2048) happens before replacing six-character emails.
  r.crash.message = 'a@b.co '.repeat(292).replaceAll('a@b.co', '[redacted]');
  r.device.model = 'a@b.co '.repeat(18).replaceAll('a@b.co', '[redacted]');
  assert(r.crash.message.length > 2048 && r.device.model.length > 128);
  assert.equal((await send(h.mf, r)).status, 201);
});

test('Discord receives only compact metadata for a newly accepted report', async t => {
  const requests = [];
  const h = await harness({
    discordWebhookUrl: 'https://discord.com/api/webhooks/12345/test-token',
    outboundService: async request => { requests.push(request); return new Response(null, { status: 204 }); },
  });
  t.after(h.close);
  const r = fixture(); r.crash.message = 'private message';
  assert.equal((await send(h.mf, r)).status, 201);
  for (let i = 0; i < 50 && requests.length === 0; i++) await new Promise(resolve => setTimeout(resolve, 10));
  assert.equal(requests.length, 1);
  const body = await requests[0].json();
  assert(body.content.includes(r.reportId));
  assert(body.content.includes(r.crash.type));
  assert(!JSON.stringify(body).includes('private message'));
  assert.deepEqual(body.allowed_mentions, { parse: [] });
  assert.equal((await send(h.mf, r)).status, 200);
  assert.equal(requests.length, 1, 'idempotent retries must not notify twice');
});

test('missing or invalid Discord configuration never prevents durable ingestion', async t => {
  const invalid = await harness({ discordWebhookUrl: 'https://example.test/api/webhooks/1/x' });
  t.after(invalid.close);
  assert.equal((await send(invalid.mf)).status, 201);
});
