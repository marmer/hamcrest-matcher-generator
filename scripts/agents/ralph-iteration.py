#!/usr/bin/env python3
"""The recurring steps of a goRalph iteration, one allowed command each (no shell chains, no `$VAR` expansion).

Usage:
  scripts/agents/ralph-iteration.py data <run-dir> <iteration> [--tickets N ...] [--prs N ...]
                                                                       # write iteration-<iteration>.json
  scripts/agents/ralph-iteration.py auto-merge <pr>                    # enable squash auto-merge for a PR
  scripts/agents/ralph-iteration.py delete-branch <name>               # delete a merged ticket, session-end, retro or
                                                                       # review branch locally and on origin

`auto-merge` uses the REST route `PUT /repos/{owner}/{repo}/pulls/{n}/ccr/auto_merge` of Claude Code cloud sessions
(GraphQL is blocked there, and the REST API has no call for it). It never merges a PR itself: when the route fails,
the script prints an ERROR line and exits 1.

`delete-branch` is the only way a loop child deletes a branch on the remote (the loop denies `git push --delete`, a
`:<branch>` refspec and `git branch -D`, because a deny rule cannot say "only these names"). It refuses every name
but `ticket-<n>`, `session-end-*`, `retro-*` and `review-*` (so `main` never) and every branch without a merged pull
request or with an open one; the branch is deleted on `origin` and locally (a squash merge leaves the local branch
"not fully merged", so the local delete is forced here, after the merged check).

The `gh` call has the timeout of `RALPH_GH_TIMEOUT_SECONDS` (an integer of at least 1, default 60, as in `loop.sh`).

Exit codes: 0 done, 1 the `gh` call failed, timed out or `gh` is missing, 2 a value the script cannot use (`ERROR <what is wrong>`).
"""
import argparse
import json
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".claude", "skills",
                                "goRalph", "scripts"))

import ghapi  # noqa: E402


def refuse(message):
    print(f"ERROR {message}")
    sys.exit(2)


def positive_number(text, what):
    if not text.isascii() or not text.isdigit() or int(text) < 1:
        refuse(f"{what} needs a positive integer, got '{text}'")
    return int(text)


def run_directory(text):
    if not os.path.isdir(text):
        refuse(f"run directory '{text}' does not exist")
    return text


def data(args):
    directory = run_directory(args.run_dir)
    iteration = positive_number(args.iteration, "iteration")
    work = {"tickets": [positive_number(n, "ticket") for n in args.tickets],
            "prs": [positive_number(n, "PR") for n in args.prs]}
    with open(os.path.join(directory, f"iteration-{iteration}.json"), "w", encoding="utf-8") as file:
        json.dump(work, file)
        file.write("\n")


def auto_merge(args):
    pr = positive_number(args.pr, "PR")
    try:
        ghapi.timeout_seconds()
    except ghapi.SettingError as error:
        refuse(str(error))
    try:
        ghapi.gh("-X", "PUT", f"{ghapi.REPO}/pulls/{pr}/ccr/auto_merge", "-f", "merge_method=squash")
    except ValueError:
        pass  # the call succeeded; ghapi.gh could not parse an empty or non-JSON body, which this route may send
    except (RuntimeError, subprocess.TimeoutExpired, OSError) as error:
        print(f"ERROR auto-merge for #{pr} failed: {error}")
        sys.exit(1)
    print(f"auto-merge (squash) enabled for #{pr}")


DELETABLE = re.compile(r"ticket-[0-9]+|(session-end|retro|review)-[A-Za-z0-9][A-Za-z0-9._-]*")


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, timeout=ghapi.timeout_seconds())


def delete_branch(args):
    name = args.name
    if not DELETABLE.fullmatch(name):
        refuse(f"branch '{name}' is not deletable: only ticket-<n>, session-end-*, retro-* and review-* branches are")
    try:
        ghapi.timeout_seconds()
        pulls = ghapi.gh(f"{ghapi.REPO}/pulls?state=all&head={ghapi.REPO.split('/')[1]}:{name}&per_page=100") or []
        if any(pull.get("state") == "open" for pull in pulls):
            print(f"ERROR branch {name} still has an open pull request")
            sys.exit(1)
        if not any(pull.get("merged_at") for pull in pulls):
            print(f"ERROR branch {name} has no merged pull request")
            sys.exit(1)
        local = git("rev-parse", "--verify", "--quiet", f"refs/heads/{name}").returncode == 0
        if local:  # first, so that the checked-out branch stops the deletion before the remote one is touched
            removed = git("branch", "-D", name)
            if removed.returncode != 0:
                print(f"ERROR deleting the local branch {name} failed: {removed.stderr.strip()}")
                sys.exit(1)
        remote = git("ls-remote", "--exit-code", "--heads", "origin", name).returncode == 0
        if remote:
            pushed = git("push", "origin", "--delete", name)
            if pushed.returncode != 0:
                print(f"ERROR deleting {name} on origin failed: {pushed.stderr.strip()}")
                sys.exit(1)
        if not local and not remote:
            print(f"branch {name} is already gone")
            return
    except ghapi.SettingError as error:
        refuse(str(error))
    except (ValueError, RuntimeError, subprocess.TimeoutExpired, OSError) as error:
        print(f"ERROR delete-branch for {name} failed: {error}")
        sys.exit(1)
    print(f"deleted branch {name}")


def parser():
    top = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = top.add_subparsers(dest="command", required=True)
    data_command = commands.add_parser("data")
    data_command.add_argument("run_dir")
    data_command.add_argument("iteration")
    data_command.add_argument("--tickets", nargs="*", default=[])
    data_command.add_argument("--prs", nargs="*", default=[])
    data_command.set_defaults(handler=data)
    auto_merge_command = commands.add_parser("auto-merge")
    auto_merge_command.add_argument("pr")
    auto_merge_command.set_defaults(handler=auto_merge)
    delete_branch_command = commands.add_parser("delete-branch")
    delete_branch_command.add_argument("name")
    delete_branch_command.set_defaults(handler=delete_branch)
    return top


if __name__ == "__main__":
    arguments = parser().parse_args()
    arguments.handler(arguments)
