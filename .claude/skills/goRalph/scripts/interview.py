"""Open items of the pre-run interview of a goRalph start, printed as JSON (no model).

Usage (through python3, no executable bit needed):
  python3 interview.py list <spec>              # what the interview has to ask, as JSON
  python3 interview.py attach <spec> <n>...     # add retro tickets <n> to the spec as sub-issues

`list` prints `{"spec", "interview", "needs_info", "retro", "carry_over", "unfinished_run"}`, every ticket as `number`, `title`, `url`:
  needs_info   open sub-issues of the spec labelled `needs-info` (questions the owner has to answer);
  retro        open issues labelled `retro` and `ready-for-human` (proposals the owner accepts or drops), without
               those already listed as needs_info and without pull requests;
  carry_over   open issues labelled `retro` and `ready-for-agent` that have no parent issue yet (a ticket that is a
               sub-issue of any spec, this one or another, cannot get a second parent): retro tickets
               accepted in an earlier interview, which the next run takes. `attach` makes them sub-issues (a ticket it cannot attach is reported on stderr, the others are still attached, exit 1), i.e. frontier
               work, so it belongs to the start of that run, before the new interview.
  unfinished_run  true when the spec has a run directory under RALPH_HOME (default ~/.goRalph) `runs/<spec>-*` without an
               `ended` marker, as loop.sh finds it: the start continues that run instead of beginning a new one, so
               the caller attaches `carry_over` only when this is false (they are taken by the next run, not by a
               run that is continued).
`interview` is true when needs_info or retro is not empty; otherwise the run starts at once.

An accepted retro ticket is no sub-issue of the spec until the next start attaches it; the frontier of the loop
(`issue-status.py --frontier`) reads only the sub-issues, so the starting run cannot take it.

A failing or timed-out tracker call ends it with one message on stderr, exit 1 and nothing on stdout; wrong arguments
give a usage line and exit 2. Every list is read completely (`--paginate`). Environment: RALPH_GH_TIMEOUT_SECONDS, the
limit of one `gh api` call in seconds (an integer of at least 1, default 60, as in loop.sh; anything else ends it with
an `ERROR` line on stderr and exit 2). The tracker call itself is `ghapi.py`, shared with summary.py.
"""
import json
import os
import subprocess
import sys
from pathlib import Path

from ghapi import REPO, SettingError, gh, gh_list, timeout_seconds

USAGE = "usage: interview.py list <spec> | interview.py attach <spec> <n>..."


def brief(issue):
    return {"number": issue["number"], "title": issue["title"], "url": issue["html_url"]}


def is_open_issue(issue):
    return issue["state"] == "open" and "pull_request" not in issue


def collect(spec):
    sub_issues = gh_list(f"{REPO}/issues/{spec}/sub_issues?per_page=100")
    in_spec = {t["number"] for t in sub_issues}
    needs_info = [t for t in sub_issues
                  if t["state"] == "open" and any(label["name"] == "needs-info" for label in t["labels"])]
    asked = {t["number"] for t in needs_info}
    retro = [i for i in gh_list(f"{REPO}/issues?labels=retro,ready-for-human&state=open&per_page=100")
             if is_open_issue(i) and i["number"] not in asked]
    carry_over = [i for i in gh_list(f"{REPO}/issues?labels=retro,ready-for-agent&state=open&per_page=100")
                  if is_open_issue(i) and i["number"] not in in_spec and not i.get("parent_issue_url")]
    return {"spec": spec, "interview": bool(needs_info or retro), "needs_info": [brief(t) for t in needs_info],
            "retro": [brief(i) for i in retro], "carry_over": [brief(i) for i in carry_over],
            "unfinished_run": has_unfinished_run(spec)}


def has_unfinished_run(spec):
    """As in loop.sh: a run directory `runs/<spec>-*` under RALPH_HOME (default ~/.goRalph) without an `ended` marker."""
    home = Path(os.environ.get("RALPH_HOME") or Path.home() / ".goRalph")
    return any(run.is_dir() and not (run / "ended").exists() for run in (home / "runs").glob(f"{spec}-*"))


def attach(spec, numbers):
    """Attaches every ticket it can; returns the numbers that failed, each reported on stderr."""
    failed = []
    for number in numbers:
        try:
            database_id = gh(f"{REPO}/issues/{number}")["id"]
            gh("-X", "POST", f"{REPO}/issues/{spec}/sub_issues", "-F", f"sub_issue_id={database_id}")
        except (RuntimeError, subprocess.SubprocessError, OSError, ValueError, KeyError) as error:
            print(f"interview.py: could not attach #{number}: {error}", file=sys.stderr)
            failed.append(number)
    return failed


def main(argv):
    numbers = argv[2:]
    if (len(argv) < 3 or argv[1] not in ("list", "attach") or not all(n.isdigit() for n in numbers)
            or (argv[1] == "list" and len(numbers) != 1) or (argv[1] == "attach" and len(numbers) < 2)):
        print(USAGE, file=sys.stderr)
        return 2
    try:
        timeout_seconds()
        if argv[1] == "list":
            print(json.dumps(collect(int(numbers[0])), indent=2))
        else:
            if attach(int(numbers[0]), numbers[1:]):
                return 1
    except SettingError as error:
        print(f"ERROR {error}", file=sys.stderr)
        return 2
    except (RuntimeError, subprocess.SubprocessError, OSError, ValueError, KeyError) as error:
        print(f"interview.py: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
