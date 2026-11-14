# goRalph control file: limit keys

The control file `control.json` in the run directory holds these limit keys (source: `docs/agents/ralph-loop.md`, "Control file"):

- `until`: stop time `YYYY-MM-DD HH:MM`, optionally followed by a zone name (Europe/Berlin otherwise)
- `max_duration`: maximum length of a run in whole minutes
- `max_iterations`: maximum number of iterations
- `max_iteration_duration`: maximum length of one iteration in whole minutes
- `max_parallel`: upper bound of tickets per iteration

The same file also holds `limit_stop` (the stop kind a reached limit acts as) and `stop` (the stop request), which are not limits.
