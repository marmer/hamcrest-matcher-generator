#!/usr/bin/env python3
"""List the open SonarCloud issues of a pull request or branch (public API, no token).

The Sonar comment on a PR only counts new issues; this prints each one: rule, severity, file,
line and message, and the total last.

Usage:
  scripts/agents/sonar-issues.py --pr <number>
  scripts/agents/sonar-issues.py --branch <name>
  scripts/agents/sonar-issues.py              # branch main (SonarCloud's main branch master is never analysed)

An unknown pull request or branch is an error naming the known ones: SonarCloud answers it with 0 issues.
"""
import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

PROJECT = "io.github.marmer.testutils:matcher-generator-parent"
API = "https://sonarcloud.io/api/issues/search"
PROJECT_PULL_REQUESTS_API = "https://sonarcloud.io/api/project_pull_requests/list"
PROJECT_BRANCHES_API = "https://sonarcloud.io/api/project_branches/list"
DEFAULT_BRANCH = "main"
PAGE_SIZE = 500


def issues_url(pull_request, branch, page):
    params = {"componentKeys": PROJECT, "resolved": "false", "ps": PAGE_SIZE, "p": page}
    if pull_request:
        params["pullRequest"] = pull_request
    if branch:
        params["branch"] = branch
    return f"{API}?{urllib.parse.urlencode(params)}"


def get_json(url):
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            return json.load(response)
    except (urllib.error.URLError, TimeoutError) as error:
        sys.exit(f"sonar-issues: {url} failed: {error}")


def check_target(fetch, pull_request, branch):
    """Exits with the known pull requests or branches when the target is none of them."""
    if pull_request:
        url, kind, target = PROJECT_PULL_REQUESTS_API, "pull request", pull_request
        known = [item["key"] for item in fetch(f"{url}?{urllib.parse.urlencode({'project': PROJECT})}")["pullRequests"]]
    else:
        url, kind, target = PROJECT_BRANCHES_API, "branch", branch
        known = [item["name"] for item in fetch(f"{url}?{urllib.parse.urlencode({'project': PROJECT})}")["branches"]]
    if target not in known:
        sys.exit(f"sonar-issues: unknown {kind} {target}; known: {', '.join(known) or 'none'}")


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
    target.add_argument("--branch", help=f"branch name (default: {DEFAULT_BRANCH}; the SonarCloud main branch master is never analysed)")
    args = parser.parse_args(argv)
    if not args.pr and not args.branch:
        args.branch = DEFAULT_BRANCH
    return args


def main(argv=None):
    args = parse_args(argv)
    check_target(get_json, args.pr, args.branch)
    print("\n".join(report(fetch_all(get_json, args.pr, args.branch))))


if __name__ == "__main__":
    main()
