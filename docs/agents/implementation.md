# Implementation process

How implementation tickets (labelled `ready-for-agent`) are built in this repo. Wayfinder map tickets (`wayfinder:*`) are decisions, not implementation, and are exempt from the model rules below.

## Implementer model per ticket

Every implementation ticket names the model its implementer subagent starts with, as a line in the ticket body:

```markdown
**Implementer model:** haiku
```

- **Default: `haiku`.** Write tickets small and precise enough for Haiku to implement: clear acceptance criteria, context pointers to the spec, research notes and relevant files, one concern per ticket.
- **Exception: `sonnet`**, when the ticket is hard to break down for Haiku, e.g. a cross-cutting design inside the annotation processor or subtle generics handling. Add a one-line reason below the model line.
- **Rare exception: `opus`**, only when even Sonnet is unlikely to succeed. Always add the reason.
- A missing model line means `haiku`.
- **Every checkable requirement is an acceptance criterion.** Implementers build to the criteria, so a constraint that lives only in "What to build" (e.g. "compared character by character") gets dropped; repeat it as a criterion. A criterion that freezes something ("unchanged", "byte-identical", "only X changes") names the files and tests it covers, and the ticket author checks that the frozen material holds no assumption the ticket itself changes.
- **The spec's testing strategy becomes criteria too.** Copy every line of the spec's testing strategy that applies to the ticket (golden files, end-to-end cases, one-line form, arrays) into its acceptance criteria; #116 missed the spec's golden-file and one-line-form tests, and the review had to add them.

## Orchestration with `/implement-spec`

The orchestrator usually runs on Sonnet. It starts each implementer subagent on the model its ticket names, then judges the result before merging:

- **Quality bar**: `./mvnw clean verify` green, the ticket's acceptance criteria met, no shortcuts (skipped or weakened tests, suppressed checks), no test-last (every new test was seen red first), and the code reads like the surrounding code.
- **Red evidence**: the orchestrator sends back every report that does not name, per new test, how it was seen red (brief steps 3 and 7). Coverage-floor tests (JaCoCo) are no exception: break the branch they pin and see them red. Such a report counts as a missed attempt under the escalation rules.
- **Haiku gets up to three failed attempts** before escalating. An attempt is the initial run or one targeted follow-up instruction to the same subagent. After the third attempt misses the bar, discard the work and rerun the ticket on `sonnet`.
- **Escalate Haiku at once** when the orchestrator judges it has taken a fundamentally wrong direction (e.g. misread the ticket, built in the wrong place, worked around the design instead of following it), since follow-ups won't recover that.
- **Sonnet and Opus get one follow-up** instruction; if the result still misses the bar, escalate (`sonnet` → `opus`). If Opus still misses it, stop and ask the user.
- **Record every escalation** as a comment on the ticket: from which model to which, and why. These comments feed the retro.
- **Subagent briefs** are `docs/agents/implementer-brief.md`: pass it to the implementer by path, with the ticket number and the `Claude-Session:` line for its commits; start it with `subagent_type: implementer` (`.claude/agents/implementer.md`), which limits its tools to git, build and file work, and pass its model explicitly. When the `implementer` agent type is not registered in the session (agent types load at session start), start `general-purpose` with the tool list of `.claude/agents/implementer.md` named in the prompt. The orchestrator passes the `Claude-Session:` line only: the implementer's co-author trailer names its own model, so a prompt never dictates it. The brief holds the implementer's steps and lessons; the orchestrator adds only the ticket-specific context.
- **Notes survive a scratchpad change**: the scratchpad path changes after `/clear` or a restart, so at session start the orchestrator copies `notes/` from earlier scratchpads of this repo into the new one: `cp -n /tmp/claude-0/*/*/scratchpad/notes/* <new scratchpad>/notes/`.
- **Stop each implementer once it has reported** (`TaskStop`); a stopped agent resumes with its context through `SendMessage`. A Haiku implementer once kept `sleep` loops running and re-sent its report three times.
- **Usage limit**: an implementer that ends on a rate-limit error (HTTP 429, "hit your limit") resumes through `SendMessage` after the reset, with its original ticket and remaining steps; `/limit-loop` (`.claude/skills/limit-loop/SKILL.md`) schedules that check. If it died before its first commit, its worktree is gone: the resumed agent starts a fresh worktree on its ticket branch from `origin/main`.
- **A ticket is done when its PR is squash-merged into `main`** (see below), not when the implementer reports: the PR's `Closes #<n>` closes the ticket. Workflow changes (`.github/workflows/`) cannot be verified locally at all, so for those the PR's CI run is the only proof.
- **One hard-to-reverse step per tool call** (create PR, enable auto-merge, close, push), so a denied or failed step leaves the others intact.
- **Keep the spec's status current**: pass `--map <spec>` to `issue.py claim`, and after each merged PR run `scripts/agents/issue-status.py <spec>` (see "Status view for maps and specs" in `docs/agents/issue-tracker.md`).

