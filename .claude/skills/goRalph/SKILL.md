---
name: goRalph
description: "Run /implement-spec as an unattended loop of fresh claude -p processes, watched through a quiet monitor."
disable-model-invocation: true
argument-hint: "A spec number and limits in plain language, e.g. #410 max 5 Runden"
---

A **goRalph run** works one spec through a script, not through this session: `scripts/loop.sh` starts one fresh `claude -p "/implement-spec <spec>"` orchestrator per iteration. This session only starts the script and watches its monitor lines. Design: `docs/agents/ralph-loop.md`.

1. **Parse** the argument into the spec number (`#<n>`) and the limits, then show the parsed values in one line and go on:
   - `max N Runden` is `--max-iterations N`; `höchstens 4 Stunden` is `--max-duration 240` (minutes); `Runden höchstens 120 Minuten` is `--max-iteration-duration 120`;
   - `bis 6 Uhr` is `--until "<next 06:00 as YYYY-MM-DD HH:MM>"`, Europe/Berlin unless a zone is named (`--until "2026-11-14 06:00 America/New_York"`);
   - `--max-parallel N` (1 to 3, default 3); `--limit-stop after-iteration|after-step|now` is the stop kind a reached limit acts as (default `after-step`).
   Every limit is absent by default. A value the script cannot read ends it with an `ERROR` line and exit 2: tell the owner.
2. **Start** the script in the background, its stdout into a monitor file, through `bash` (no executable bit needed):
   ```bash
   mkdir -p ~/.goRalph && bash "${CLAUDE_SKILL_DIR}/scripts/loop.sh" <spec> <limit options> >> ~/.goRalph/monitor-<spec>.log
   ```
   Use `run_in_background`. Omit an option for no such limit. The script creates the run directory `~/.goRalph/runs/<spec>-<start time>/` (control file `control.json`, `status.json`, `result-<i>.json`, `stream-<i>.jsonl`) and a lock under `~/.goRalph/locks/`; a second start for the same spec is refused with an `ERROR` line, an unfinished run of the spec is continued.
3. **Watch** with the Monitor tool on `tail -n 0 -F ~/.goRalph/monitor-<spec>.log`. The script prints only the lines the monitor needs: `iteration <i> ok: ...` per iteration end, `ERROR ...` (an iteration with exit code != 0, `is_error` set, or a result without `result` field, e.g. a turn limit), `DENIAL ...` and `END ...`. Pass nothing else on to the owner. Per line kind: `iteration ... ok`: do nothing; `ERROR`/`DENIAL`: tell the owner in one short sentence; `END`: show the summary line, stop the monitor and publish the summary page (step 5).
4. **Change and stop**: edit `control.json` of the run directory on request (same keys as the options: `until`, `max_duration`, `max_iterations`, `max_iteration_duration`, `max_parallel`, `limit_stop`); the script reads it before every iteration. Stop by setting `"stop"`: `after-step` (the default, also for a request without a kind; the script ends before the next iteration, the orchestrator ends after its current work step), `after-iteration` (the running iteration finishes) or `now` (the script kills the iteration's process group; the state is the last pushed work step). A reached limit writes its stop kind itself; `max_iteration_duration` writes it during the iteration and kills the iteration after a grace period of 5 min. `status.json` shows the state on request.
5. **End: summary page**, once per run, on the `END` line only (a restart after a dead script is not an end; no page during the run, no updates after it). Run `python3 "${CLAUDE_SKILL_DIR}/scripts/summary.py" <run directory>`; its JSON, from `status.json` and the tracker (never the streams or results), is the only data. Build one phone-friendly Artifact page from it: duration and iteration counts, then merged PRs, escalations, open questions (`needs-info` tickets), new retro tickets, each a short list with links; an empty list says "none". If the script fails, tell the owner in one sentence and publish no page.
