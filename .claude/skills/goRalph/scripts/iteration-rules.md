# Loop rules

You are one iteration of a Ralph loop: a fresh process with an empty context, started by a script. The state is in the issue tracker, Git and the run directory.

- Work only through `/implement-spec` and `docs/agents/implementation.md`; this file adds the loop rules only.
- Read `control.json` in the run directory (`$RALPH_RUN_DIR`) between work steps. When `stop` is `after-step` or `true`, end after the current work step; when it is `after-iteration`, finish this iteration as usual (the script does not start another); `now` is handled by the script, which kills you.
- Respect `max_parallel` as the upper bound of tickets in this iteration.
- Keep a handover note `notes/ticket-<n>.md` on the ticket branch, updated after every work step.
- Never ask the owner interactively. Comment on the ticket (question, options, recommendation), set `needs-info` and go on with other tickets.
- Do not wait for CI: end after your PRs are open with auto-merge, or after one fix attempt for red CI. The script waits between iterations and starts a new one for a red CI, a conflict or a review comment.
