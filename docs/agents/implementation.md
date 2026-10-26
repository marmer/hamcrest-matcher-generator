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
- **Every checkable requirement is an acceptance criterion.** Implementers build to the criteria, so a constraint that lives only in "What to build" (e.g. "compared character by character") gets dropped; repeat it as a criterion.

## Orchestration with `/implement-spec`

The orchestrator usually runs on Sonnet. It starts each implementer subagent on the model its ticket names, then judges the result before merging:

- **Quality bar**: `./mvnw clean verify` green, the ticket's acceptance criteria met, no shortcuts (skipped or weakened tests, suppressed checks), and the code reads like the surrounding code.
- **Haiku gets up to three failed attempts** before escalating. An attempt is the initial run or one targeted follow-up instruction to the same subagent. After the third attempt misses the bar, discard the work and rerun the ticket on `sonnet`.
- **Escalate Haiku at once** when the orchestrator judges it has taken a fundamentally wrong direction (e.g. misread the ticket, built in the wrong place, worked around the design instead of following it), since follow-ups won't recover that.
- **Sonnet and Opus get one follow-up** instruction; if the result still misses the bar, escalate (`sonnet` → `opus`). If Opus still misses it, stop and ask the user.
- **Record every escalation** as a comment on the ticket: from which model to which, and why. These comments feed the retro.
- **Subagent briefs** state that the subagent reports back only through its final answer and leaves session tools (`send_message`, `archive_session`, `create_session`) to the orchestrator; Haiku otherwise tends to call them with placeholder arguments. They tell it to take the commit attribution trailer from its own context and give only the `Claude-Session:` line, and to back every claim about the project's history or the code ("version X changed nothing") with the command that shows it, so the orchestrator can re-run it.
- **A ticket is done when CI is green on the pushed integration branch**, not when the merge succeeds: after merging and pushing, read the check runs of the new head (`gh api $R/commits/<sha>/check-runs`) before closing the ticket. Workflow changes (`.github/workflows/`) cannot be verified locally at all, so for those wait for the run.
- **One hard-to-reverse step per tool call** (create PR, merge, close, push), so a denied or failed step leaves the others intact.
- **Keep the spec's status current**: pass `--map <spec>` to `issue.py claim`/`close`, and after a merge run `scripts/agents/issue-status.py <spec>` (see "Status view for maps and specs" in `docs/agents/issue-tracker.md`).

## Batch completion

These steps override step 7 of `/implement-spec`, which reviews only once all tickets are complete. A **batch** ends when the session stops taking new tickets, and at the latest after 5 merged tickets, whatever the batch size. Then, in order and without asking the user:

1. **Review**: run `code-review` on the integration branch since its tip before the batch's first merge, so the Spec axis covers only that batch's tickets.
2. **Fix**: start a single implementer subagent on the findings. Brief it which findings to fix and which stay as they are (with the reason); pick its model by "Implementer model per ticket" above and judge it by the same quality bar and escalation rules. Ask the user only about findings that need their decision, and leave those out of the fix.
3. **Green**: merge and push the fix; CI on the new head is green.
4. **Draft PR**: open a draft PR from the integration branch into `development` with the `pr` skill, or update the open one's title and body to the new state. It closes the spec's resolved tickets and stays a draft until the spec is complete.
5. **Watch**: subscribe to the PR's activity and drive it to green: fix CI failures and answer or address review comments until it is merged or closed.

## Retro at the end of every session

`retro` is user-invoked only (`disable-model-invocation`), so the agent cannot start it. Close the final report of every implementation session (and every wayfinder session) by asking the user to run `/retro`. When it runs, apply the candidates the user accepts (checks, coding standards, docs, ticket-writing guidance, model defaults) and commit them, so the process improves with each session. Escalations recorded on tickets are a primary input: repeated escalations of a ticket kind mean tickets of that kind should be cut smaller or default to a higher model.

## Explaining pull requests visually

Whenever a PR's change is easier to grasp as a picture than as prose (a new module boundary, a changed call flow, a restructured class hierarchy, a before/after of generated code or a mismatch message, a CI or release pipeline), read `.agents/skills/show-me/SKILL.md` and apply it to the PR body: the smallest view that makes the point, e.g. a Mermaid diagram, a call tree, a module tree or a before/after snippet. `show-me` itself is user-invoked only; when a richer, interactive view would help the reviewer, end the PR report by suggesting the user run `/show-me` on the PR.
