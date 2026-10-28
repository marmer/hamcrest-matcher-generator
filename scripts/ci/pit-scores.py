#!/usr/bin/env python3
"""Mutation score per module from the PIT reports of `./mvnw verify -Pmutation`, as a Markdown table.

PIT writes `<module>/target/pit-reports/mutations.xml` (timestampedReports=false). The score is what PIT's
mutationThreshold checks: detected mutants over all mutants, uncovered ones included.

Usage: scripts/ci/pit-scores.py [<root-dir>] [--threshold 90] [--expect <module>...]
Prints the table (the CI job writes it to the job summary). Exit 1 when a module is below the threshold,
when a module named with --expect has no report, or when no report exists at all.
"""
import argparse
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

REPORT = Path("target") / "pit-reports" / "mutations.xml"


def mutation_counts(xml_text):
    """(detected, total) over every <mutation> element."""
    mutations = ElementTree.fromstring(xml_text).iter("mutation")
    detected = total = 0
    for mutation in mutations:
        total += 1
        if mutation.get("detected") == "true":
            detected += 1
    return detected, total


def score_percent(detected, total):
    """Mutation score in percent, None without mutants."""
    return None if total == 0 else 100.0 * detected / total


def measured_modules(root):
    """{module: (detected, total)} for every <module>/target/pit-reports/mutations.xml under root, sorted by name."""
    scores = {}
    for report in sorted(root.glob("*/" + str(REPORT))):
        scores[report.parents[2].name] = mutation_counts(report.read_text(encoding="utf-8"))
    return scores


def verdict(detected, total, threshold):
    score = score_percent(detected, total)
    if score is None:
        return "no mutations"
    return "pass" if score >= threshold else f"below {threshold}"


def markdown_table(scores, threshold):
    lines = ["| Module | Detected / mutants | Score | Verdict |", "| --- | --- | --- | --- |"]
    for module, (detected, total) in sorted(scores.items()):
        score = score_percent(detected, total)
        shown = "-" if score is None else f"{score:.1f} %"
        lines.append(f"| `{module}` | {detected} / {total} | {shown} | {verdict(detected, total, threshold)} |")
    return "\n".join(lines)


def failing_modules(scores, expected, threshold):
    """Modules below the threshold plus the expected modules that have no report."""
    below = [module for module, (detected, total) in sorted(scores.items())
             if score_percent(detected, total) is not None and score_percent(detected, total) < threshold]
    missing = [module for module in expected if module not in scores]
    return below + missing


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("root", nargs="?", default=".")
    parser.add_argument("--threshold", type=float, default=90)
    parser.add_argument("--expect", nargs="*", default=[])
    args = parser.parse_args()
    scores = measured_modules(Path(args.root).resolve())
    if not scores:
        print("no PIT report found under the root directory")
        sys.exit(1)
    print(markdown_table(scores, args.threshold))
    failing = failing_modules(scores, args.expect, args.threshold)
    if failing:
        print(f"\nmodules below {args.threshold:g} % or without a report: {', '.join(failing)}")
        sys.exit(1)
    print(f"\nevery module reaches {args.threshold:g} %")


if __name__ == "__main__":
    main()