## One ticket, one review, one PR

These steps replace steps 3 to 8 of `/implement-spec`: there is no integration branch, no merger subagent and no batch. `main` is the trunk, and every ticket reaches it through its own small PR, so a defect is caught before the next ticket builds on it. The owner approves pushing `ticket-<n>`, retro and session-end branches, whatever branch a cloud session was assigned.

1. **Branch**: the implementer works on `ticket-<n>`, based on the tip of `origin/main`. A ticket is only started when its blockers are merged there.
2. **Judge**: the orchestrator checks the report against the quality bar above. Only the implementer builds in its worktree: the orchestrator stops the implementer agent before it verifies there itself. Right after each implementer report, the orchestrator pushes `ticket-<n>` as a backup, not only at PR time; if a container restart ends an implementer, a fresh one continues from `origin/ticket-<n>`.
3. **Review**: run `code-review` with both axes (Standards and Spec) on `ticket-<n>` against `origin/main`, so the Spec axis covers exactly this ticket. Findings go back to the same implementer as a follow-up; it counts as an attempt under the escalation rules. Brief which findings to fix and which stay as they are (with the reason). Ask the user only about findings that need their decision.
4. **PR**: once the review's findings are fixed, the implementer merges the current `origin/main` tip into its branch (never rebase), re-runs the verify builds and reports. The orchestrator pushes the branch and opens a PR into `main`, ready for review (not a draft, so auto-merge can fire), with the `pr` skill: `Closes #<n>`, `Part of #<spec>`. Then it enables auto-merge with the squash method (`enable_pr_auto_merge`), as the owner rule in `docs/agents/wayfinding.md` describes.
5. **Watch**: subscribe to the PR's activity. Red CI, review threads or a merge conflict are work now: fix them on the branch and push, until GitHub merges it. Then run `issue-status.py <spec>` and start the next frontier ticket (or tickets, under the parallel rule below) from the new `main` tip.

By default the orchestrator commissions implementation tickets one after another: the next ticket goes to an implementer only after the previous ticket's PR has merged, and it starts from the new `origin/main` tip. Parallel ticket implementers caused merge conflicts in every implementation session so far (owner rule). Tickets may run in parallel if, and only if, the orchestrator is sure from a short estimate (no extensive analysis) that they will not conflict: they touch disjoint files, judged from the tickets and the code they name. Two tickets in the same package or class (e.g. both in the Hamcrest runtime library, both changing the generator or golden files, both editing the root pom) count as conflicting; #116 and #117 looked separate but both edited `BeanPropertyMatcher`. Other work may run in parallel anyway: reviews, research, and changes that are not tickets of the spec (e.g. a retro or session-end fix). A whole-repo reformat ticket runs alone and last. When a PR still conflicts with `main`, merge `origin/main` into its branch and push; never force-push.

## Session-end review

Per-ticket reviews cannot see problems that span tickets: duplicated helpers across tickets, docs drifting from code, two tickets choosing different conventions. When the session stops taking tickets, run `code-review` with both axes on `origin/main` since its tip before the session's first merged PR, so the Spec axis covers all of the session's tickets. Fix its findings in one implementer subagent, through one PR with the same review and auto-merge steps.

## Retro at the end of every session

`retro` is user-invoked only (`disable-model-invocation`), so the agent cannot start it. Close the final report of every implementation session (and every wayfinder session) by asking the user to run `/retro`. When it runs, apply the candidates the user accepts (checks, coding standards, docs, ticket-writing guidance, model defaults) and commit them, so the process improves with each session. Escalations recorded on tickets are a primary input: repeated escalations of a ticket kind mean tickets of that kind should be cut smaller or default to a higher model. Implementer subagents make the retro fixes wherever that is sensible, each on the model its change needs under "Implementer model per ticket", under the same quality bar, escalation rules and "One ticket, one review, one PR" steps. The orchestrator only judges, reviews, pushes and opens the PR, including for fixes that come up while the retro PR is watched, such as a red CI job.

## Explaining pull requests visually

Whenever a PR's change is easier to grasp as a picture than as prose (a new module boundary, a changed call flow, a restructured class hierarchy, a before/after of generated code or a mismatch message, a CI or release pipeline), read `.agents/skills/show-me/SKILL.md` and apply it to the PR body: the smallest view that makes the point, e.g. a Mermaid diagram, a call tree, a module tree or a before/after snippet. `show-me` itself is user-invoked only; when a richer, interactive view would help the reviewer, end the PR report by suggesting the user run `/show-me` on the PR.
