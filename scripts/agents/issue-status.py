#!/usr/bin/env python3
"""Render the ticket status of a parent issue (wayfinder map or spec) as a Mermaid graph.

Reads the parent's sub-issues and their blocked-by dependencies through the GitHub REST API
(via `gh api`, which also works where GraphQL is blocked) and replaces the status section of
the parent's body, so the current state is visible on the issue itself.

Usage:
  scripts/agents/issue-status.py <parent-number>           # update the parent's body
  scripts/agents/issue-status.py <parent-number> --print   # print the section only
  scripts/agents/issue-status.py <parent-number> --json    # print the raw status data
  scripts/agents/issue-status.py <parent-number> --frontier  # print the ready tickets, those labelled needs-info or ready-for-human on a "waiting for the owner" line (no map write)

Besides --json and --frontier, every run warns on stderr about open tickets that do not lead to the
final ticket (see loose_tickets), so a ticket added after the blocking edges were set cannot go unnoticed.
"""
import json
import subprocess
import sys

REPO = "repos/marmer/hamcrest-matcher-generator"
START = "<!-- status:start -->"
END = "<!-- status:end -->"
STATES = {
    "done": ("Done", "fill:#2da44e,color:#fff,stroke:#1a7f37"),
    "claimed": ("In progress", "fill:#bf8700,color:#fff,stroke:#9a6700"),
    "ready": ("Ready", "fill:#0969da,color:#fff,stroke:#0550ae"),
    "blocked": ("Blocked", "fill:#d0d7de,color:#24292f,stroke:#8c959f"),
}


def gh(*args, body_file=None):
    cmd = ["gh", "api", *args]
    if body_file:
        cmd += ["-F", f"body=@{body_file}"]
    out = subprocess.run(cmd, capture_output=True, text=True, check=True, timeout=60)
    return json.loads(out.stdout) if out.stdout.strip() else None


def collect(parent):
    children = gh(f"{REPO}/issues/{parent}/sub_issues?per_page=100")
    tickets = []
    for child in children:
        blockers = gh(f"{REPO}/issues/{child['number']}/dependencies/blocked_by?per_page=100") or []
        tickets.append({
            "number": child["number"],
            "title": child["title"],
            "closed": child["state"] == "closed",
            "assigned": bool(child.get("assignees")),
            "labels": [label["name"] for label in child.get("labels", [])],
            "blocked_by": [b["number"] for b in blockers],
            "open_blockers": [b["number"] for b in blockers if b["state"] == "open"],
        })
    for ticket in tickets:
        if ticket["closed"]:
            ticket["status"] = "done"
        elif ticket["assigned"]:
            ticket["status"] = "claimed"
        elif ticket["open_blockers"]:
            ticket["status"] = "blocked"
        else:
            ticket["status"] = "ready"
    return tickets


def loose_tickets(tickets):
    """The final ticket and the open tickets that do not lead to it.

    The final ticket is the open ticket that blocks no other open ticket and has the most open
    ancestors (ties: the highest number), i.e. the release. An open ticket that is not one of its
    ancestors gates nothing, so the final ticket could be worked while it is still open.
    """
    open_numbers = {t["number"] for t in tickets if not t["closed"]}
    blockers = {t["number"]: [b for b in t["blocked_by"] if b in open_numbers]
                for t in tickets if not t["closed"]}
    sinks = open_numbers - {b for bs in blockers.values() for b in bs}

    def ancestors(number):
        seen, todo = set(), [number]
        while todo:
            for blocker in blockers[todo.pop()]:
                if blocker not in seen:
                    seen.add(blocker)
                    todo.append(blocker)
        return seen

    if not sinks:
        return None, []
    final = max(sinks, key=lambda n: (len(ancestors(n)), n))
    return final, sorted(open_numbers - ancestors(final) - {final})


