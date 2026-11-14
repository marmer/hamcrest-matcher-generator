#!/usr/bin/env python3
"""The recurring steps of a goRalph iteration, one allowed command each (no shell chains, no `$VAR` expansion).

Usage:
  scripts/agents/ralph-iteration.py control <run-dir>                  # print control.json
  scripts/agents/ralph-iteration.py data <run-dir> <iteration> [--tickets N ...] [--prs N ...]
                                                                       # write iteration-<iteration>.json
  scripts/agents/ralph-iteration.py auto-merge <pr>                    # enable squash auto-merge for a PR

`auto-merge` uses the REST route `PUT /repos/{owner}/{repo}/pulls/{n}/ccr/auto_merge` of Claude Code cloud sessions
(GraphQL is blocked there, and the REST API has no call for it). It never merges a PR itself: when the route fails,
the script prints an ERROR line and exits 1.

Exit codes: 0 done, 1 the `gh` call failed, 2 a value the script cannot use (`ERROR <what is wrong>`).
"""
import argparse
import json
import os
import subprocess
import sys

REPO = "repos/marmer/hamcrest-matcher-generator"
GH_TIMEOUT_SECONDS = 60


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


def control(args):
    path = os.path.join(run_directory(args.run_dir), "control.json")
    if not os.path.isfile(path):
        refuse(f"no control.json in '{args.run_dir}'")
    with open(path, encoding="utf-8") as file:
        sys.stdout.write(file.read())


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
    done = subprocess.run(["gh", "api", "-X", "PUT", f"{REPO}/pulls/{pr}/ccr/auto_merge", "-f", "merge_method=squash"],
                          capture_output=True, text=True, timeout=GH_TIMEOUT_SECONDS)
    if done.returncode != 0:
        print(f"ERROR auto-merge for #{pr} failed: {done.stderr.strip() or done.stdout.strip()}")
        sys.exit(1)
    print(f"auto-merge (squash) enabled for #{pr}")


def parser():
    top = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = top.add_subparsers(dest="command", required=True)
    one = commands.add_parser("control")
    one.add_argument("run_dir")
    one.set_defaults(handler=control)
    two = commands.add_parser("data")
    two.add_argument("run_dir")
    two.add_argument("iteration")
    two.add_argument("--tickets", nargs="*", default=[])
    two.add_argument("--prs", nargs="*", default=[])
    two.set_defaults(handler=data)
    three = commands.add_parser("auto-merge")
    three.add_argument("pr")
    three.set_defaults(handler=auto_merge)
    return top


if __name__ == "__main__":
    arguments = parser().parse_args()
    arguments.handler(arguments)
