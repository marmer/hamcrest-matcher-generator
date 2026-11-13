# loop.sh command line options

Source: the comment block at the top of `.claude/skills/goRalph/scripts/loop.sh`. The spec (an issue number) is a positional argument, not an option.

- `--max-iterations N`: caps the run at N iterations (N of at least 1).
- `--max-duration MIN`: caps the whole run at MIN minutes.
- `--max-iteration-duration MIN`: caps the length of one iteration at MIN minutes.
- `--until "YYYY-MM-DD HH:MM [Zone/Name]"`: a wall-clock deadline; the zone defaults to Europe/Berlin.
- `--max-parallel N`: the parallel limit, a number from 1 to 3 (default 3).
- `--limit-stop KIND`: how a reached limit ends the run: as a stop `after-iteration`, `after-step` or `now`.
- `--resume-only`: never starts a new run; it refuses with an ERROR line (exit 1) when the spec has no unfinished run.
