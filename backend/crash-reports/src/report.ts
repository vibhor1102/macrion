// Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later
export const MAX_BYTES = 262144;
type Obj = Record<string, unknown>;
export class InvalidReport extends Error {}
function requireValue(ok: unknown): asserts ok { if (!ok) throw new InvalidReport(); }
function object(value: unknown, required: string[], optional: string[] = []): Obj {
  requireValue(value !== null && typeof value === "object" && !Array.isArray(value));
  const o = value as Obj;
  requireValue(required.every(k => Object.hasOwn(o, k)));
  requireValue(Object.keys(o).every(k => required.includes(k) || optional.includes(k)));
  return o;
}
function string(value: unknown, max: number): asserts value is string {
  requireValue(typeof value === "string" && value.length <= max);
}
// Android bounds the input to its redactor, not the output. Short emails/paths can
// expand to "[redacted]". Twice the input bound accommodates every v1 pattern.
function diagnosticString(value: unknown, inputBound: number): asserts value is string { string(value, inputBound * 2); }
function integer(value: unknown, min = -2147483648, max = 2147483647) {
  requireValue(typeof value === "number" && Number.isSafeInteger(value) && value >= min && value <= max);
}
function boolean(value: unknown) { requireValue(typeof value === "boolean"); }
function optional(o: Obj, key: string, check: (v: unknown) => void) {
  if (Object.hasOwn(o, key) && o[key] !== null) check(o[key]);
}
function array(value: unknown, max: number): unknown[] {
  requireValue(Array.isArray(value) && value.length <= max); return value;
}
export interface Report extends Obj {
  reportId: string;
  build: Obj & { versionCode: number };
  crash: Obj & { type: string };
}

// Matches CrashReportFactory v1, including omitted JSONObject nulls and truncated graphs.
// An iterative walk rejects deep attacker-controlled trees before any recursive processing.
export function validateReport(input: unknown): Report {
  const root = object(input, ["schemaVersion", "redactionVersion", "reportId", "build", "device",
    "mainThread", "crash", "recentOperations", "redactionCount", "truncated"]);
  requireValue(root.schemaVersion === 1 && root.redactionVersion === 1);
  string(root.reportId, 36);
  requireValue(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(root.reportId));
  boolean(root.mainThread); boolean(root.truncated); integer(root.redactionCount, 0);
  const b = object(root.build, ["versionName", "versionCode", "flavor", "buildType"]);
  diagnosticString(b.versionName, 128); integer(b.versionCode, 0, Number.MAX_SAFE_INTEGER);
  requireValue(b.flavor === "fDroid");
  requireValue(["debug", "release"].includes(b.buildType as string));
  const d = object(root.device, ["androidVersion", "api", "manufacturer", "model", "abi"]);
  for (const key of ["androidVersion", "manufacturer", "model", "abi"]) diagnosticString(d[key], 128);
  integer(d.api, 1, 1000);
  for (const entry of array(root.recentOperations, 50)) {
    const e = object(entry, ["event", "millisecondsBeforeCrash"]);
    requireValue(["HOME_OPENED", "SETTINGS_OPENED"].includes(e.event as string));
    integer(e.millisecondsBeforeCrash, 0, Number.MAX_SAFE_INTEGER);
  }
  const pending = [{ value: root.crash, depth: 0 }];
  let nodes = 0, frames = 0, placeholders = 0;
  while (pending.length) {
    const { value, depth } = pending.pop()!;
    const e = object(value, [], ["type", "message", "frames", "cause", "suppressed", "detection", "nativeExit", "truncated"]);
    requireValue(depth <= 8);
    if (e.truncated === true) {
      requireValue(depth > 0 && Object.keys(e).length === 1 && ++placeholders <= 80);
      continue;
    }
    requireValue(++nodes <= 16 && depth < 8 && !Object.hasOwn(e, "truncated"));
    diagnosticString(e.type, 256); requireValue(e.type.length > 0);
    optional(e, "message", v => diagnosticString(v, 2048));
    // The factory's oversized fallback intentionally contains only type/message.
    requireValue((Object.hasOwn(e, "frames") && Object.hasOwn(e, "suppressed")) ||
      (depth === 0 && root.truncated === true && Object.keys(e).every(k => ["type", "message"].includes(k))));
    if (e.frames !== undefined) for (const frame of array(e.frames, 256)) {
      requireValue(++frames <= 256);
      const f = object(frame, ["class", "method", "line", "native"], ["file"]);
      diagnosticString(f.class, 256); diagnosticString(f.method, 256); optional(f, "file", v => diagnosticString(v, 128));
      integer(f.line); boolean(f.native);
    }
    if (e.suppressed !== undefined) for (const child of array(e.suppressed, 4)) pending.push({ value: child, depth: depth + 1 });
    if (e.cause !== undefined) pending.push({ value: e.cause, depth: depth + 1 });
    if (e.detection !== undefined) {
      const required = ["screenWidth", "screenHeight", "areaLeft", "areaTop", "areaWidth", "areaHeight", "threshold"];
      const opt = ["textLength", "originalWidth", "originalHeight", "scaledWidth", "scaledHeight", "color", "numberFormat"];
      const c = object(e.detection, ["operation", ...required], ["modelId", ...opt]);
      requireValue(["IMAGE", "COLOR", "TEXT", "NUMBER"].includes(c.operation as string));
      for (const key of required) integer(c[key]);
      for (const key of opt) optional(c, key, integer);
      optional(c, "modelId", v => diagnosticString(v, 128));
    }
    if (e.nativeExit !== undefined) {
      requireValue(depth === 0 && e.type === "android.native_crash" && root.mainThread === false);
      requireValue(array(e.frames, 256).length === 0 && array(e.suppressed, 4).length === 0);
      requireValue(!Object.hasOwn(e, "message") && !Object.hasOwn(e, "cause") && !Object.hasOwn(e, "detection"));
      const nativeExit = object(e.nativeExit, ["occurredAtMs", "status", "importance", "pssKb", "rssKb"]);
      integer(nativeExit.occurredAtMs, 0, Number.MAX_SAFE_INTEGER);
      integer(nativeExit.status); integer(nativeExit.importance);
      integer(nativeExit.pssKb, 0, Number.MAX_SAFE_INTEGER); integer(nativeExit.rssKb, 0, Number.MAX_SAFE_INTEGER);
    } else requireValue(e.type !== "android.native_crash");
  }
  return root as Report;
}

const patterns = [
  /\b(?:https?|content|file):\/\/[^\s<>"']+/gi,
  /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi,
  /\b(?:bearer|basic)\s+[A-Z0-9+/=._~-]+/gi,
  /\b(?:password|passwd|token|access_token|refresh_token|api[_-]?key|secret|authorization)\b["']?\s*[:=]\s*(?:"[^"]*"|'[^']*'|[^\s,;}]+)/gi,
  /\/(?:storage|sdcard|data\/(?:user|user_de|data)|home)\/[^\s<>"']+/g,
];
// Validate first. Sorting object keys makes formatting/key-order-only retries identical.
export function sanitizedJson(report: Report): string {
  function visit(v: unknown): unknown {
    if (typeof v === "string") return patterns.reduce((s, p) => s.replace(p, "[redacted]"), v);
    if (Array.isArray(v)) return v.map(visit);
    if (v !== null && typeof v === "object") return Object.fromEntries(
      Object.entries(v).sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0).map(([k, child]) => [k, visit(child)]));
    return v;
  }
  return JSON.stringify(visit(report));
}
