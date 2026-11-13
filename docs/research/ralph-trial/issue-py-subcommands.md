# scripts/agents/issue.py subcommands

Issue write operations through the GitHub REST API. Each subcommand, in the order `--help` lists them:

- `claim`: assign an issue to a login (default: the gh user), optionally refresh a map's status.
- `comment`: post the text of a file as a comment on an issue.
- `close`: close an issue as completed or not planned.
- `decide`: append a decision line for a ticket to a map's Decisions-so-far section.
- `amend`: replace the existing decision line for a ticket in a map.
- `scope`: append an out-of-scope line for a ticket to a map's Out-of-scope section.
- `fog`: remove and/or add a bullet in a map's Not-yet-specified section.
- `note`: remove and/or add a bullet in a map's Notes section.
- `child`: create a wayfinder child ticket as a sub-issue of a map.
- `block`: record that an issue is blocked by other issues.
- `check`: tick or untick task items (acceptance criteria) in an issue body.
