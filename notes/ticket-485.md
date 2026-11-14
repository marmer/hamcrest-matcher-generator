# Handover: ticket #485 (background wait ceiling for goRalph children)

Part of spec #104. Branch `ticket-485`, based on origin/main 28199131.

## What changed

- `.claude/skills/goRalph/scripts/loop.sh`, `start_claude`: the child gets `CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS="${CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS:-3600000}"` (iteration and retro both go through it). A value already set in loop.sh's environment wins.
- `scripts/agents/test_go_ralph_loop.py`: stub `claude` records the ceiling it sees (`STUB_CALLS.ceiling`, `STUB_CALLS.retro.ceiling`); `env()` drops the variable so the runner's shell cannot leak in; four tests (default and already-set, iteration and retro).
- `docs/agents/ralph-loop.md`: bullet "Background wait ceiling (#485)".
- `docs/agents/goRalph-learnings.md`: the 10-minute line names run `104-20261114-131620`, iteration 1, and the variable (learnings file stays at 14 of 40 lines).

## Why one hour, not 0

`0` waits indefinitely; then only `max_iteration_duration` bounds an iteration, and it is absent by default. Owner decision in the ticket.

## Red evidence

- Default tests: red before the change (`'3600000' != 'unset'`), green after.
- Already-set tests: green on the old code (the child inherits the variable). Broken on purpose by hard-coding `3600000` in `start_claude`: `'1234' != '3600000'` for both iteration and retro, then restored.

## Verify

Commands and results are in the final report.
