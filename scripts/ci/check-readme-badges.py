#!/usr/bin/env python3
"""Fail when a badge in README.md does not load or says "not found".

SonarCloud answers HTTP 200 with "Project not found" for a wrong project key, so the body is checked too.

Every Markdown image `![alt](url)` of README.md is a badge. Each URL is fetched with a 20 second timeout, redirects are
followed, and the run fails when the status is 400 or more, the body contains "not found" (case-insensitive) or the
badge cannot be fetched at all. One line per badge is printed; a broken badge is an `::error::` annotation.
Stdlib only. Run weekly by the `Check the README badges` job of .github/workflows/external-links.yml.

Usage:
  scripts/ci/check-readme-badges.py [<README.md>]
"""
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
README = ROOT / "README.md"
TIMEOUT_SECONDS = 20
USER_AGENT = "hamcrest-matcher-generator-badge-check (+https://github.com/marmer/hamcrest-matcher-generator)"
NOT_FOUND = "not found"
BADGE_IMAGE = re.compile(r"!\[[^\]]*\]\(([^)\s]+)\)")


def badge_urls(text):
    """The image URLs of the Markdown text, in order: every badge of the README, linked or not."""
    return BADGE_IMAGE.findall(text)


def badge_problem(status, body):
    """Why the response to a badge is broken, or None when it is fine."""
    if status >= 400:
        return f"HTTP {status}"
    if NOT_FOUND in body.decode("utf-8", "replace").lower():
        return 'the badge says "not found" (for SonarCloud: the project key is wrong)'
    return None


def check_badge(url, opener=urllib.request.urlopen):
    """Fetch one badge and return its problem, or None when it is fine."""
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with opener(request, timeout=TIMEOUT_SECONDS) as response:
            status, body = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, body = error.code, error.read()
    except (OSError, ValueError) as error:
        return f"could not fetch the badge ({type(error).__name__})"
    return badge_problem(status, body)


def main(readme=README, opener=urllib.request.urlopen, out=sys.stdout):
    urls = badge_urls(Path(readme).read_text(encoding="utf-8"))
    if not urls:
        print(f"::error::no badge found in {readme}", file=out)
        return 1
    broken = 0
    for url in urls:
        problem = check_badge(url, opener)
        if problem is None:
            print(f"ok {url}", file=out)
        else:
            broken += 1
            print(f"::error::badge {url} is broken: {problem}", file=out)
    return 1 if broken else 0


if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:2]))
