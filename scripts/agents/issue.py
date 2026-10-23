#!/usr/bin/env python3
"""Issue write operations through the GitHub REST API (via `gh api`).

`gh issue ...` uses GraphQL, which is blocked in Claude Code cloud sessions. These subcommands
use REST instead, like scripts/agents/issue-status.py does.

Usage:
  scripts/agents/issue.py claim <n> [--login L]          # assign to L (default: the gh user)
  scripts/agents/issue.py comment <n> --body-file F      # post F as a comment
  scripts/agents/issue.py close <n> [--reason completed|not_planned]
"""
import argparse
import json
import subprocess
import sys

REPO = "repos/marmer/hamcrest-matcher-generator"


def gh(*args, check=True):
    out = subprocess.run(["gh", "api", *args], capture_output=True, text=True, timeout=60)
    if check and out.returncode != 0:
        sys.exit(f"gh api {' '.join(args)} failed: {out.stderr.strip() or out.stdout.strip()}")
    return json.loads(out.stdout) if out.stdout.strip() else None


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

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
