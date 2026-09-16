// Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later
import { spawnSync } from 'node:child_process';

const [action = 'list', id, confirmation] = process.argv.slice(2);
const validId = value => typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(value);
if (!['list', 'get', 'decode', 'delete', 'stats'].includes(action) || (['get', 'decode', 'delete'].includes(action) && !validId(id))) {
  console.error('Usage: npm run reports -- list|stats|decode <report-id>|get <report-id>|delete <report-id> --yes');
  process.exit(2);
}
if (action === 'delete' && confirmation !== '--yes') {
  console.error('Deletion requires an exact report ID and --yes.');
  process.exit(2);
}
const sql = action === 'list'
  ? 'SELECT report_id, received_at, expires_at, payload_bytes, version_code, exception_type FROM crash_reports ORDER BY received_at DESC LIMIT 100'
  : action === 'stats'
    ? 'SELECT day, reports, bytes FROM daily_usage ORDER BY day DESC LIMIT 31'
    : ['get', 'decode'].includes(action)
      ? `SELECT payload FROM crash_reports WHERE report_id = '${id}'`
      : `DELETE FROM crash_reports WHERE report_id = '${id}' RETURNING report_id`;
const wrangler = new URL('../node_modules/wrangler/bin/wrangler.js', import.meta.url).pathname;
const result = spawnSync(process.execPath, [wrangler, 'd1', 'execute', 'DB', '--remote', '--json', '--command', sql], {
  encoding: 'utf8', env: { ...process.env, WRANGLER_SEND_METRICS: 'false' }, maxBuffer: 2 * 1024 * 1024,
});
if (result.status !== 0) {
  console.error('Cloudflare D1 command failed.');
  process.exit(result.status ?? 1);
}
let output;
try { output = JSON.parse(result.stdout); } catch { console.error('Could not parse Cloudflare response.'); process.exit(1); }
const rows = output.flatMap(entry => entry.results ?? []);
if (['get', 'decode'].includes(action)) {
  if (rows.length !== 1) { console.error('Report not found.'); process.exit(1); }
  if (action === 'decode') {
    const decoder = new URL('../../../scripts/diagnostics/decode.py', import.meta.url).pathname;
    const decoded = spawnSync('python3', [decoder], { input: rows[0].payload, stdio: ['pipe', 'inherit', 'inherit'] });
    process.exit(decoded.status ?? 1);
  }
  console.log(JSON.stringify(JSON.parse(rows[0].payload), null, 2));
} else if (rows.length) console.table(rows.map(row => action === 'list' ? {
  ...row,
  received_at: new Date(row.received_at * 1000).toISOString(),
  expires_at: new Date(row.expires_at * 1000).toISOString(),
} : row));
else console.log(action === 'delete' ? 'Report not found.' : 'No rows.');
