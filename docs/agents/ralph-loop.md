# Ralph loop

A script-driven loop for long unattended implementation runs in a cloud session. The main session only starts the script and watches its status; the work happens in fresh `claude -p` processes. Research and measurements: `docs/research/ralph-loop-in-cloud-sessions.md`. This file is being written during the design interview; sections marked *draft* are not settled yet.

## Terms

**Run**:
One start of the loop script until it ends: the frontier is empty, a limit is reached, or a stop was requested.
_Avoid_: session (that is the Claude Code session the main session runs in)

**Iteration** (German: Runde):
One fresh orchestrator process started by the script with the same prompt. It reads the state from the issue tracker, Git and the run directory, does its work and ends; the next iteration starts with an empty context.

**Work step** (German: Arbeitsschritt):
The smallest unit after which the state is saved by commit and push, e.g. "ticket implemented", "review round 2 fixed", "PR opened". A stop `after-step` waits for the current work step to end.

**Control file**:
`control.json` in the run directory. It holds the limits (`until`, `max_iterations`, `max_parallel`, budgets) and the stop request (`now`, `after-step`, `after-iteration`). The script reads it before each iteration, the orchestrator between work steps; changing it changes the running loop.

## Settled decisions (draft)

- The loop works on one spec: its argument is the spec issue number, the frontier comes from `scripts/agents/issue-status.py`. A run ends by itself when the frontier holds only `ready-for-human` or `needs-info` tickets.
- `stop: now` terminates the child process group; the state is the last pushed work step, uncommitted changes are lost (no automatic WIP commit). The gentle default is `after-step`.
- Every implementation is followed by a `code-review`, also when the implementation was itself a review fix; the chain ends with a review without must-fix findings, at the latest after 3 review rounds per ticket; the rest becomes follow-up tickets (`docs/agents/review-briefs.md`). This rule is meant for `docs/agents/implementation.md` as a whole.
- Models: orchestrator `sonnet`; implementer `haiku` for up to 3 attempts, then `sonnet` for 2 attempts, then the ticket gets `ready-for-human` with a comment and the loop goes on with other tickets. No `opus` in the loop.
- `max_parallel` is the upper bound of tickets per iteration, default and maximum 3; the disjoint-files rule of `implementation.md` still applies.
- Self-improvement: a retro may change only lessons in `docs/agents/implementer-brief.md` and the loop's own learnings file without the owner (own PR with review). Everything that changes rules, model defaults, gates or prompts becomes a `ready-for-human` ticket, presented to the owner with options and a recommendation when the loop skill is started; accepted ones are planned then and processed as normal tickets in a later run.
- Budget: 3 USD per iteration, 25 USD per run. On a usage limit (HTTP 429) the script waits for the reset and retries, at most 5 h 15 min, then ends the run cleanly.
