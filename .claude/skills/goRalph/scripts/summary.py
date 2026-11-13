"""Data of the end-of-run summary page of a goRalph run, printed as JSON.

Usage (through python3, no executable bit needed):
  python3 summary.py <run directory>

Reads the run's `started` file, `status.json` and `ended` marker and asks the tracker through `gh api`: PRs merged
since the run started, escalation comments on the spec's tickets, open `needs-info` tickets of the spec and `retro`
tickets created since the run started. It never reads the iteration streams or results. A failing tracker call
ends it with a message on stderr, exit 1 and nothing on stdout.
"""
import json
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

REPO = "repos/marmer/hamcrest-matcher-generator"


def gh(path):
    done = subprocess.run(["gh", "api", path], capture_output=True, text=True, timeout=60)
    if done.returncode != 0:
        raise RuntimeError(f"gh api {path} failed: {done.stderr.strip()}")
    return json.loads(done.stdout) if done.stdout.strip() else []


def collect(run_dir):
    run = Path(run_dir)
    started = int(run.joinpath("started").read_text(encoding="utf-8").strip())
    status = json.loads(run.joinpath("status.json").read_text(encoding="utf-8"))
    ended = run / "ended"
    finished = int(ended.stat().st_mtime) if ended.exists() else int(time.time())
    since = datetime.fromtimestamp(started, timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    spec = int(status["spec"])

    pulls = gh(f"{REPO}/pulls?state=closed&sort=updated&direction=desc&per_page=100")
    tickets = gh(f"{REPO}/issues/{spec}/sub_issues?per_page=100")
    retro = gh(f"{REPO}/issues?labels=retro&state=all&since={since}&per_page=100")

    escalations = []
    for ticket in tickets:
        for comment in gh(f"{REPO}/issues/{ticket['number']}/comments?since={since}&per_page=100"):
            if comment["created_at"] >= since and "escalat" in comment["body"].lower():
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
        data = collect(argv[1])
    except (RuntimeError, OSError, ValueError, KeyError) as error:
        print(f"summary.py: {error}", file=sys.stderr)
        return 1
    print(json.dumps(data, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
