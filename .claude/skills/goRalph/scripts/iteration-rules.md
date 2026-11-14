# Loop rules

You are one iteration of a Ralph loop: a fresh process with an empty context, started by a script. The state is in the issue tracker, Git and the run directory.

- Read `docs/agents/goRalph-learnings.md` before you start: it holds the loop's lessons so far.
- Work only through `/implement-spec` and `docs/agents/implementation.md`; this file adds the loop rules only.
- Read `control.json` in the run directory (the literal value under "This run") between work steps. When `stop` is `after-step` or `true`, end after the current work step; when it is `after-iteration`, finish this iteration as usual (the script does not start another); `now` is handled by the script, which kills you.
- `max_parallel` is a hard number, not a hint: read it from `control.json` at the start (Read tool on the literal run directory under "This run") and never have more implementers running at the same time than that number. It is the upper bound of tickets in this iteration; with 1, start the next implementer only after the previous one has reported.
- Read `control.json` again (Read tool, same path) before each new implementer, after each merge and before each review round, not only at the start; a `stop` found there is handled as in the rule above.
- Keep a handover note `notes/ticket-<n>.md` on the ticket branch, updated after every work step.
- Before you end, write the work data with `python3 scripts/agents/ralph-iteration.py data <run directory> <iteration> --tickets <numbers> --prs <numbers>` (it writes `iteration-<iteration>.json` as `{"tickets": [<numbers>], "prs": [<numbers>]}`): the tickets you worked on and the PRs you opened in this iteration, no numbers after a flag when there were none. The script prints them on the iteration's end line.
- Never ask the owner interactively. Comment on the ticket with the question, the options and your recommendation, set `needs-info` and go on with other tickets.
- Open the PR of each ticket with squash auto-merge. Wait for CI once, with one blocking poll of at most 30 min that spends no tokens while it blocks.
- On red CI make one fix, then end; whatever is left (second red run, merge conflict, timeout) goes into the handover note for the next iteration.
- End when your PRs are merged or after that one CI fix.
- The escalation path after failed attempts (you run on `sonnet`, no `opus` in the loop) is the rule "Escalation in the `/goRalph` loop" in `docs/agents/implementation.md`.
- Enable the auto-merge of a PR with `python3 scripts/agents/ralph-iteration.py auto-merge <pr>`. Never merge by hand (no merge endpoint, no `gh pr merge`), also not when a check is red or the command fails: say so in the handover note and a ticket comment and leave the PR open.
- The run directory and your iteration number are given as literal values under "This run" at the end of this prompt: use them wherever these rules name the run directory or the iteration, and never expand a variable such as `$RALPH_RUN_DIR`.
- Make every Bash call one command: no `;` or `&&` chain, no `$VAR` and no command substitution; a pipe is allowed only into `head` or `grep`.
