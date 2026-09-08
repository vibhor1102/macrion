#!/usr/bin/env node

/**
 * prepare-release-notes.mjs
 *
 * Prepares GitHub release notes from a source release markdown file
 * (e.g. `docs/changelog/releases/v0.3.0.md`).
 *
 * Filtering rules:
 * - Omits `<!-- changelog-only -->...<!-- /changelog-only -->` blocks.
 * - Retains and unwraps `<!-- release-only -->...<!-- /release-only -->` blocks (e.g. Installation guides).
 * - Ensures an Installation guidance suffix exists; if missing, appends the standard Macrion build selection guide.
 *
 * CLI Usage:
 *   node scripts/prepare-release-notes.mjs <input-file> [output-file]
 */

import fs from 'node:fs';
import path from 'node:path';

const DEFAULT_INSTALLATION_SUFFIX = `
## Installation

Download the APK matching your device. \`arm64-v8a\` is the standard choice for modern Android devices; use the universal APK if you are unsure.

Back up important scenarios before upgrading. Macrion is based on [Klick'r](https://github.com/Nain57/Smart-AutoClicker), and the existing Klick'r 4.0.1 feature set remains available. Report issues through the repository's issue tracker.
`.trim();

function prepareReleaseNotes(inputFile, outputFile) {
  if (!fs.existsSync(inputFile)) {
    console.error(`Input release notes file not found: ${inputFile}`);
    process.exit(1);
  }

  let content = fs.readFileSync(inputFile, 'utf8');

  // Normalize line endings
  content = content.replace(/\r\n/g, '\n');

  // 1. Strip changelog-only blocks
  content = content.replace(/<!--\s*changelog-only\s*-->[\s\S]*?<!--\s*\/changelog-only\s*-->/gi, '');
  content = content.replace(/<!--\s*changelog:start\s*-->[\s\S]*?<!--\s*\/changelog:end\s*-->/gi, '');

  // 2. Unwrap release-only tags (keep the content inside)
  content = content.replace(/<!--\s*release-only\s*-->/gi, '');
  content = content.replace(/<!--\s*\/release-only\s*-->/gi, '');
  content = content.replace(/<!--\s*release:start\s*-->/gi, '');
  content = content.replace(/<!--\s*\/release:end\s*-->/gi, '');

  content = content.trim();

  // 3. Check if an Installation section exists; append standard fallback if absent
  if (!/^##\s+Installation/m.test(content)) {
    content += '\n\n' + DEFAULT_INSTALLATION_SUFFIX + '\n';
  }

  // 4. Remap relative doc links to full website URLs for GitHub Releases
  content = content.replace(/\(\/documentation\//g, '(https://vibhor1102.github.io/macrion/documentation/');
  content = content.replace(/\(\/guides\//g, '(https://vibhor1102.github.io/macrion/guides/');

  if (outputFile) {
    const outDir = path.dirname(outputFile);
    if (!fs.existsSync(outDir)) {
      fs.mkdirSync(outDir, { recursive: true });
    }
    fs.writeFileSync(outputFile, content + '\n', 'utf8');
    console.log(`Prepared release notes written to: ${outputFile}`);
  } else {
    process.stdout.write(content + '\n');
  }
}

const args = process.argv.slice(2);
if (args.length === 0) {
  console.error('Usage: node scripts/prepare-release-notes.mjs <input-file> [output-file]');
  process.exit(1);
}

const inputFile = path.resolve(process.cwd(), args[0]);
const outputFile = args[1] ? path.resolve(process.cwd(), args[1]) : null;

prepareReleaseNotes(inputFile, outputFile);
