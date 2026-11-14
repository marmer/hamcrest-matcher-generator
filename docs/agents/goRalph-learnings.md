# goRalph learnings

Every goRalph iteration reads this file before it starts working (`iteration-rules.md` points here). It holds lessons for the loop only; lessons for every implementation belong in `implementer-brief.md`.

Limit: 40 lines (`LearningsFileTest` in `scripts/agents/test_go_ralph_loop.py` pins it). When the file is full, the retro condenses it instead of appending: general lessons move to `implementer-brief.md`, obsolete ones are deleted, and if it still does not fit, a retro ticket proposes raising the limit or restructuring. Only the retro iteration edits this file, through its own reviewed PR.

## Lessons

- Done means the PR is squash-merged, not that the implementer reported: open it with auto-merge, wait for CI once, then end.
- Record every escalation as a ticket comment (from which model to which, and why); the retro reads these comments.
- Never call the squash-merge API yourself, even when CI looks flaky: a failed check means fix or report on the ticket, not merge (run 443 merged #447 with a red check).
- Tests of scripts: build the fake PATH from the needed tools only, wait with poll-and-deadline helpers (never fixed sleeps), and give every failure line an assertion message, never a bare timeout.
- Before implementing a ticket, check for an existing remote branch `ticket-<n>` and an open PR (`git ls-remote --heads origin ticket-<n>`). If one exists, continue on it (fetch, check CI, enable auto-merge) instead of implementing again: a second push is rejected and the work is lost (run 443).
- Headless `-p` kills background work 10 minutes after your last turn unless `CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS` says otherwise; loop.sh sets one hour (run `104-20261114-131620`, iteration 1: two implementers stopped, branches pushed without PRs). Never end a turn with implementers running; wait for their completion notifications, and never poll with `until ... sleep` or `while true` loops, `timeout ... bash -c` wrappers or the Monitor tool (all denied; run 104, iterations 1, 2 and 6). The loop's `check_prs` picks up a PR still waiting for CI after the iteration ends.
