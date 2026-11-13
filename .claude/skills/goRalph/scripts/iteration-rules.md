# Loop rules

You are one iteration of a Ralph loop: a fresh process with an empty context, started by a script. The state is in the issue tracker, Git and the run directory.

- Read `docs/agents/goRalph-learnings.md` before you start: it holds the loop's lessons so far.
- Work only through `/implement-spec` and `docs/agents/implementation.md`; this file adds the loop rules only.
- Read `control.json` in the run directory (`$RALPH_RUN_DIR`) between work steps. When `stop` is `after-step` or `true`, end after the current work step; when it is `after-iteration`, finish this iteration as usual (the script does not start another); `now` is handled by the script, which kills you.
- Respect `max_parallel` as the upper bound of tickets in this iteration.
- Keep a handover note `notes/ticket-<n>.md` on the ticket branch, updated after every work step.
- Before you end, write `$RALPH_RUN_DIR/iteration-$RALPH_ITERATION.json` (the script sets `RALPH_ITERATION`) as `{"tickets": [<numbers>], "prs": [<numbers>]}`: the tickets you worked on and the PRs you opened in this iteration, empty lists when there were none. The script prints them on the iteration's end line.
- Never ask the owner interactively. Comment on the ticket with the question, the options and your recommendation, set `needs-info` and go on with other tickets.
- Open the PR of each ticket with squash auto-merge. Wait for CI once, with one blocking poll of at most 30 min that spends no tokens while it blocks.
- On red CI make one fix, then end; whatever is left (second red run, merge conflict, timeout) goes into the handover note for the next iteration.
- End when your PRs are merged or after that one CI fix.
- The escalation path after failed attempts (you run on `sonnet`, no `opus` in the loop) is the rule "Escalation in the `/goRalph` loop" in `docs/agents/implementation.md`.
