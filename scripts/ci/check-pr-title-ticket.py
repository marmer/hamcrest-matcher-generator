#!/usr/bin/env python3
"""Fail when a pull request title names no ticket, unless the pull request was opened by a bot.

The squash-merged PR title becomes the commit message on main, so it must carry the ticket number that the change
closes: `docs: add the badge (#284)` (GitHub appends ` (#<PR>)` itself). A ticket number is a `#` followed by digits
that is not glued to a word; a title may name several. Pull requests whose author is a bot (release-please, Renovate,
Dependabot, github-actions) are exempt: their titles name no ticket. Run by the `PR title` job of
.github/workflows/pr-title.yml, which passes the title and the author type through the environment, never through a
shell line. Stdlib only.

Environment:
  PR_TITLE        the pull request title (empty when unset)
  PR_AUTHOR_TYPE  `github.event.pull_request.user.type`, `Bot` for bots (anything else counts as a human)

Usage:
  PR_TITLE='docs: add the badge (#284)' PR_AUTHOR_TYPE=User scripts/ci/check-pr-title-ticket.py
"""
import os
import re
import sys

BOT = "Bot"
EXAMPLE = "docs: add the badge (#284)"
TICKET_NUMBER = re.compile(r"(?<![\w&])#\d+\b")


def problem(title, author_type):
    """Why the pull request title is not acceptable, or None when it is fine."""
    if author_type == BOT:
        return None
    if TICKET_NUMBER.search(title):
        return None
    return (f'PR title "{title}" names no ticket number. Put the ticket the change closes in the title, '
            f"e.g. `{EXAMPLE}`. A change without a ticket gets one first (CONTRIBUTING.md).")


def main(environ=os.environ, out=sys.stdout):
    title = environ.get("PR_TITLE", "")
    author_type = environ.get("PR_AUTHOR_TYPE", "")
    found = problem(title, author_type)
    if found is None:
        if author_type == BOT:
            print("ok: bot pull request, no ticket number required", file=out)
        else:
            print("ok: PR title names a ticket number", file=out)
        return 0
    print(f"::error::{found}", file=out)
    return 1


if __name__ == "__main__":
    sys.exit(main())
