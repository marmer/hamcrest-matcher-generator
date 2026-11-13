# Ralph loop (`goRalph`)

The skill is called `goRalph`. A script-driven loop for long unattended implementation runs in a cloud session. The main session only starts the script and watches its status; the work happens in fresh `claude -p` processes. Research and measurements: `docs/research/ralph-loop-in-cloud-sessions.md`. Spec #410; what is built (#411, #412, #413, #417, #420) and what is still planned (the open tickets) are separated below.

## Terms

**Run**:
One start of the loop script until it ends: the frontier is empty, a limit is reached, or a stop was requested.
_Avoid_: session (that is the Claude Code session the main session runs in)

**Iteration** (German: Runde):
One fresh orchestrator process started by the script with the same prompt. It reads the state from the issue tracker, Git and the run directory, does its work and ends; the next iteration starts with an empty context.

**Work step** (German: Arbeitsschritt):
The smallest unit after which the state is saved by commit and push, e.g. "ticket implemented", "review round 2 fixed", "PR opened". A stop `after-step` waits for the current work step to end.

**Control file**:
`control.json` in the run directory. It holds the limits (`until`, `max_duration`, `max_iterations`, `max_iteration_duration`, `max_parallel`), the stop kind a reached limit acts as (`limit_stop`, default `after-step`) and the stop request `stop` (`now`, `after-step`, `after-iteration`; `true` means `after-step`). `until` is `YYYY-MM-DD HH:MM`, optionally followed by a zone name (Europe/Berlin otherwise); durations are whole minutes. The script validates every value (`ERROR` line, exit 2) and reads the file before each iteration, a stop `now` and `max_iteration_duration` also while the iteration runs (every 5 s; kill after a grace period of 5 min); the orchestrator reads it between work steps; changing it changes the running loop.

## Monitor lines and status file (reference)

The one place that lists what the script prints and writes; `SKILL.md` and the other docs point here. Stdout carries one line per iteration end, error, denial, stop and run end, nothing else:

| line | meaning |
|---|---|
| `iteration <i> ok: <n> turns, cost $<usd>` | the iteration ended with a result |
| `ERROR iteration <i> failed: <reason>` | a real failure; reason is `no result event (exit <rc>)`, `unreadable result event (exit <rc>)`, `exit <rc>, <subtype>`, `is_error set, <subtype>` or `no result field, <subtype>` (e.g. a turn limit) |
| `ERROR frontier check for #<spec> failed: <stderr>` and `ERROR PR check for #<spec> failed: <stderr>` | the work check failed (never read as green); an `END` line follows |
| `DENIAL iteration <i>: ["<tool>", ...]` | `permission_denials` of the result |
| `STOP iteration <i> killed (<reason>)` | a deliberate stop, not a failure: `stop now`, or `max_iteration_duration <n> reached and grace period over`; it counts in `failed` of the run |
| `END after <i> iterations: <reason> (<ok> ok, <failed> failed)` | the run ended: `stop requested (<kind>)`, `until ... reached`, `max_duration <n> reached`, `max_iterations <n> reached`, `no ticket ready for an agent`, `frontier check failed` or `PR check failed` |

Exit codes: 0 after an `END`; 2 for a value the script cannot read (option, environment variable or a hand edit of the control file: `ERROR <what is wrong>`, the run stays unfinished) and for a missing tool (`jq`, `flock`, `mktemp`, `timeout`); 1 when the lock of the spec is held (`ERROR spec #<n> already has a running loop (pid <pid>), lock <path>`); 143 when the script itself is terminated.

`status.json` in the run directory is rewritten at every state change: `spec` (string), `state`, `iteration` (number of the current or last iteration), `ok`, `failed` (counts of this script start) and `run_dir`. States: `running` (an iteration runs), `waiting` (PRs wait for CI, the script sleeps), `between` (after an iteration, before the next check) and `ended`.

## Built

