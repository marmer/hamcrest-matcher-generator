# Issue tracker: GitHub

Issues and specs for this repo live as GitHub issues. Use `gh api` (REST) for all operations, or `scripts/agents/issue.py` for claim, comment and close. `gh issue`/`gh pr` subcommands use GraphQL, which is blocked in Claude Code cloud sessions; use `gh api` REST or `scripts/agents/issue.py`.

## Conventions

`R` is `repos/marmer/hamcrest-matcher-generator`, as in **Cloud sessions** below.

- **Create an issue**: `gh api -X POST $R/issues -f title="..." -F body=@body.md`. Write multi-line bodies to a file first.
- **Read an issue**: `gh api $R/issues/<number>`.
- **List issues**: `gh api "$R/issues?state=open&labels=<label>&per_page=100"` (drop entries with a `pull_request` key).
- **Make an issue a sub-issue of a parent**: `gh api -X POST $R/issues/<parent>/sub_issues -F sub_issue_id=<child-db-id>` (database id, as in **Blocking** below). Without sub-issues, put `Part of #<parent>` at the top of the child body.
- **Comment on an issue**: `scripts/agents/issue.py comment <number> --body-file comment.md` (or `gh api -X POST $R/issues/<number>/comments -F body=@comment.md`).
- **Apply / remove labels**: `gh api -X POST $R/issues/<number>/labels -f 'labels[]=...'` / `gh api -X DELETE $R/issues/<number>/labels/<label>`.
- **Close**: `scripts/agents/issue.py close <number> [--reason completed|not_planned]` (or `gh api -X PATCH $R/issues/<number> -f state=closed -f state_reason=completed`).

Infer the repo from `git remote -v`; the REST commands below hard-code `R`, so use the same repo.

## Cloud sessions: REST only

In Claude Code cloud sessions GitHub GraphQL is blocked, so `gh issue …`, `gh pr …` and `gh label list` fail with HTTP 403. Use `gh api` (REST) there; `R=repos/marmer/hamcrest-matcher-generator`, and `-F body=@file.md` for multi-line bodies:

- **Create**: `gh api -X POST $R/issues -f title="..." -F body=@body.md -f 'labels[]=...'`
- **Read**: `gh api $R/issues/<n>` and `gh api $R/issues/<n>/comments`
- **List**: `gh api "$R/issues?state=open&labels=<label>&per_page=100"` (the result includes PRs; drop entries with a `pull_request` key)
- **Comment**: `gh api -X POST $R/issues/<n>/comments -F body=@comment.md`
- **Labels**: `gh api -X POST $R/issues/<n>/labels -f 'labels[]=...'` / `gh api -X DELETE $R/issues/<n>/labels/<label>`; list with `gh api "$R/labels?per_page=100"`, create with `gh api -X POST $R/labels -f name=... -f color=...`
- **Assign / claim**: `gh api -X POST $R/issues/<n>/assignees -f 'assignees[]=marmer'`
- **Close**: `gh api -X PATCH $R/issues/<n> -f state=closed -f state_reason=completed`
- **Edit body**: `gh api -X PATCH $R/issues/<n> -F body=@body.md`
- **Sub-issue**: `gh api -X POST $R/issues/<parent>/sub_issues -F sub_issue_id=<child-db-id>`; list with `gh api $R/issues/<parent>/sub_issues`
- **Blocking**: as in **Wayfinding operations** below (already REST).

The database id is `gh api $R/issues/<n> --jq .id`, not the `#number`.

## Pull requests as a triage surface

**PRs as a request surface: no.** _(Set to `yes` if this repo treats external PRs as feature requests; `/triage` reads this flag.)_

When set to `yes`, PRs run through the same labels and states as issues, using the `gh pr` equivalents:

- **Read a PR**: `gh pr view <number> --comments` and `gh pr diff <number>` for the diff.
- **List external PRs for triage**: `gh api --paginate 'repos/{owner}/{repo}/pulls?state=open' --jq '.[] | select(.author_association | IN("OWNER","MEMBER","COLLABORATOR") | not) | {number, title, author: .user.login, author_association, labels: [.labels[].name]}'`.
- **Comment / label / close**: `gh pr comment`, `gh pr edit --add-label`/`--remove-label`, `gh pr close`.

GitHub shares one number space across issues and PRs, so a bare `#42` may be either: resolve with `gh pr view 42` and fall back to `gh api $R/issues/42`.

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

## When a skill says "fetch the relevant ticket"

Read it as in **Read an issue** above.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a single issue with **child** issues as tickets.

- **Map**: a single issue labelled `wayfinder:map`, holding the Notes / Decisions-so-far / Fog body. `gh api -X POST $R/issues -f title=... -F body=@map.md -f 'labels[]=wayfinder:map'`.
- **Child ticket**: `scripts/agents/issue.py child <map> --title "..." --type <type> --body-file body.md`. It creates the issue with label `wayfinder:<type>` (`research`/`prototype`/`grilling`/`task`), appends `Part of #<map>`, links it as a sub-issue (by database id), prints the new number and refreshes the map's status. Where sub-issues aren't enabled, add the child to a task list in the map body instead. Once claimed, the ticket is assigned to the driving dev.
- **Blocking**: GitHub's **native issue dependencies**, the canonical, UI-visible representation. Add edges with `scripts/agents/issue.py block <child> --by <blocker> [--by <blocker2> ...] [--map <map>]`, which resolves the blockers' database ids and refreshes the map's status when `--map` is given. (Raw REST: `POST $R/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`, with the database id from `gh api $R/issues/<n> --jq .id`, not the `#number`.) GitHub reports `issue_dependencies_summary.blocked_by` (open blockers only, the live gate). Where dependencies aren't available, fall back to a `Blocked by: #<n>, #<n>` line at the top of the child body. A ticket is unblocked when every blocker is closed.
- **Frontier query**: list the map's open children (`gh api "$R/issues?state=open&per_page=100"`, dropping PRs, scoped to the map's sub-issues / task list), drop any with an open blocker (`issue_dependencies_summary.blocked_by > 0`, or an open issue in the `Blocked by` line) or an assignee; first in map order wins.
- **Claim**: `scripts/agents/issue.py claim <n>` (assigns the gh user; `--login` overrides), the session's first write.
- **Resolve**: `scripts/agents/issue.py comment <n> --body-file answer.md`, then `scripts/agents/issue.py close <n>`, then `scripts/agents/issue.py decide <map> <n> --gist "..."` to append the decision (gist + link) to the map's Decisions-so-far. It fails, changing nothing, if the section is missing or already links `<n>`.
- **Status**: after every claim, resolve, new ticket or new blocking edge, run `scripts/agents/issue-status.py <map>`. It regenerates the map's `## Status` section (a Mermaid graph of every child, coloured done / in progress / ready / blocked) between the `<!-- status:start -->` / `<!-- status:end -->` markers; leave that section to the script.

## Status view for maps and specs

Every parent issue with child tickets (a wayfinder map, or a spec from `/to-spec` with tickets from `/to-tickets`) carries the generated `## Status` section above, so its progress is visible on the issue at any time. Whoever changes a child's state (claim, close, new child, new blocker) runs `scripts/agents/issue-status.py <parent>` right after. `--print` previews the section; `--json` prints the raw status data, which is the input to use when the user runs `/show-me` for a richer view of the same state.
