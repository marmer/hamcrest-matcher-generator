#!/usr/bin/env python3
"""Fail when a status check the branch ruleset requires is not the name of a CI job.

A required context without a job blocks every merge (a removed `Verify JDK 11` once did for hours).

Usage (the JSON is the response of the branch rules API, the job names come from .github/workflows/):
  gh api repos/<owner>/<repo>/rules/branches/development | scripts/ci/check-ruleset-job-names.py
  scripts/ci/check-ruleset-job-names.py <rules.json> [<root-dir>]
"""
import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def required_contexts(rules):
    """Status check contexts that any `required_status_checks` rule of the API response requires."""
    return sorted({
        check["context"]
        for rule in rules
        if rule.get("type") == "required_status_checks"
        for check in rule.get("parameters", {}).get("required_status_checks", [])
    })


def missing_jobs(rules, job_names):
    """Required contexts that are not the name of a job."""
    return [context for context in required_contexts(rules) if context not in job_names]


def load_job_names(root):
    """Reuse the job name expansion (incl. matrix roles) of check-domain-docs-sync.py."""
    path = ROOT / "scripts" / "agents" / "check-domain-docs-sync.py"
    spec = importlib.util.spec_from_file_location("check_domain_docs_sync", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.workflow_job_names(root)


def main():
    args = sys.argv[1:]
    if len(args) > 2:
        sys.exit(__doc__)
    rules = json.loads(Path(args[0]).read_text(encoding="utf-8") if args else sys.stdin.read())
    root = Path(args[1]).resolve() if len(args) == 2 else ROOT
    missing = missing_jobs(rules, load_job_names(root))
    for context in missing:
        print(f"ruleset requires status check '{context}', which is not a job name in .github/workflows/")
    if missing:
        sys.exit(1)
    print("every required status check of the ruleset is a CI job name")


if __name__ == "__main__":
    main()