- The loop works on one spec: its argument is the spec issue number, the frontier comes from `scripts/agents/issue-status.py`. A run ends by itself when the frontier holds only `ready-for-human` or `needs-info` tickets and no PR needs action or waits (#411, #413).
- Run state lives outside the repo in `~/.goRalph/runs/<spec>-<start time>/` (control, status, per-iteration results and streams); one active run per spec through a lock under `~/.goRalph/locks/`, and a new start for the same spec continues an unfinished run (#411).
- `/goRalph` takes its arguments in plain language (e.g. `/goRalph #312 bis 6 Uhr, max 7 Runden`); the main session turns them into options of the script and shows the parsed values in one line before starting. Defaults: no time, duration or iteration limit, `max_parallel` 3 (#412).
- No cost budget and, by default, no limit on how long a run or an iteration lasts. A reached limit acts as a stop request of the kind set in the control file, `after-step` unless stated otherwise; it acts within one poll slice also while the script sleeps for waiting PRs. `until` times are Europe/Berlin unless a zone is named (#412, #413).
- Stop kinds stay three: `after-iteration`, `after-step` (the default, also for a stop request without a kind) and `now`; there is no interrupt-and-save mode. `stop: now` terminates the child process group; the state is the last pushed work step, uncommitted changes are lost (no automatic WIP commit) (#412).
- Iteration prompt: the script calls `claude -p "/implement-spec <spec>"` with `--append-system-prompt` from `scripts/iteration-rules.md`, which holds only the loop rules. `/implement-spec` with `implementation.md` stays the only description of how a ticket is implemented. Review chain, escalation path (orchestrator `sonnet`, no `opus` in the loop) and parallel tickets are rules of `docs/agents/implementation.md`, shared with hand-run sessions; `max_parallel` is the upper bound of tickets per iteration (#411, #417).
- Child permissions: an explicit `--allowedTools` list (git, `./mvnw`, `gh api`, `python3 scripts/`, Read, Edit, Write, Agent, Skill) plus `permissions.deny` in `--settings` (force push, push to `main`, `reset --hard`, `branch -D`) and `disableAllHooks`; denials become a `DENIAL` line. No `bypassPermissions`. That `permissions.deny` holds in `-p` without hooks is not yet verified (trial, #421).
- PRs: an iteration opens the PRs of its tickets with squash auto-merge and waits for CI with one blocking poll (no tokens while blocked), at most 30 min. On red CI it fixes once itself while its context is fresh; anything left (second red run, merge conflict, timeout) goes to the next iteration through the ticket's handover note `notes/ticket-<n>.md` on the ticket branch, updated after every work step. The iteration ends when its PRs are merged or after that one fix attempt (`iteration-rules.md`).
- Work check before each iteration (`loop.sh`, `check_prs`, #413): through `gh api`, without a model, free frontier tickets or PRs that need action (red CI, conflict, new review comment) start an iteration; only PRs waiting for CI with auto-merge make the script sleep and check again every 5 min; nothing at all ends the run. This is a separate mechanism from the iteration's blocking poll: it handles the PRs left over after an iteration ended (timeout, second red run, conflict, new review comment) and costs no tokens.
  - Which PRs: the open ones on branches `ticket-<n>` of the spec's sub-issues. Every list is read with `gh api --paginate --slurp`; every `gh` call has a timeout (`RALPH_GH_TIMEOUT_SECONDS`, default 60; `summary.py` reads the same variable). A failed call, a timeout or an answer `jq` cannot read ends the run with `ERROR PR check ... failed`, never as green.
  - Needs action: a check run failed, the merge state is `dirty`, or a review comment (not a reply in a thread) or a review (changes requested, or with text) is newer than the head commit.
  - Waiting: no action needed, auto-merge is on, and CI is still pending (a check run not completed, or none yet). The pause is `RALPH_PR_SLEEP_SECONDS` (default 300), sliced by `RALPH_POLL_SECONDS` so that a stop request or a reached limit ends it early. A green PR that is blocked, behind or unstable with all checks completed is neither action nor waiting, so it cannot keep the script asleep forever.
  - A free frontier ticket wins over waiting PRs.
  - Known limit: "newer than the head commit" compares with the committer date, which the author controls, and the REST API has no push time; a comment on a commit with a future date is missed, one on a back-dated commit counts as new. Replies are ignored so that an iteration that only answers on a thread does not trigger itself again.
- Main session output: the monitor lines above; no live status page, `status.json` on request; at run end one summary page as an artifact from `scripts/summary.py` (duration, counts, merged PRs, escalations, open questions, new retro tickets; no cost) (#420).
- The request to run `/retro` at the end of a session is dropped for sessions that started `/goRalph`, because the loop is to run its own retro (#418); the rule is in `docs/agents/implementation.md` ("Retro at the end of every session"), `CLAUDE.md` and the `brief-me` skill (#417).

## Planned (open tickets; not in the code yet)

- #414, usage limit wait: on a usage limit (HTTP 429) the script waits for the reset and retries, at most 5 h 15 min, then ends the run cleanly.
- #415, resume and safety net: besides the monitor (re-armed every 30 min) the main session keeps a `send_later` check-in every 60 min; it restarts a dead script idempotently when the run neither ended nor was stopped, and reports the restart. The counts `ok` and `failed` after a resume belong here.
- #416, iteration rules and permissions: the final content of `iteration-rules.md` (including the escalation pointer) and the permission set. Questions for the owner: the orchestrator comments on the ticket (question, options, recommendation), sets `needs-info` and goes on with other tickets; the ticket comment is the source of truth.
- #418, retro iteration: only as the last iteration of a run, also after `after-step` and `after-iteration` stops, not after `now`. `/retro` gets a non-interactive mode: it writes only the lessons allowed below and files retro tickets (`retro`, `ready-for-human`) instead of asking. A retro may change only lessons in `docs/agents/implementer-brief.md` and the loop's own learnings file `docs/agents/goRalph-learnings.md` without the owner (own PR with review); everything that changes rules, model defaults, gates or prompts becomes a `ready-for-human` ticket. The learnings file is capped at about 40 lines because every iteration reads it; when it is full the retro condenses instead of appending (lessons for every implementation move to `implementer-brief.md`, obsolete ones are deleted, and if it still does not fit a retro ticket raises the limit). Whether 40 lines are enough is open.
- #419, pre-run interview: when `goRalph` starts, the main session presents all open `needs-info` questions of the spec together with the retro tickets in one interview round, records the answers on the tickets and sets `ready-for-agent`; per retro ticket the owner decides do or drop, and accepted ones are planned right away (acceptance criteria, model line, `ready-for-agent`) and taken by the next run.
- #421 and #422, trials: first on a small test spec of three documentation-only Haiku tickets, then on a real small spec; they cover `permissions.deny`, the three stop modes, a resume after a deliberately killed script and a run of about 2 h.
