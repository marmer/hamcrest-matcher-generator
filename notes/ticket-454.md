# Ticket 454

- `write_status` in `.claude/skills/goRalph/scripts/loop.sh` now writes `status.json.tmp` and `mv`s it over.
- Test poll helper `state()` skips a `JSONDecodeError` read (bounded by `wait_until` deadline).
- Pin: `test_status_file_is_never_observable_half_written` (jq stand-in splits output, inspects status.json between halves).
