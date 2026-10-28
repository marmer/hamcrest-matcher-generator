#!/usr/bin/env python3
"""Markdown summary of a SCANOSS scan (`scanoss-py scan --output ...`) for the job summary. Never fails: the scan is non-blocking.

Usage:
  scripts/ci/scanoss-summary.py <scanoss-results.json> >> "$GITHUB_STEP_SUMMARY"
"""
import json
import sys
from pathlib import Path

NO_RESULTS = "## SCANOSS\n\nThe scan did not produce results (see the step log).\n"


def summarize(results):
    """Markdown for a scan result (file -> list of matches, `id` `none` when nothing matched)."""
    if results is None:
        return NO_RESULTS
    matches = {}  # (component, license) -> files
    matched_files = set()
    for file_name, entries in results.items():
        for entry in entries:
            if entry.get("id") == "none":
                continue
            matched_files.add(file_name)
            component = entry.get("component", "unknown")
            names = {license_.get("name", "unknown") for license_ in entry.get("licenses", [])}
            licenses = sorted(names) or ["undeclared"]
            for license_name in licenses:
                matches.setdefault((component, license_name), set()).add(entry.get("file", file_name))
    lines = ["## SCANOSS", "",
             f"Matched {len(matched_files)} of {len(results)} scanned files. "
             "Non-blocking: nothing here fails the build.", ""]
    if matches:
        lines += ["| Component | License | Files |", "|---|---|---|"]
        lines += [f"| {component} | {license_name} | {len(files)} |"
                  for (component, license_name), files in sorted(matches.items())]
    return "\n".join(lines) + "\n"


def main(argv):
    results = None
    if argv and Path(argv[0]).is_file():
        try:
            results = json.loads(Path(argv[0]).read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            results = None
    if not isinstance(results, dict):
        results = None
    print(summarize(results))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
