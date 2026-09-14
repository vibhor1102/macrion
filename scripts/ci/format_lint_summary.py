#!/usr/bin/env python3
#
# Copyright (C) 2026 Vibhor Goel
# SPDX-License-Identifier: GPL-3.0-or-later
#
"""
Parses Android Lint XML results and formats a GitHub Actions Job Summary ($GITHUB_STEP_SUMMARY).
Presents errors prominently at the top with file locations, followed by warnings.
"""

import glob
import os
import sys
import xml.etree.ElementTree as ET


def escape_markdown(text: str) -> str:
    return text.replace("|", "\\|").replace("\n", " ").strip()


def truncate(text: str, max_length: int = 150) -> str:
    text = text.strip()
    if len(text) <= max_length:
        return text
    return text[:max_length] + "…"


def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    search_pattern = os.path.join(repo_root, "**/build/reports/lint-results*.xml")
    xml_files = sorted(glob.glob(search_pattern, recursive=True))

    if not xml_files:
        summary = "### ⚠️ No Android Lint results found\nNo lint result XML files were produced.\n"
        output_summary(summary)
        return

    errors = []
    warnings = []
    infos = []

    for file_path in xml_files:
        try:
            tree = ET.parse(file_path)
            root = tree.getroot()
            for issue in root.findall("issue"):
                issue_id = issue.get("id", "UnknownIssue")
                severity = issue.get("severity", "Warning").capitalize()
                message = issue.get("message", "")
                category = issue.get("category", "")
                explanation = (issue.get("explanation") or "").strip()

                locations = []
                for loc in issue.findall("location"):
                    loc_file = loc.get("file", "")
                    loc_line = loc.get("line")
                    if loc_file:
                        rel_file = os.path.relpath(loc_file, repo_root) if os.path.isabs(loc_file) else loc_file
                        loc_str = f"{rel_file}:{loc_line}" if loc_line else rel_file
                        locations.append(loc_str)

                loc_display = ", ".join(locations) if locations else "N/A"

                item = {
                    "id": issue_id,
                    "severity": severity,
                    "message": message,
                    "location": loc_display,
                    "category": category,
                    "explanation": explanation,
                }

                if severity == "Error" or severity == "Fatal":
                    errors.append(item)
                elif severity == "Warning":
                    warnings.append(item)
                else:
                    infos.append(item)
        except Exception as e:
            print(f"Warning: Failed to parse lint file {file_path}: {e}", file=sys.stderr)

    lines = []

    # 1. Main Header
    if errors:
        lines.append(f"## ❌ Android Lint: {len(errors)} Error(s), {len(warnings)} Warning(s)\n")
    elif warnings:
        lines.append(f"## ⚠️ Android Lint: 0 Errors, {len(warnings)} Warning(s)\n")
    else:
        lines.append("## ✅ Android Lint: 0 Errors, 0 Warnings\n")

    # 2. ERRORS FIRST (at the top)
    if errors:
        lines.append(f"### ❌ Lint Errors ({len(errors)})\n")
        lines.append("| Issue | Location | Message | Category |")
        lines.append("| :--- | :--- | :--- | :--- |")
        for err in errors:
            msg = escape_markdown(truncate(err["message"], 120))
            lines.append(f"| `{err['id']}` | `{escape_markdown(err['location'])}` | {msg} | {escape_markdown(err['category'])} |")
        lines.append("")

        lines.append("<details>\n<summary>🔍 <b>Detailed Error Explanations</b></summary>\n")
        for err in errors:
            lines.append(f"#### `{err['id']}` at `{err['location']}`")
            lines.append(f"> **Message:** {escape_markdown(err['message'])}\n")
            if err["explanation"]:
                lines.append(f"{err['explanation']}\n")
        lines.append("</details>\n")

    # 3. Warnings (in collapsible details so they don't drown out errors)
    if warnings:
        lines.append(f"<details>\n<summary>⚠️ <b>Lint Warnings ({len(warnings)})</b></summary>\n")
        lines.append("| Issue | Location | Message |")
        lines.append("| :--- | :--- | :--- |")
        for w in warnings:
            msg = escape_markdown(truncate(w["message"], 120))
            lines.append(f"| `{w['id']}` | `{escape_markdown(w['location'])}` | {msg} |")
        lines.append("</details>\n")

    # 4. Info (if any)
    if infos:
        lines.append(f"<details>\n<summary>ℹ️ <b>Informational Notices ({len(infos)})</b></summary>\n")
        lines.append("| Issue | Location | Message |")
        lines.append("| :--- | :--- | :--- |")
        for info in infos:
            msg = escape_markdown(truncate(info["message"], 120))
            lines.append(f"| `{info['id']}` | `{escape_markdown(info['location'])}` | {msg} |")
        lines.append("</details>\n")

    summary_content = "\n".join(lines)
    output_summary(summary_content)


def output_summary(content: str):
    print(content)
    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_file:
        try:
            with open(summary_file, "a", encoding="utf-8") as fp:
                fp.write(content + "\n")
        except Exception as e:
            print(f"Error writing to GITHUB_STEP_SUMMARY: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
