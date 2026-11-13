"""Data of the end-of-run summary page of a goRalph run, printed as JSON.

Usage (through python3, no executable bit needed):
  python3 summary.py <run directory>

Reads the run's `started` file, `status.json` and `ended` marker and asks the tracker through `gh api`: PRs merged
since the run started, escalation comments on the spec's tickets, open `needs-info` tickets of the spec and `retro`
tickets created since the run started. It never reads the iteration streams or results. A failing or timed-out tracker call
ends it with one message on stderr, exit 1 and nothing on stdout. Every list is read completely (`--paginate`).
Environment: RALPH_GH_TIMEOUT_SECONDS, the limit of one `gh api` call in seconds (an integer of at least 1, default 60, as in
loop.sh; anything else ends it with an `ERROR` line on stderr and exit 2). The tracker call is `ghapi.py`.

A comment counts as an escalation when it (1) mentions "escalat", (2) names a model transition (`haiku -> sonnet`,
`haiku => sonnet`, a unicode arrow, `haiku to sonnet`) or (3) is a closing line `Model: sonnet|opus ...`, i.e. the
ticket ran above the default model. The rule is loose on purpose: a comment like "no escalation needed" is a known
false positive.
"""
import json
import re
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

from ghapi import REPO, SettingError, gh_list, timeout_seconds
MODELS = "haiku|sonnet|opus"
ESCALATION = re.compile(rf"escalat|\b({MODELS})\s*(->|=>|\u2192|to)\s*({MODELS})\b|\bModel:\s*(sonnet|opus)\b", re.I)


def collect(run_dir):
    run = Path(run_dir)
    started = int(run.joinpath("started").read_text(encoding="utf-8").strip())
    status = json.loads(run.joinpath("status.json").read_text(encoding="utf-8"))
    ended = run / "ended"
    finished = int(ended.stat().st_mtime) if ended.exists() else int(time.time())
    since = datetime.fromtimestamp(started, timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    spec = int(status["spec"])

    pulls = gh_list(f"{REPO}/pulls?state=closed&sort=updated&direction=desc&per_page=100")
    tickets = gh_list(f"{REPO}/issues/{spec}/sub_issues?per_page=100")
    retro = gh_list(f"{REPO}/issues?labels=retro&state=all&since={since}&per_page=100")

    escalations = []
    for ticket in tickets:
        for comment in gh_list(f"{REPO}/issues/{ticket['number']}/comments?since={since}&per_page=100"):
            if comment["created_at"] >= since and ESCALATION.search(comment["body"]):
                escalations.append({"ticket": ticket["number"], "title": ticket["title"],
                                    "comment": comment["body"], "url": comment["html_url"]})

    return {
        "spec": spec,
        "state": status["state"],
        "iterations": status["iteration"],
        "ok": status["ok"],
        "failed": status["failed"],
        "duration_seconds": finished - started,
        "merged_prs": [{"number": p["number"], "title": p["title"], "url": p["html_url"]}
                       for p in pulls if p.get("merged_at") and p["merged_at"] >= since],
        "escalations": escalations,
        "open_questions": [{"number": t["number"], "title": t["title"], "url": t["html_url"]}
                           for t in tickets
                           if t["state"] == "open" and any(label["name"] == "needs-info" for label in t["labels"])],
        "retro_tickets": [{"number": i["number"], "title": i["title"], "url": i["html_url"]}
                          for i in retro if "pull_request" not in i and i["created_at"] >= since],
    }


def main(argv):
    if len(argv) != 2:
        print("usage: summary.py <run directory>", file=sys.stderr)
        return 2
    try:
        timeout_seconds()
        data = collect(argv[1])
    except SettingError as error:
        print(f"ERROR {error}", file=sys.stderr)
        return 2
    except (RuntimeError, subprocess.SubprocessError, OSError, ValueError, KeyError) as error:
        print(f"summary.py: {error}", file=sys.stderr)
        return 1
    print(json.dumps(data, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
