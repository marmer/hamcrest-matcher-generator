# Ralph loop (`goRalph`)

The skill is called `goRalph`. A script-driven loop for long unattended implementation runs in a cloud session. The main session only starts the script and watches its status; the work happens in fresh `claude -p` processes. Research and measurements: `docs/research/ralph-loop-in-cloud-sessions.md`. This file is being written during the design interview; sections marked *draft* are not settled yet.

## Terms

**Run**:
One start of the loop script until it ends: the frontier is empty, a limit is reached, or a stop was requested.
_Avoid_: session (that is the Claude Code session the main session runs in)

**Iteration** (German: Runde):
One fresh orchestrator process started by the script with the same prompt. It reads the state from the issue tracker, Git and the run directory, does its work and ends; the next iteration starts with an empty context.

**Work step** (German: Arbeitsschritt):
The smallest unit after which the state is saved by commit and push, e.g. "ticket implemented", "review round 2 fixed", "PR opened". A stop `after-step` waits for the current work step to end.

**Control file**:
`control.json` in the run directory. It holds the limits (`until`, `max_duration`, `max_iterations`, `max_iteration_duration`, `max_parallel`) and the stop request (`now`, `after-step`, `after-iteration`). The script reads it before each iteration, the orchestrator between work steps; changing it changes the running loop.

## Settled decisions (draft)

- The loop works on one spec: its argument is the spec issue number, the frontier comes from `scripts/agents/issue-status.py`. A run ends by itself when the frontier holds only `ready-for-human` or `needs-info` tickets.
- `stop: now` terminates the child process group; the state is the last pushed work step, uncommitted changes are lost (no automatic WIP commit). The gentle default is `after-step`.
- Every implementation is followed by a `code-review`, also when the implementation was itself a review fix; the chain ends with a review without must-fix findings, at the latest after 3 review rounds per ticket; the rest becomes follow-up tickets (`docs/agents/review-briefs.md`). This rule is meant for `docs/agents/implementation.md` as a whole.
- Models: orchestrator `sonnet`; implementer `haiku` for up to 3 attempts, then `sonnet` for 2 attempts, then the ticket gets `ready-for-human` with a comment and the loop goes on with other tickets. No `opus` in the loop.
- `max_parallel` is the upper bound of tickets per iteration, default and maximum 3; the disjoint-files rule of `implementation.md` still applies.
- Self-improvement: a retro may change only lessons in `docs/agents/implementer-brief.md` and the loop's own learnings file without the owner (own PR with review). Everything that changes rules, model defaults, gates or prompts becomes a `ready-for-human` ticket, presented to the owner with options and a recommendation when the loop skill is started; accepted ones are planned then and processed as normal tickets in a later run.
- No cost budget and, by default, no limit on how long a run or an iteration lasts. The owner may set an end time (`until`), a run duration (`max_duration`), a number of iterations (`max_iterations`) and a duration per iteration (`max_iteration_duration`, e.g. 120 min). A limit that is reached acts as a stop request of the kind set in the control file, `after-step` unless stated otherwise. On a usage limit (HTTP 429) the script waits for the reset and retries, at most 5 h 15 min, then ends the run cleanly.
- PRs: an iteration opens the PRs of its tickets with auto-merge and waits for CI with one blocking poll (no tokens while blocked), at most 30 min. On red CI it fixes once itself while its context is fresh; anything left (second red run, merge conflict, timeout) goes to the next iteration through the ticket's handover note `notes/ticket-<n>.md` on the ticket branch, updated after every work step. The iteration ends when its PRs are merged or after that one fix attempt.
- Questions for the owner: the orchestrator comments on the ticket (question, options, recommendation), sets `needs-info` and goes on with other tickets. The ticket comment is the source of truth; the owner can answer in any session. When `goRalph` starts, the main session presents all open `needs-info` questions of the spec together with the retro tickets in one interview round, records the answers on the tickets and sets `ready-for-agent`.
- Parallelism: `docs/agents/implementation.md` allows 3 parallel tickets in general, not only in the loop; the disjoint-files rule stays.
- Retro tickets carry `retro` and `ready-for-human`. At `goRalph` start the owner decides per ticket: do or drop. Accepted ones are planned right away (acceptance criteria, model line, `ready-for-agent`) and taken by the next run, not the one starting now.
- Child permissions: an explicit `--allowedTools` list (git, `./mvnw`, `gh api`, `python3 scripts/`, Read, Edit, Write, Agent, Skill) plus `permissions.deny` in `--settings` (force push, push to `main`, `reset --hard`, `branch -D`); denials from `permission_denials` become a monitor line. No `bypassPermissions`. Whether `permissions.deny` holds in `-p` without hooks is checked first in the trial.
- Main session output: monitor lines only for iteration end (tickets, PRs, cost), errors and denials, stop and `END`; no live status page, `status.json` on request; at run end one summary page as an artifact (merged PRs, escalations, open questions, new retro tickets, cost).
- Safety net: besides the monitor (re-armed every 30 min) the main session keeps a `send_later` check-in every 60 min; it restarts a dead script idempotently when the run neither ended nor was stopped, and reports the restart.
- Retro: only as the last iteration of a run, also after `after-step` and `after-iteration` stops, not after `now`. `/retro` gets a non-interactive mode for this: it writes only the lessons allowed above and files retro tickets instead of asking.
- Iteration prompt: the script calls `claude -p "/implement-spec <spec>"` with `--append-system-prompt` from a fixed `iteration-rules.md` that holds only the loop rules (check the control file between work steps, keep handover notes, respect `max_parallel`, ask through `needs-info`, end after the PRs). `/implement-spec` with `implementation.md` stays the only description of how a ticket is implemented.
- Run state lives outside the repo in `~/.goRalph/runs/<spec>-<start time>/` (control, status, per-iteration results, streams, a lock file); one active run per spec, and a new `/goRalph` call for the same spec continues an unfinished run.
- Loop learnings: `docs/agents/goRalph-learnings.md`, changed only through retro PRs, capped at about 40 lines because every iteration reads it; it holds only loop-specific lessons on top of `implementer-brief.md`. Whether 40 lines are enough is open and gets revisited.
- The skill is built in the design session itself, followed by a `code-review` under the recursive review rule; the trial runs first on a small test spec of three documentation-only Haiku tickets, then on a real small spec, and covers `permissions.deny`, the three stop modes, a resume after a deliberately killed script, and a run of about 2 h.
- `until` times are Europe/Berlin unless stated otherwise.
- When the learnings file is full, the retro condenses instead of appending: lessons that hold for every implementation move to `implementer-brief.md`, lessons made obsolete by a rule change are deleted, and if it still does not fit, the retro files a retro ticket to raise the limit or restructure.
- `/goRalph` takes its arguments in plain language (e.g. `/goRalph #312 bis 6 Uhr, max 7 Runden`); the main session turns them into `control.json` and shows the parsed values in one line before starting. Defaults: no time, duration or iteration limit, `max_parallel` 3.
- Before each iteration the script checks through `gh api`, without a model, whether there is work: free frontier tickets or PRs that need action (red CI, conflict, new review comment) start an iteration; only PRs waiting for CI with auto-merge make the script sleep and check again every 5 min; nothing at all ends the run.
- Stop kinds stay three: `after-iteration`, `after-step` (the default, also for a stop request without a kind) and `now`. There is no interrupt-and-save mode.
- The request to run `/retro` at the end of a session is dropped for sessions that started `/goRalph`, because the loop runs its own retro; it stays for sessions without the loop.
