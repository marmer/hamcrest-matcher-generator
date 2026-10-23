#!/usr/bin/env python3
"""Issue write operations through the GitHub REST API (via `gh api`).

`gh issue ...` uses GraphQL, which is blocked in Claude Code cloud sessions. These subcommands
use REST instead, like scripts/agents/issue-status.py does.

Usage:
  scripts/agents/issue.py claim <n> [--login L]          # assign to L (default: the gh user)
  scripts/agents/issue.py comment <n> --body-file F      # post F as a comment
  scripts/agents/issue.py close <n> [--reason completed|not_planned]
  scripts/agents/issue.py decide <map> <n> --gist "..."  # append a decision line to the map
  scripts/agents/issue.py child <map> --title T --type {research,prototype,grilling,task} --body-file F
  scripts/agents/issue.py block <n> --by <m> [--by <m2> ...] [--map <map>]
"""
import argparse
import json
import os
import re
import subprocess
import sys

REPO = "repos/marmer/hamcrest-matcher-generator"
REPO_URL = "https://github.com/marmer/hamcrest-matcher-generator/issues"
DECISIONS = "## Decisions so far"
STATUS_SCRIPT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "issue-status.py")


def gh(*args, check=True):
    out = subprocess.run(["gh", "api", *args], capture_output=True, text=True, timeout=60)
    if check and out.returncode != 0:
        sys.exit(f"gh api {' '.join(args)} failed: {out.stderr.strip() or out.stdout.strip()}")
    return json.loads(out.stdout) if out.stdout.strip() else None


def database_id(number):
    """The numeric database id of an issue (what sub_issues and dependencies take), not its #number."""
    return gh(f"{REPO}/issues/{number}")["id"]


def refresh_status(map_number):
    result = subprocess.run([sys.executable, STATUS_SCRIPT, str(map_number)], timeout=300)
    if result.returncode != 0:
        sys.exit(f"issue-status.py {map_number} failed")


class DecisionError(Exception):
    pass


def add_decision(body, ticket, title, gist):
    """Return `body` with a decision bullet appended as the last item of the Decisions-so-far section.

    Pure text manipulation, no API calls. Raises DecisionError (body unchanged) when the section is
    missing or the section already links the ticket.
    """
    newline = "\r\n" if "\r\n" in body else "\n"
    lines = body.split(newline)
    start = next((i for i, line in enumerate(lines) if line.strip() == DECISIONS), None)
    if start is None:
        raise DecisionError(f"no '{DECISIONS}' section in the map body; nothing changed")
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("## ")), len(lines))
    section = lines[start + 1:end]
    if re.search(rf"\]\({re.escape(REPO_URL)}/{ticket}\)", "\n".join(section)):
        raise DecisionError(f"#{ticket} is already linked in '{DECISIONS}'; nothing changed")

    while section and not section[-1].strip():
        section.pop()
    if not section:
        section = [""]
    safe_title = title.replace("[", "\\[").replace("]", "\\]")
    bullet = f"- [{safe_title}]({REPO_URL}/{ticket}): {gist}"
    rest = lines[end:]
    new_lines = lines[: start + 1] + section + [bullet]
    if rest:
        new_lines += ["", *rest]
    elif body.endswith(newline):
        new_lines.append("")
    return newline.join(new_lines)


def claim(args):
    login = args.login or gh("user")["login"]
    gh("-X", "POST", f"{REPO}/issues/{args.number}/assignees", "-f", f"assignees[]={login}")
    print(f"Assigned #{args.number} to {login}")


def comment(args):
    gh("-X", "POST", f"{REPO}/issues/{args.number}/comments", "-F", f"body=@{args.body_file}")
    print(f"Commented on #{args.number}")


def close(args):
    gh("-X", "PATCH", f"{REPO}/issues/{args.number}", "-f", "state=closed",
       "-f", f"state_reason={args.reason}")
    print(f"Closed #{args.number} ({args.reason})")


def decide(args):
    map_issue = gh(f"{REPO}/issues/{args.map}")
    title = gh(f"{REPO}/issues/{args.ticket}")["title"]
    new_body = add_decision(map_issue["body"] or "", args.ticket, title, args.gist)
    gh("-X", "PATCH", f"{REPO}/issues/{args.map}", "-f", f"body={new_body}")
    print(f"Recorded decision for #{args.ticket} in #{args.map}")


def child(args):
    with open(args.body_file, encoding="utf-8") as file:
        body = file.read()
    marker = f"Part of #{args.map}"
    if not re.search(rf"Part of #{args.map}\b", body):
        body = (body.rstrip() + "\n\n" if body.strip() else "") + marker
    created = gh("-X", "POST", f"{REPO}/issues", "-f", f"title={args.title}", "-f", f"body={body}",
                 "-f", f"labels[]=wayfinder:{args.type}")
    number = created["number"]
    gh("-X", "POST", f"{REPO}/issues/{args.map}/sub_issues", "-F", f"sub_issue_id={database_id(number)}")
    print(number)
    refresh_status(args.map)


def block(args):
    for blocker in args.by:
        gh("-X", "POST", f"{REPO}/issues/{args.number}/dependencies/blocked_by",
           "-F", f"issue_id={database_id(blocker)}")
        print(f"#{args.number} is blocked by #{blocker}")
    if args.map:
        refresh_status(args.map)


def main():
    parser = argparse.ArgumentParser(description="Issue writes via the GitHub REST API.")
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("claim", help="assign an issue to a login")
    p.add_argument("number", type=int)
    p.add_argument("--login", help="assignee login (default: the authenticated gh user)")
    p.set_defaults(func=claim)

    p = sub.add_parser("comment", help="comment on an issue")
    p.add_argument("number", type=int)
    p.add_argument("--body-file", required=True, help="file holding the comment body")
    p.set_defaults(func=comment)

    p = sub.add_parser("close", help="close an issue")
    p.add_argument("number", type=int)
    p.add_argument("--reason", choices=["completed", "not_planned"], default="completed")
    p.set_defaults(func=close)

    p = sub.add_parser("decide", help="append a decision line to the map's Decisions-so-far section")
    p.add_argument("map", type=int, help="map issue number")
    p.add_argument("ticket", type=int, help="resolved ticket number to link")
    p.add_argument("--gist", required=True, help="one-line summary of the decision")
    p.set_defaults(func=decide)

    p = sub.add_parser("child", help="create a wayfinder child ticket as a sub-issue of a map")
    p.add_argument("map", type=int, help="map issue number")
    p.add_argument("--title", required=True, help="ticket title")
    p.add_argument("--type", required=True, choices=["research", "prototype", "grilling", "task"],
                   help="ticket type, applied as the wayfinder:<type> label")
    p.add_argument("--body-file", required=True, help="file holding the ticket body")
    p.set_defaults(func=child)

    p = sub.add_parser("block", help="record that an issue is blocked by other issues")
    p.add_argument("number", type=int, help="the blocked issue")
    p.add_argument("--by", type=int, action="append", required=True, metavar="M",
                   help="blocker issue number (repeatable)")
    p.add_argument("--map", type=int, help="map issue number; refresh its status section afterwards")
    p.set_defaults(func=block)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
