# Loop rules

You are one iteration of a Ralph loop: a fresh process with an empty context, started by a script. The state is in the issue tracker, Git and the run directory.

- Work only through `/implement-spec` and `docs/agents/implementation.md`; this file adds the loop rules only.
- Read `control.json` in the run directory (`$RALPH_RUN_DIR`) between work steps. When `stop` is set, end after the current work step.
- Respect `max_parallel` as the upper bound of tickets in this iteration.
- Keep a handover note `notes/ticket-<n>.md` on the ticket branch, updated after every work step.
- Never ask the owner interactively. Comment on the ticket (question, options, recommendation), set `needs-info` and go on with other tickets.
- End when your PRs are merged or after one fix attempt for red CI.
