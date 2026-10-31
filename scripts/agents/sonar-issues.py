#!/usr/bin/env python3
"""List the open SonarCloud issues of a pull request or branch (public API, no token).

The Sonar comment on a PR only counts new issues; this prints each one: rule, severity, file,
line and message, and the total last.

Usage:
  scripts/agents/sonar-issues.py --pr <number>
  scripts/agents/sonar-issues.py --branch <name>
  scripts/agents/sonar-issues.py              # the project's default branch
"""
import argparse
import json
import urllib.parse
import urllib.request

PROJECT = "io.github.marmer.testutils:matcher-generator-parent"
API = "https://sonarcloud.io/api/issues/search"
PAGE_SIZE = 500


def issues_url(pull_request, branch, page):
    params = {"componentKeys": PROJECT, "resolved": "false", "ps": PAGE_SIZE, "p": page}
    if pull_request:
        params["pullRequest"] = pull_request
    if branch:
        params["branch"] = branch
    return f"{API}?{urllib.parse.urlencode(params)}"


def get_json(url):
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.load(response)


def fetch_all(fetch, pull_request, branch):
    issues = []
    page = 1
    while True:
        data = fetch(issues_url(pull_request, branch, page))
        issues += data["issues"]
        if not data["issues"] or len(issues) >= data["total"]:
            return issues
        page += 1


def format_issue(issue):
    location = issue["component"].removeprefix(PROJECT + ":")
    if "line" in issue:
        location += f":{issue['line']}"
    return f"{issue['rule']} {issue['severity']} {location} {issue['message']}"


def report(issues):
    return [*map(format_issue, issues), f"{len(issues)} open issue(s)"]


def parse_args(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    target = parser.add_mutually_exclusive_group()
    target.add_argument("--pr", help="pull request number")
    target.add_argument("--branch", help="branch name (default: the project's default branch)")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    print("\n".join(report(fetch_all(get_json, args.pr, args.branch))))


if __name__ == "__main__":
    main()
