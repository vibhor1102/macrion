#!/usr/bin/env python3
#
# Copyright (C) 2026 Vibhor Goel
# SPDX-License-Identifier: GPL-3.0-or-later
#
"""
Parses JUnit XML test results and formats a GitHub Actions Job Summary ($GITHUB_STEP_SUMMARY).
Shows ONLY failures with actionable stack traces at the top, omitting all passing test noise.
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
    search_pattern = os.path.join(repo_root, "**/build/test-results/testFDroidDebugUnitTest/TEST-*.xml")
    xml_files = sorted(glob.glob(search_pattern, recursive=True))

    if not xml_files:
        summary = "### ⚠️ No unit test results found\nNo test result XML files were produced.\n"
        output_summary(summary)
        return

    total_tests = 0
    total_failures = 0
    total_errors = 0
    total_skipped = 0
    total_time = 0.0

    failed_cases = []
    skipped_cases = []

    for file_path in xml_files:
        try:
            tree = ET.parse(file_path)
            root = tree.getroot()
            suites = [root] if root.tag == "testsuite" else root.findall("testsuite")

            for suite in suites:
                s_tests = int(suite.get("tests", 0))
                s_failures = int(suite.get("failures", 0))
                s_errors = int(suite.get("errors", 0))
                s_skipped = int(suite.get("skipped", 0))
                s_time = float(suite.get("time", 0.0))
                s_name = suite.get("name", "UnknownSuite")

                total_tests += s_tests
                total_failures += s_failures
                total_errors += s_errors
                total_skipped += s_skipped
                total_time += s_time

                for case in suite.findall("testcase"):
                    case_name = case.get("name", "")
                    classname = case.get("classname", s_name)
                    c_time = case.get("time", "0.0")

                    failure_el = case.find("failure")
                    error_el = case.find("error")
                    skipped_el = case.find("skipped")

                    if failure_el is not None or error_el is not None:
                        fail_node = failure_el if failure_el is not None else error_el
                        msg = fail_node.get("message", "")
                        stacktrace = (fail_node.text or "").strip()
                        failed_cases.append({
                            "suite": classname,
                            "case": case_name,
                            "message": msg,
                            "stacktrace": stacktrace,
                            "time": c_time,
                        })
                    elif skipped_el is not None:
                        skipped_cases.append({
                            "suite": classname,
                            "case": case_name,
                        })
        except Exception as e:
            print(f"Warning: Failed to parse {file_path}: {e}", file=sys.stderr)

    total_failed = total_failures + total_errors
    total_passed = total_tests - total_failed - total_skipped

    lines = []

    if total_failed == 0:
        lines.append(f"## ✅ Unit Tests: All {total_passed} tests passed ({total_time:.2f}s)")
        if skipped_cases:
            lines.append(f"\n_{len(skipped_cases)} skipped test(s)_\n")
    else:
        lines.append(f"## ❌ Unit Tests: {total_failed} Failed (out of {total_tests} tests, {total_time:.2f}s)\n")

        # ONLY FAILING TESTS TABLE
        lines.append("| Suite | Test Case | Failure Summary |")
        lines.append("| :--- | :--- | :--- |")
        for f in failed_cases:
            short_suite = f["suite"].split(".")[-1]
            summary_msg = escape_markdown(truncate(f["message"] or "Failed without explicit message", 120))
            lines.append(f"| `{short_suite}` | `{escape_markdown(f['case'])}` | {summary_msg} |")
        lines.append("")

        lines.append("<details open>\n<summary>🔍 <b>Stack Traces</b></summary>\n")
        for f in failed_cases:
            lines.append(f"#### `{f['suite']}` > `{f['case']}`")
            if f["message"]:
                lines.append(f"> **Message:** {escape_markdown(f['message'])}\n")
            if f["stacktrace"]:
                lines.append("```")
                lines.append(f["stacktrace"])
                lines.append("```\n")
        lines.append("</details>\n")

        if skipped_cases:
            lines.append(f"<details>\n<summary>⚠️ <b>Skipped Tests ({len(skipped_cases)})</b></summary>\n")
            lines.append("| Suite | Test Case |")
            lines.append("| :--- | :--- |")
            for s in skipped_cases:
                lines.append(f"| `{s['suite']}` | `{escape_markdown(s['case'])}` |")
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