def loose_warning(final, loose):
    numbers = ", ".join(f"#{n}" for n in loose)
    return (f"warning: {numbers} do not lead to the final ticket #{final}, which could be worked while they are open. "
            f"Run `scripts/agents/issue.py block {final} --by <n>` for each that belongs before it, "
            f"or move it out of the parent.")


def kind(ticket):
    """The wayfinder:<type> label suffix, else the first label, else None."""
    typed = next((name.split(":", 1)[1] for name in ticket["labels"] if name.startswith("wayfinder:")), None)
    return typed or (ticket["labels"][0] if ticket["labels"] else None)


def frontier_line(ticket):
    suffix = f" ({kind(ticket)})" if kind(ticket) else ""
    return f"#{ticket['number']} {ticket['title']}{suffix}"


OWNER_LABELS = ("needs-info", "ready-for-human")


def frontier_lines(tickets):
    """The ready tickets an agent can start; those labelled for the owner wait on a separate line."""
    ready = [t for t in tickets if t["status"] == "ready"]
    waiting = [t for t in ready if any(name in OWNER_LABELS for name in t["labels"])]
    lines = [frontier_line(t) for t in ready if t not in waiting]
    if waiting:
        lines.append("waiting for the owner: " + ", ".join(frontier_line(t) for t in waiting))
    return lines


def label(ticket, width=48):
    title = ticket["title"] if len(ticket["title"]) <= width else ticket["title"][: width - 1] + "…"
    typed = kind(ticket)
    suffix = f"<br/><i>{typed}</i>" if typed else ""
    # Mermaid entity codes: "#35;" renders "#", "#quot;" renders a double quote.
    return f"#35;{ticket['number']} " + title.replace('"', "#quot;") + suffix


def render(tickets):
    done = sum(t["status"] == "done" for t in tickets)
    counts = ", ".join(f"{STATES[s][0]} {sum(t['status'] == s for t in tickets)}" for s in STATES)
    lines = [START, "## Status", "", f"**{done}/{len(tickets)} done** ({counts}).", "",
             "```mermaid", "flowchart LR"]
    for ticket in tickets:
        lines.append(f'  t{ticket["number"]}["{label(ticket)}"]:::{ticket["status"]}')
    for ticket in tickets:
        for blocker in ticket["blocked_by"]:
            if any(t["number"] == blocker for t in tickets):
                lines.append(f"  t{blocker} --> t{ticket['number']}")
    for state, (_, style) in STATES.items():
        lines.append(f"  classDef {state} {style}")
    lines += ["```", "",
              "_Arrows point from blocker to blocked ticket. Generated by `scripts/agents/issue-status.py`; do not edit by hand._",
              END]
    return "\n".join(lines)


def replace_section(body, section):
    if START in body and END in body:
        return body[: body.index(START)] + section + body[body.index(END) + len(END):]
    # New section goes right after the first H2 block, i.e. before the second "## " heading.
    first = body.find("\n## ", body.find("## ") + 1) if body.startswith("## ") else body.find("\n## ")
    if first == -1:
        return body.rstrip() + "\n\n" + section + "\n"
    return body[: first + 1] + section + "\n\n" + body[first + 1:]


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    parent = sys.argv[1]
    tickets = collect(parent)
    if "--json" in sys.argv:
        print(json.dumps(tickets, indent=2))
        return
    if "--frontier" in sys.argv:
        for line in frontier_lines(tickets):
            print(line)
        return
    final, loose = loose_tickets(tickets)
    if loose:
        print(loose_warning(final, loose), file=sys.stderr)
    section = render(tickets)
    if "--print" in sys.argv:
        print(section)
        return
    body = gh(f"{REPO}/issues/{parent}")["body"] or ""
    path = f"/tmp/issue-status-{parent}.md"
    with open(path, "w") as file:
        file.write(replace_section(body, section))
    gh("-X", "PATCH", f"{REPO}/issues/{parent}", body_file=path)
    print(f"Updated status of #{parent}: {sum(t['status'] == 'done' for t in tickets)}/{len(tickets)} done")


if __name__ == "__main__":
    main()
