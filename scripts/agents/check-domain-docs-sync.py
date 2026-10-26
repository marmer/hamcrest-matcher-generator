#!/usr/bin/env python3
"""Check that the agent-facing domain docs are mirrored in the human-facing arc42 docs.

Rules (see docs/agents/domain.md, "Redundancy rules"):
  1. Every term in GLOSSARY.md (lines of the form `**Term**:`) has a row in the table of
     docs/src/12_glossary.adoc (a `|Term` line followed by a `|definition` line).
  2. Every ADR file docs/adr/NNNN-*.md is linked from docs/src/09_architecture_decisions.adoc
     (matched by its filename).
  3. The glossary table contains no leftover template placeholder rows such as `<Term-1>`.
  4. Every required check that docs/agents/wayfinding.md names ("required checks of the
     `development` ruleset pass (`Build`, ...)") is the name of a job in .github/workflows/,
     with `${{ matrix.role }}` expanded to each `role:` of the job's matrix.

A missing GLOSSARY.md, docs/adr/ or required-checks list means there is nothing to check for that rule and it passes.

Usage:
  scripts/agents/check-domain-docs-sync.py              # check the repository this script lives in
  scripts/agents/check-domain-docs-sync.py <root-dir>   # check another checkout (e.g. a test copy)

Exits 1 and prints one line per violation if any rule is broken, otherwise 0.
"""
import re
import sys
from pathlib import Path

GLOSSARY_MD = "GLOSSARY.md"
ADR_DIR = "docs/adr"
GLOSSARY_ADOC = "docs/src/12_glossary.adoc"
DECISIONS_ADOC = "docs/src/09_architecture_decisions.adoc"
WAYFINDING_MD = "docs/agents/wayfinding.md"
WORKFLOW_DIR = ".github/workflows"

TERM_LINE = re.compile(r"^\*\*(.+?)\*\*:\s*$")
ADR_FILE = re.compile(r"^\d{4}-.+\.md$")
PLACEHOLDER = re.compile(r"<\s*Term[^>]*>|^<[^>]*>$", re.IGNORECASE)
REQUIRED_CHECKS = re.compile(r"required checks of the `development` ruleset pass \(([^)]*)\)")
JOB_NAME = re.compile(r"^    name:\s*(.+?)\s*$")
MATRIX_ROLE = re.compile(r"^\s*-?\s*role:\s*[\"']?([^\"'\s]+)[\"']?\s*$")


def read(path):
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def glossary_terms(root):
    path = root / GLOSSARY_MD
    if not path.is_file():
        return []
    return [m.group(1).strip() for line in read(path).splitlines() if (m := TERM_LINE.match(line))]


def glossary_table_rows(text):
    """Return (term, definition) pairs from the first table of the glossary adoc."""
    lines = text.splitlines()
    fences = [i for i, line in enumerate(lines) if line.strip() == "|==="]
    if len(fences) < 2:
        return []
    body = [line.strip() for line in lines[fences[0] + 1:fences[1]]]
    body = [line for line in body if line]
    rows = []
    i = 0
    while i < len(body):
        line = body[i]
        # A term row is `|Term` alone on its line; the header `|Term |Definition` has a second cell.
        if line.startswith("|") and "|" not in line[1:] and i + 1 < len(body) and body[i + 1].startswith("|"):
            rows.append((line[1:].strip(), body[i + 1][1:].strip()))
            i += 2
        else:
            i += 1
    return rows


def check_glossary(root, violations):
    terms = glossary_terms(root)
    if not terms:
        return
    text = read(root / GLOSSARY_ADOC)
    rows = glossary_table_rows(text)
    row_terms = {term for term, _ in rows}
    for term in terms:
        if term not in row_terms:
            violations.append(f"{GLOSSARY_ADOC}: glossary term '{term}' from {GLOSSARY_MD} has no table row")
    for term, _ in rows:
        if PLACEHOLDER.search(term):
            violations.append(f"{GLOSSARY_ADOC}: leftover template placeholder row '{term}'")
    for match in sorted(set(PLACEHOLDER.findall(text))):
        if not any(match == term for term, _ in rows):
            violations.append(f"{GLOSSARY_ADOC}: leftover template placeholder '{match}'")


def check_adrs(root, violations):
    adr_dir = root / ADR_DIR
    if not adr_dir.is_dir():
        return
    decisions = read(root / DECISIONS_ADOC)
    for adr in sorted(adr_dir.iterdir()):
        if adr.is_file() and ADR_FILE.match(adr.name) and adr.name not in decisions:
            violations.append(f"{DECISIONS_ADOC}: ADR file {ADR_DIR}/{adr.name} is not linked")


def workflow_job_names(root):
    """Job display names of every workflow, with `${{ matrix.role }}` expanded per matrix role."""
    names = set()
    workflow_dir = root / WORKFLOW_DIR
    if not workflow_dir.is_dir():
        return names
    for workflow in sorted(workflow_dir.glob("*.y*ml")):
        jobs = []  # (name, roles) per job, in file order
        for line in read(workflow).splitlines():
            if m := JOB_NAME.match(line):
                jobs.append((m.group(1).strip("\"'"), []))
            elif jobs and (m := MATRIX_ROLE.match(line)):
                jobs[-1][1].append(m.group(1))
        for name, roles in jobs:
            if "${{ matrix.role }}" in name:
                names.update(name.replace("${{ matrix.role }}", role) for role in roles)
            else:
                names.add(name)
    return names


def check_required_checks(root, violations):
    m = REQUIRED_CHECKS.search(read(root / WAYFINDING_MD))
    if not m:
        return
    jobs = workflow_job_names(root)
    for check in re.findall(r"`([^`]+)`", m.group(1)):
        if check not in jobs:
            violations.append(f"{WAYFINDING_MD}: required check '{check}' is not a job name in {WORKFLOW_DIR}/")


def main():
    if len(sys.argv) > 2:
        sys.exit(__doc__)
    root = Path(sys.argv[1]).resolve() if len(sys.argv) == 2 else Path(__file__).resolve().parents[2]
    violations = []
    check_glossary(root, violations)
    check_adrs(root, violations)
    check_required_checks(root, violations)
    for violation in violations:
        print(violation)
    if violations:
        print(f"domain docs out of sync: {len(violations)} violation(s)")
        sys.exit(1)
    print("domain docs in sync")


if __name__ == "__main__":
    main()
