---
name: goRalph
description: "Run /implement-spec as an unattended loop of fresh claude -p processes, watched through a quiet monitor."
disable-model-invocation: true
argument-hint: "A spec number and limits in plain language, e.g. #410 max 5 Runden"
---

A **goRalph run** works one spec through a script, not through this session: `scripts/loop.sh` starts one fresh `claude -p "/implement-spec <spec>"` orchestrator per iteration. This session only starts the script and watches its monitor lines. Design: `docs/agents/ralph-loop.md`.

1. **Parse** the argument: the spec number and `max_iterations` (the only limit the script enforces yet). Show the parsed values in one line, then go on.
2. **Start** the script in the background, its stdout into a monitor file, through `bash` (no executable bit needed):
   ```bash
   mkdir -p ~/.goRalph && bash "${CLAUDE_SKILL_DIR}/scripts/loop.sh" <spec> --max-iterations <N> >> ~/.goRalph/monitor-<spec>.log
   ```
   Use `run_in_background`. Omit `--max-iterations` for no limit. The script creates the run directory `~/.goRalph/runs/<spec>-<start time>/` (control file `control.json`, `status.json`, `result-<i>.json`, `stream-<i>.jsonl`) and a lock under `~/.goRalph/locks/`; a second start for the same spec is refused with an `ERROR` line, an unfinished run of the spec is continued.
3. **Watch** with the Monitor tool on `tail -n 0 -F ~/.goRalph/monitor-<spec>.log`. The script prints only the lines the monitor needs: `iteration <i> ok: ...` per iteration end, `ERROR ...` (an iteration with exit code != 0, `is_error` set, or a result without `result` field, e.g. a turn limit), `DENIAL ...` and `END ...`. Pass nothing else on to the owner.
4. **Stop**: set `"stop": "after-step"` (or `after-iteration`) in `control.json` of the run directory; the script ends before the next iteration. `status.json` shows the state on request.
