"""The one `gh api` call of interview.py and summary.py (imported from the same directory, no model).

Environment: RALPH_GH_TIMEOUT_SECONDS, the limit of one call in seconds: an integer of at least 1, default 60, as in
loop.sh; RALPH_GH_MAX_PAGES, the page limit of one list: an integer of at least 1, default 100. Anything else is a
`SettingError` (the scripts print it as an `ERROR` line and exit 2).
"""
import json
import os
import re
import subprocess

REPO = "repos/marmer/hamcrest-matcher-generator"
PER_PAGE = 100


class SettingError(Exception):
    """An environment value the script cannot read."""


def timeout_seconds():
    value = os.environ.get("RALPH_GH_TIMEOUT_SECONDS", "60")
    if not re.fullmatch(r"[1-9][0-9]*", value):
        raise SettingError(f"RALPH_GH_TIMEOUT_SECONDS needs a positive integer, got '{value}'")
    return int(value)


def max_pages():
    value = os.environ.get("RALPH_GH_MAX_PAGES", "100")
    if not re.fullmatch(r"[1-9][0-9]*", value):
        raise SettingError(f"RALPH_GH_MAX_PAGES needs a positive integer, got '{value}'")
    return int(value)


def gh(*args):
    """The parsed answer of `gh api <args>`, None for an empty one; a failing call is a RuntimeError, a timeout a
    subprocess.TimeoutExpired."""
    done = subprocess.run(["gh", "api", *args], capture_output=True, text=True, timeout=timeout_seconds())
    if done.returncode != 0:
        raise RuntimeError(f"gh api {' '.join(args)} failed: {done.stderr.strip()}")
    return json.loads(done.stdout) if done.stdout.strip() else None


def gh_list(path):
    """All items of a list endpoint whose path has a query with `per_page=100`: pages `page=1, 2, ...` are read one
    by one (not with `--paginate`: GitHub's Link header leads to `repositories/{id}/...` paths, which the cloud proxy
    refuses with 403) up to the first page with fewer than 100 items; a list still full at the page limit is a
    RuntimeError, never a truncated one."""
    items = []
    for page in range(1, max_pages() + 1):
        batch = gh(f"{path}&page={page}") or []
        items.extend(batch)
        if len(batch) < PER_PAGE:
            return items
    raise RuntimeError(f"{path} has more than {max_pages()} pages of {PER_PAGE} items (RALPH_GH_MAX_PAGES)")
