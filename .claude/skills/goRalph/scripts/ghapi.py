"""The one `gh api` call of interview.py and summary.py (imported from the same directory, no model).

Environment: RALPH_GH_TIMEOUT_SECONDS, the limit of one call in seconds: an integer of at least 1, default 60, as in
loop.sh; anything else is a `SettingError` (the scripts print it as an `ERROR` line and exit 2).
"""
import json
import os
import re
import subprocess

REPO = "repos/marmer/hamcrest-matcher-generator"


class SettingError(Exception):
    """An environment value the script cannot read."""


def timeout_seconds():
    value = os.environ.get("RALPH_GH_TIMEOUT_SECONDS", "60")
    if not re.fullmatch(r"[1-9][0-9]*", value):
        raise SettingError(f"RALPH_GH_TIMEOUT_SECONDS needs a positive integer, got '{value}'")
    return int(value)


def gh(*args):
    """The parsed answer of `gh api <args>`, None for an empty one; a failing call is a RuntimeError, a timeout a
    subprocess.TimeoutExpired."""
    done = subprocess.run(["gh", "api", *args], capture_output=True, text=True, timeout=timeout_seconds())
    if done.returncode != 0:
        raise RuntimeError(f"gh api {' '.join(args)} failed: {done.stderr.strip()}")
    return json.loads(done.stdout) if done.stdout.strip() else None


def gh_list(path):
    """All items of a list endpoint: `--slurp` wraps the pages of `--paginate` in one array, flattened here."""
    pages = gh("--paginate", "--slurp", path) or []
    return [item for page in pages for item in page]
