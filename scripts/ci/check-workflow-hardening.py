#!/usr/bin/env python3
"""Fail when a workflow in .github/workflows/ breaks the pipeline hardening rules.

Stdlib only and line based, like scripts/agents/workflow_jobs.py.

Rules:
- every `uses:` is pinned to a full commit SHA (local `./` actions are exempt; Renovate keeps the version
  as a trailing comment);
- the top-level `permissions:` is exactly `contents: read`, per-job additions belong to the job that needs them;
- every `setup-java` step has `cache: maven`, and no step caches ~/.m2 by hand
  (the Sonar and PIT caches are not Maven caches);
- the workflows that run required checks (`ci-build.yml`, `domain-docs-sync.yml`) run on `pull_request` and on
  `push` to main, not on every branch.

Usage:
  scripts/ci/check-workflow-hardening.py [<root-dir>]
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW_DIR = ".github/workflows"
# Workflows whose jobs are required checks (ruleset): pull_request plus push to main only, no bare `push:`.
REQUIRED_CHECK_WORKFLOWS = ("ci-build.yml", "domain-docs-sync.yml")

USES = re.compile(r"^\s*(?:-\s+)?uses:\s*(\S+)")
STEP_START = re.compile(r"^(\s*)-\s")
FULL_SHA = re.compile(r"^[0-9a-f]{40}$")
CACHE_KEY_MAVEN = re.compile(r"^\s+cache:\s*maven\s*(#.*)?$")
M2_PATH = re.compile(r"^\s+path:.*\.m2\b")


def _indent(line):
    return len(line) - len(line.lstrip())


def unpinned_actions(text):
    """(line number, reference) of every `uses:` that is not pinned to a full commit SHA."""
    found = []
    for number, line in enumerate(text.splitlines(), 1):
        match = USES.match(line)
        if not match:
            continue
        reference = match.group(1).strip("'\"")
        if reference.startswith("./"):
            continue
        _, _, version = reference.partition("@")
        if not FULL_SHA.match(version):
            found.append((number, reference))
    return found


def permissions_problems(text):
    """Problems with the top-level `permissions:` block, which must be exactly `contents: read`."""
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if not line.startswith("permissions:"):
            continue
        inline = line[len("permissions:"):].split("#")[0].strip()
        if inline:
            return [f"top-level permissions must be exactly `contents: read`, found {inline!r}"]
        entries = {}
        for block_line in lines[index + 1:]:
            if block_line.strip() and not block_line.startswith((" ", "\t")):
                break
            entry = block_line.split("#")[0].strip()
            if entry:
                key, _, value = entry.partition(":")
                entries[key.strip()] = value.strip()
        if entries == {"contents": "read"}:
            return []
        return [f"top-level permissions must be exactly `contents: read`, found {entries}"]
    return ["no top-level permissions block"]


def _step_bounds(lines, index):
    """Half-open line range of the step (list item) that holds the line at `index`."""
    start_match = STEP_START.match(lines[index])
    if start_match:
        start, dash = index, len(start_match.group(1))
    else:
        key_indent = _indent(lines[index])
        start = index - 1
        while start >= 0:
            match = STEP_START.match(lines[start])
            if match and len(match.group(1)) < key_indent:
                break
            start -= 1
        dash = len(STEP_START.match(lines[start]).group(1)) if start >= 0 else -1
    end = start + 1
    while end < len(lines) and (not lines[end].strip() or _indent(lines[end]) > dash):
        end += 1
    return start, end


def java_cache_problems(text):
    """setup-java steps without `cache: maven`, and hand-made caches of ~/.m2."""
    lines = text.splitlines()
    problems = []
    for index, line in enumerate(lines):
        match = USES.match(line)
        if not match:
            continue
        action = match.group(1).partition("@")[0]
        start, end = _step_bounds(lines, index)
        block = lines[start:end]
        if action == "actions/setup-java" and not any(CACHE_KEY_MAVEN.match(each) for each in block):
            problems.append(f"line {index + 1}: setup-java without `cache: maven`")
        elif action.startswith("actions/cache") and any(M2_PATH.match(each) for each in block):
            problems.append(f"line {index + 1}: manual Maven cache (actions/cache on ~/.m2), "
                            "use setup-java `cache: maven`")
    return problems


def ci_trigger_problems(text):
    """Workflows that run required checks must run on pull requests and on pushes to main (not every branch)."""
    has_pull_request = re.search(r"^  pull_request:", text, re.MULTILINE)
    has_push_to_main = re.search(r"^  push:\n    branches: \[ ?main ?\]", text, re.MULTILINE)
    if has_pull_request and has_push_to_main:
        return []
    return ["required-check workflows must run on `pull_request` and on `push` to main"]


def check_workflows(root):
    """Every problem of every workflow under `root`, as printable lines."""
    problems = []
    for path in sorted((root / WORKFLOW_DIR).glob("*.y*ml")):
        text = path.read_text(encoding="utf-8")
        name = path.relative_to(root).as_posix()
        for number, reference in unpinned_actions(text):
            problems.append(f"{name}:{number}: `uses: {reference}` is not pinned to a full commit SHA")
        problems += [f"{name}: {problem}" for problem in permissions_problems(text)]
        problems += [f"{name}: {problem}" for problem in java_cache_problems(text)]
        if path.name in REQUIRED_CHECK_WORKFLOWS:
            problems += [f"{name}: {problem}" for problem in ci_trigger_problems(text)]
    return problems


def main():
    args = sys.argv[1:]
    if len(args) > 1:
        sys.exit(__doc__)
    root = Path(args[0]).resolve() if args else ROOT
    problems = check_workflows(root)
    for problem in problems:
        print(problem)
    if problems:
        sys.exit(1)
    print("every workflow is SHA-pinned, has minimal permissions and caches Maven through setup-java")


if __name__ == "__main__":
    main()
