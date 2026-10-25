#!/usr/bin/env python3
"""Issue write operations through the GitHub REST API (via `gh api`).

`gh issue ...` uses GraphQL, which is blocked in Claude Code cloud sessions. These subcommands
use REST instead, like scripts/agents/issue-status.py does.

Usage:
  scripts/agents/issue.py claim <n> [--login L]          # assign to L (default: the gh user)
  scripts/agents/issue.py comment <n> --body-file F      # post F as a comment
  scripts/agents/issue.py close <n> [--reason completed|not_planned]
  scripts/agents/issue.py decide <map> <n> --gist "..."  # append a decision line to the map
  scripts/agents/issue.py amend <map> <n> --gist "..."   # replace the existing decision line for <n>
  scripts/agents/issue.py scope <map> <n> --gist "..."   # append an out-of-scope line to the map
  scripts/agents/issue.py fog <map> [--remove TEXT] [--add TEXT]  # edit the Not-yet-specified bullets
  scripts/agents/issue.py note <map> [--remove TEXT] [--add TEXT]  # edit the Notes bullets
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
OUT_OF_SCOPE = "## Out of scope"
FOG = "## Not yet specified"
NOTES = "## Notes"
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


def _split_section(body, heading):
    """Return (newline, lines, start, end, section) for `heading`; raise DecisionError if missing."""
    newline = "\r\n" if "\r\n" in body else "\n"
    lines = body.split(newline)
    start = next((i for i, line in enumerate(lines) if line.strip() == heading), None)
    if start is None:
        raise DecisionError(f"no '{heading}' section in the map body; nothing changed")
    # a section ends at the next heading or at the `---` rule before the attribution footer
    end = next((i for i in range(start + 1, len(lines))
                if lines[i].startswith("## ") or lines[i].strip() == "---"), len(lines))
    return newline, lines, start, end, lines[start + 1:end]


def _join_section(body, newline, lines, start, end, section):
    while section and not section[-1].strip():
        section.pop()
    if not section:
        section = [""]
    rest = lines[end:]
    new_lines = lines[: start + 1] + section
    if rest:
        new_lines += ["", *rest]
    elif body.endswith(newline):
        new_lines.append("")
    return newline.join(new_lines)


# a bullet that starts with a link to the ticket; the title is escape-aware (`\[`, `\]`) and ends at its own `](`,
# so an inline link in the gist does not match
def _linked_bullet_pattern(ticket):
    return re.compile(rf"^\s*- \[(?:\\.|[^\]\\])*\]\({re.escape(REPO_URL)}/{ticket}\)", re.MULTILINE)


def _linked_line(ticket, title, gist):
    safe_title = title.replace("[", "\\[").replace("]", "\\]")
    return f"- [{safe_title}]({REPO_URL}/{ticket}): {gist}"


def add_linked_bullet(body, heading, ticket, title, gist):
    """Return `body` with `- [title](link): gist` appended as the last item of the `heading` section.

    Pure text manipulation, no API calls. Raises DecisionError (body unchanged) when the section is
    missing or a bullet of the section already starts with a link to the ticket. Inline links to the
    ticket inside other bullets' gists do not count.
    """
    newline, lines, start, end, section = _split_section(body, heading)
    if _linked_bullet_pattern(ticket).search("\n".join(section)):
        raise DecisionError(f"#{ticket} is already linked in '{heading}'; nothing changed")
    while section and not section[-1].strip():
        section.pop()
    section.append(_linked_line(ticket, title, gist))
    return _join_section(body, newline, lines, start, end, section)


def add_decision(body, ticket, title, gist):
    """Append a decision bullet to the Decisions-so-far section (see add_linked_bullet)."""
    return add_linked_bullet(body, DECISIONS, ticket, title, gist)


def replace_linked_bullet(body, heading, ticket, title, gist):
    """Return `body` with the bullet of `heading` that links `ticket` rewritten to the freshly built line.

    Pure text manipulation, no API calls. Raises DecisionError (body unchanged) when the section is
    missing, has no bullet linking the ticket, or has more than one. Indented continuation lines of
    the replaced bullet are replaced with it.
    """
    newline, lines, start, end, section = _split_section(body, heading)
    pattern = _linked_bullet_pattern(ticket)
    hits = [i for i, line in enumerate(section) if pattern.match(line)]
    if not hits:
        raise DecisionError(f"no line linking #{ticket} in '{heading}'; nothing changed")
    if len(hits) > 1:
        raise DecisionError(f"{len(hits)} lines link #{ticket} in '{heading}'; nothing changed")
    stop = hits[0] + 1
    while stop < len(section) and section[stop].startswith("  "):
        stop += 1
    section[hits[0]:stop] = [_linked_line(ticket, title, gist)]
    return _join_section(body, newline, lines, start, end, section)


def amend_decision(body, ticket, title, gist):
    """Replace the existing decision bullet for `ticket` in the Decisions-so-far section."""
    return replace_linked_bullet(body, DECISIONS, ticket, title, gist)


def edit_bullets(body, heading, remove=None, add=None):
    """Return `body` with one top-level bullet of `heading` removed and/or one appended.

    `remove` must match exactly one top-level bullet (substring match); its indented sub-bullets go with it.
    `add` is the new bullet text without the leading "- ". Raises DecisionError (body unchanged) otherwise.
    """
    newline, lines, start, end, section = _split_section(body, heading)
    if remove:
        hits = [i for i, line in enumerate(section) if line.startswith("- ") and remove in line]
        if len(hits) != 1:
            raise DecisionError(f"'{remove}' matches {len(hits)} bullets in '{heading}'; nothing changed")
        stop = hits[0] + 1
        while stop < len(section) and section[stop].startswith("  "):
            stop += 1
        del section[hits[0]:stop]
    if add:
        while section and not section[-1].strip():
            section.pop()
        section.append(f"- {add}")
    return _join_section(body, newline, lines, start, end, section)


def edit_fog(body, remove=None, add=None):
    return edit_bullets(body, FOG, remove, add)


def edit_notes(body, remove=None, add=None):
    return edit_bullets(body, NOTES, remove, add)


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


def amend(args):
    map_issue = gh(f"{REPO}/issues/{args.map}")
    title = gh(f"{REPO}/issues/{args.ticket}")["title"]
    new_body = amend_decision(map_issue["body"] or "", args.ticket, title, args.gist)
    gh("-X", "PATCH", f"{REPO}/issues/{args.map}", "-f", f"body={new_body}")
    print(f"Amended decision for #{args.ticket} in #{args.map}")
    refresh_status(args.map)


def scope(args):
    map_issue = gh(f"{REPO}/issues/{args.map}")
    title = gh(f"{REPO}/issues/{args.ticket}")["title"]
    new_body = add_linked_bullet(map_issue["body"] or "", OUT_OF_SCOPE, args.ticket, title, args.gist)
    gh("-X", "PATCH", f"{REPO}/issues/{args.map}", "-f", f"body={new_body}")
    print(f"Recorded #{args.ticket} as out of scope in #{args.map}")


def fog(args):
    if not (args.remove or args.add):
        sys.exit("fog needs --remove and/or --add")
    map_issue = gh(f"{REPO}/issues/{args.map}")
    new_body = edit_fog(map_issue["body"] or "", args.remove, args.add)
    gh("-X", "PATCH", f"{REPO}/issues/{args.map}", "-f", f"body={new_body}")
    print(f"Updated '{FOG}' in #{args.map}")


def note(args):
    if not (args.remove or args.add):
        sys.exit("note needs --remove and/or --add")
    map_issue = gh(f"{REPO}/issues/{args.map}")
    new_body = edit_notes(map_issue["body"] or "", args.remove, args.add)
    gh("-X", "PATCH", f"{REPO}/issues/{args.map}", "-f", f"body={new_body}")
    print(f"Updated '{NOTES}' in #{args.map}")


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

    p = sub.add_parser("amend", help="replace the existing decision line for a ticket in the map")
    p.add_argument("map", type=int, help="map issue number")
    p.add_argument("ticket", type=int, help="resolved ticket number whose decision line to replace")
    p.add_argument("--gist", required=True, help="one-line summary of the decision")
    p.set_defaults(func=amend)

    p = sub.add_parser("scope", help="append an out-of-scope line to the map's Out-of-scope section")
    p.add_argument("map", type=int, help="map issue number")
    p.add_argument("ticket", type=int, help="ticket to link (the ruled-out ticket, or the one that ruled it out)")
    p.add_argument("--gist", required=True, help="one-line summary plus why it is out of scope")
    p.set_defaults(func=scope)

    p = sub.add_parser("fog", help="remove and/or add a bullet in the map's Not-yet-specified section")
    p.add_argument("map", type=int, help="map issue number")
    p.add_argument("--remove", metavar="TEXT", help="remove the one bullet containing TEXT")
    p.add_argument("--add", metavar="TEXT", help="append a bullet with TEXT")
    p.set_defaults(func=fog)

    p = sub.add_parser("note", help="remove and/or add a bullet in the map's Notes section")
    p.add_argument("map", type=int, help="map issue number")
    p.add_argument("--remove", metavar="TEXT", help="remove the one top-level bullet containing TEXT, with its sub-bullets")
    p.add_argument("--add", metavar="TEXT", help="append a bullet with TEXT")
    p.set_defaults(func=note)

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
