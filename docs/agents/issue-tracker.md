# Issue tracker: GitHub

Issues and specs for this repo live as GitHub issues. Use the `gh` CLI for all operations.

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --json number,title,body,labels,comments`.
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with appropriate `--label` and `--state` filters.
- **Make an issue a sub-issue of a parent**: `gh issue create --parent <parent> ...`, or `gh issue edit <parent> --add-sub-issue <child>` afterwards (`gh` 2.94+). Older `gh`: `gh api --method POST repos/<owner>/<repo>/issues/<parent>/sub_issues -F sub_issue_id=<child-db-id>` (database id, as in **Blocking** below). Without sub-issues, put `Part of #<parent>` at the top of the child body.
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

Infer the repo from `git remote -v`; `gh` does this automatically when run inside a clone.

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

GitHub shares one number space across issues and PRs, so a bare `#42` may be either: resolve with `gh pr view 42` and fall back to `gh issue view 42`.

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

## When a skill says "fetch the relevant ticket"

Read it as in **Read an issue** above.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a single issue with **child** issues as tickets.

- **Map**: a single issue labelled `wayfinder:map`, holding the Notes / Decisions-so-far / Fog body. `gh issue create --label wayfinder:map`.
- **Child ticket**: an issue linked to the map as a GitHub sub-issue (see **Make an issue a sub-issue of a parent**). Where sub-issues aren't enabled, add the child to a task list in the map body and put `Part of #<map>` at the top of the child body. Labels: `wayfinder:<type>` (`research`/`prototype`/`grilling`/`task`). Once claimed, the ticket is assigned to the driving dev.
- **Blocking**: GitHub's **native issue dependencies**, the canonical, UI-visible representation. Add an edge with `gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`, where `<blocker-db-id>` is the blocker's numeric **database id** (`gh api repos/<owner>/<repo>/issues/<n> --jq .id`, _not_ the `#number` or `node_id`). GitHub reports `issue_dependencies_summary.blocked_by` (open blockers only, the live gate). Where dependencies aren't available, fall back to a `Blocked by: #<n>, #<n>` line at the top of the child body. A ticket is unblocked when every blocker is closed.
- **Frontier query**: list the map's open children (`gh issue list --state open`, scoped to the map's sub-issues / task list), drop any with an open blocker (`issue_dependencies_summary.blocked_by > 0`, or an open issue in the `Blocked by` line) or an assignee; first in map order wins.
- **Claim**: `gh issue edit <n> --add-assignee @me`, the session's first write.
- **Resolve**: `gh issue comment <n> --body "<answer>"`, then `gh issue close <n>`, then append a context pointer (gist + link) to the map's Decisions-so-far.
- **Status**: after every claim, resolve, new ticket or new blocking edge, run `scripts/agents/issue-status.py <map>`. It regenerates the map's `## Status` section (a Mermaid graph of every child, coloured done / in progress / ready / blocked) between the `<!-- status:start -->` / `<!-- status:end -->` markers; leave that section to the script.

## Status view for maps and specs

Every parent issue with child tickets (a wayfinder map, or a spec from `/to-spec` with tickets from `/to-tickets`) carries the generated `## Status` section above, so its progress is visible on the issue at any time. Whoever changes a child's state (claim, close, new child, new blocker) runs `scripts/agents/issue-status.py <parent>` right after. `--print` previews the section; `--json` prints the raw status data, which is the input to use when the user runs `/show-me` for a richer view of the same state.
