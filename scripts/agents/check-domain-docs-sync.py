#!/usr/bin/env python3
"""Check that the agent-facing domain docs are mirrored in the human-facing arc42 docs.

Rules (see docs/agents/domain.md, "Redundancy rules"):
  1. Every term in GLOSSARY.md (lines of the form `**Term**:`) has a row in the table of
     docs/src/docs/asciidoc/architecture/_chapters/12_glossary.adoc (a `|Term` line followed by a `|definition` line).
  2. Every ADR file docs/adr/NNNN-*.md is linked from docs/src/docs/asciidoc/architecture/_chapters/09_architecture_decisions.adoc
     (matched by its filename).
  3. The glossary table contains no leftover template placeholder rows such as `<Term-1>`.
  4. Every required check that docs/agents/wayfinding.md names ("required checks of the
     `main` ruleset pass (`Build`, ...)") is the name of a job in .github/workflows/,
     with `${{ matrix.role }}` expanded to each `role:` of the job's matrix.
  5. Every backticked file name in CLAUDE.md ending in .java, .kt, .adoc, .yml or .xml exists in the repository
     (files git tracks or would add; ignored ones such as target/ do not count; a name with a `/` matches as a path
     suffix, a bare name as a file name; names starting with `~` or `/` are outside the repository and skipped), and
     every backticked `io.github.marmer...` package exists as a directory under some src/*/java.
  6. Every public type declared in the core `spi` package (matcher-generator-core/src/main/java/**/spi) is named in
     docs/src/docs/asciidoc/architecture/_chapters/05_building_block_view.adoc.

A missing GLOSSARY.md, docs/adr/ or required-checks list means there is nothing to check for that rule and it passes.

Usage:
  scripts/agents/check-domain-docs-sync.py              # check the repository this script lives in
  scripts/agents/check-domain-docs-sync.py <root-dir>   # check another checkout (e.g. a test copy)

Exits 1 and prints one line per violation if any rule is broken, otherwise 0.
"""
import re
import subprocess
import sys
from pathlib import Path

from workflow_jobs import workflow_job_names

GLOSSARY_MD = "GLOSSARY.md"
ADR_DIR = "docs/adr"
GLOSSARY_ADOC = "docs/src/docs/asciidoc/architecture/_chapters/12_glossary.adoc"
DECISIONS_ADOC = "docs/src/docs/asciidoc/architecture/_chapters/09_architecture_decisions.adoc"
WAYFINDING_MD = "docs/agents/wayfinding.md"
WORKFLOW_DIR = ".github/workflows"
CLAUDE_MD = "CLAUDE.md"
BUILDING_BLOCKS_ADOC = "docs/src/docs/asciidoc/architecture/_chapters/05_building_block_view.adoc"
CORE_MAIN_SOURCES = "matcher-generator-core/src/main/java"

TERM_LINE = re.compile(r"^\*\*(.+?)\*\*:\s*$")
ADR_FILE = re.compile(r"^\d{4}-.+\.md$")
PLACEHOLDER = re.compile(r"<\s*Term[^>]*>|^<[^>]*>$", re.IGNORECASE)
REQUIRED_CHECKS = re.compile(r"required checks of the `main` ruleset pass \(([^)]*)\)")
BACKTICKED = re.compile(r"`([^`\n]+)`")
FILE_NAME = re.compile(r"[\w.-]+\.(?:java|kt|adoc|yml|xml)")
PUBLIC_TYPE = re.compile(
    r"^public\s+(?:(?:abstract|final|sealed|non-sealed|static)\s+)*(?:@?interface|class|record|enum)\s+(\w+)", re.MULTILINE)
PACKAGE = re.compile(r"io\.github\.marmer(?:\.\w+)*")


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


def check_required_checks(root, violations):
    m = REQUIRED_CHECKS.search(read(root / WAYFINDING_MD))
    if not m:
        return
    jobs = workflow_job_names(root)
    for check in re.findall(r"`([^`]+)`", m.group(1)):
        if check not in jobs:
            violations.append(f"{WAYFINDING_MD}: required check '{check}' is not a job name in {WORKFLOW_DIR}/")


def repository_files_and_packages(root):
    """Return (file paths, package names found as directories under src/*/java) of the files git tracks or would add.

    Ignored files (target/, .claude/worktrees/) never count.
    """
    listing = subprocess.run(["git", "-C", str(root), "ls-files", "--cached", "--others", "--exclude-standard"],
                             check=True, capture_output=True, text=True).stdout
    files, packages = set(listing.splitlines()), set()
    for file in files:
        parts = file.split("/")[:-1]
        for i in range(len(parts) - 2):
            if parts[i] == "src" and parts[i + 2] == "java":
                packages.update(".".join(parts[i + 3:j]) for j in range(i + 4, len(parts) + 1))
    return files, packages


def file_exists(name, files):
    """A name with a `/` matches as a path suffix, a bare name as a file name anywhere."""
    return any(file == name or file.endswith("/" + name) for file in files)


def check_claude_md_references(root, violations):
    text = read(root / CLAUDE_MD)
    if not text:
        return
    files, packages = repository_files_and_packages(root)
    for token in dict.fromkeys(BACKTICKED.findall(text)):
        if token.startswith(("~", "/")):
            continue  # outside the repository
        if FILE_NAME.fullmatch(token.rsplit("/", 1)[-1]):
            if not file_exists(token, files):
                violations.append(f"{CLAUDE_MD}: file '{token}' does not exist in the repository")
        elif PACKAGE.fullmatch(token) and token not in packages:
            violations.append(f"{CLAUDE_MD}: package '{token}' is not a directory under any src/*/java")


def check_spi_types_named(root, violations):
    chapter = read(root / BUILDING_BLOCKS_ADOC)
    for spi_dir in sorted((root / CORE_MAIN_SOURCES).glob("**/spi")):
        for source in sorted(spi_dir.rglob("*.java")):
            for name in PUBLIC_TYPE.findall(read(source)):
                if not re.search(rf"\b{name}\b", chapter):
                    violations.append(
                        f"{BUILDING_BLOCKS_ADOC}: SPI type '{name}' from {source.relative_to(root)} is not named")


def main():
    if len(sys.argv) > 2:
        sys.exit(__doc__)
    root = Path(sys.argv[1]).resolve() if len(sys.argv) == 2 else Path(__file__).resolve().parents[2]
    violations = []
    check_glossary(root, violations)
    check_adrs(root, violations)
    check_required_checks(root, violations)
    check_claude_md_references(root, violations)
    check_spi_types_named(root, violations)
    for violation in violations:
        print(violation)
    if violations:
        print(f"domain docs out of sync: {len(violations)} violation(s)")
        sys.exit(1)
    print("domain docs in sync")


if __name__ == "__main__":
    main()
