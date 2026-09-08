#!/usr/bin/env node

/**
 * generate-changelog.mjs
 *
 * Compiles individual release markdown files from `docs/changelog/releases/`
 * into a single unified `docs/changelog/index.md` for the VitePress site.
 *
 * Filtering rules:
 * - Omits `<!-- release-only -->...<!-- /release-only -->` sections (e.g. installation guides).
 * - Retains and unwraps `<!-- changelog-only -->...<!-- /changelog-only -->` sections.
 * - Promotes/demotes headings so the page has a single top-level H1 (# Changelog),
 *   each release is an H2 (## Version {#anchor}), and release subsections are H3 (### Section).
 * - Adds a direct link to the corresponding GitHub Release for each version.
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const RELEASES_DIR = path.resolve(__dirname, '../docs/changelog/releases');
const OUTPUT_FILE = path.resolve(__dirname, '../docs/changelog/index.md');
const GITHUB_REPO = 'https://github.com/vibhor1102/Macrion';

function parseSemver(filename) {
  const match = filename.match(/^v?(\d+)\.(\d+)\.(\d+)(?:[-.]([0-9A-Za-z.-]+))?\.md$/);
  if (!match) return [0, 0, 0, ''];
  return [parseInt(match[1], 10), parseInt(match[2], 10), parseInt(match[3], 10), match[4] || ''];
}

function compareSemverDesc(fileA, fileB) {
  const [majA, minA, patchA, preA] = parseSemver(fileA);
  const [majB, minB, patchB, preB] = parseSemver(fileB);

  if (majA !== majB) return majB - majA;
  if (minA !== minB) return minB - minA;
  if (patchA !== patchB) return patchB - patchA;
  // If one has pre-release and one doesn't, standard release ranks higher
  if (!preA && preB) return -1;
  if (preA && !preB) return 1;
  return fileB.localeCompare(fileA);
}

function filterChangelogContent(rawMarkdown, version) {
  let content = rawMarkdown;

  // Strip Windows line endings
  content = content.replace(/\r\n/g, '\n');

  // Strip release-only blocks
  // Matches <!-- release-only --> ... <!-- /release-only -->
  // and <!-- release:start --> ... <!-- release:end -->
  content = content.replace(/<!--\s*release-only\s*-->[\s\S]*?<!--\s*\/release-only\s*-->/gi, '');
  content = content.replace(/<!--\s*release:start\s*-->[\s\S]*?<!--\s*\/release:end\s*-->/gi, '');

  // Unwrap changelog-only tags
  content = content.replace(/<!--\s*changelog-only\s*-->/gi, '');
  content = content.replace(/<!--\s*\/changelog-only\s*-->/gi, '');
  content = content.replace(/<!--\s*changelog:start\s*-->/gi, '');
  content = content.replace(/<!--\s*\/changelog:end\s*-->/gi, '');

  const lines = content.split('\n');
  const processedLines = [];
  let foundH1 = false;
  let title = `Macrion ${version.replace(/^v/, '')}`;
  const anchorId = version.replace(/\./g, '-').replace(/^v/, 'v');

  for (let line of lines) {
    // Top-level H1 heading: # Macrion X.Y.Z
    if (!foundH1 && line.startsWith('# ')) {
      foundH1 = true;
      title = line.replace(/^#\s+/, '').trim();
      // Format as H2 with custom anchor and GitHub Release link
      processedLines.push(`## ${title} {#${anchorId}}\n`);
      if (version !== 'v0.0.0') {
        processedLines.push(`<div class="release-actions">`);
        processedLines.push(`  <a href="${GITHUB_REPO}/releases/tag/${version}" target="_blank" rel="noopener noreferrer" class="release-tag-link">`);
        processedLines.push(`    <svg class="release-gh-icon" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>`);
        processedLines.push(`    <span>View Release on GitHub</span>`);
        processedLines.push(`  </a>`);
        processedLines.push(`</div>\n`);
      }
      continue;
    }

    // Demote subordinate headings by 1 level so H2 -> H3, H3 -> H4
    if (line.startsWith('## ')) {
      processedLines.push(`### ${line.replace(/^##\s+/, '')}`);
    } else if (line.startsWith('### ')) {
      processedLines.push(`#### ${line.replace(/^###\s+/, '')}`);
    } else {
      processedLines.push(line);
    }
  }

  // Trim extraneous leading/trailing blank lines
  return processedLines.join('\n').trim();
}

function generateChangelog() {
  if (!fs.existsSync(RELEASES_DIR)) {
    console.error(`Releases directory does not exist: ${RELEASES_DIR}`);
    process.exit(1);
  }

  const files = fs.readdirSync(RELEASES_DIR)
    .filter(f => f.endsWith('.md') && f.startsWith('v'))
    .sort(compareSemverDesc);

  if (files.length === 0) {
    console.warn('No release markdown files found.');
    return;
  }

  const releaseSections = files.map(file => {
    const rawContent = fs.readFileSync(path.join(RELEASES_DIR, file), 'utf8');
    const version = file.replace(/\.md$/, '');
    return filterChangelogContent(rawContent, version);
  });

  const pageContent = [
    '---',
    'title: Changelog',
    'description: Comprehensive release notes and version history for Macrion.',
    'aside: false',
    'prev: false',
    'next: false',
    '---',
    '',
    '# Changelog',
    '',
    'All notable changes to the **Macrion** project are documented on this page. Each version reflects a published release on GitHub.',
    '',
    '---',
    '',
    releaseSections.join('\n\n---\n\n'),
    '',
  ].join('\n');

  fs.writeFileSync(OUTPUT_FILE, pageContent, 'utf8');
  console.log(`Successfully generated changelog: ${OUTPUT_FILE} (${files.length} releases compiled).`);
}

generateChangelog();
