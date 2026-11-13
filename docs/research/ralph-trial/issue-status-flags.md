# Command line flags of `scripts/agents/issue-status.py`

The script takes a parent issue number as its first argument and one optional flag:

- `--print`: prints the status section (counts and Mermaid graph) to stdout instead of updating the parent's body.
- `--json`: prints the raw status data of the sub-issues as JSON and stops, without writing anything.
- `--frontier`: prints the ready tickets, one per line, with tickets waiting for the owner on a separate line; the parent is not written.
