# Ticket 451: the frontier skips tickets with an open PR or a remote ticket branch

- `scripts/agents/issue-status.py --frontier`: `in_flight_numbers()` reads the open PRs and `git/matching-refs/heads/ticket-` (page by page); `frontier_lines(tickets, in_flight)` drops those tickets. Only called when a ticket is ready.
- The work check (`check_prs` in `loop.sh`) is unchanged: it already handles open PRs of the spec's tickets (action, or waiting with auto-merge).
- Docs: `docs/agents/ralph-loop.md`, one line in `.claude/skills/goRalph/scripts/iteration-rules.md`.
- Tests: `test_issue_status.py`, `test_go_ralph_loop.py` (STUB_GH_TWO serves a second free ticket #8).
- State: implemented, scripts tests green; full verify pending at time of writing.
