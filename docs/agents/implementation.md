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

## Orchestration with `/implement-spec`

The orchestrator usually runs on Sonnet. It starts each implementer subagent on the model its ticket names, then judges the result before merging:

- **Quality bar**: `./mvnw clean verify` green, the ticket's acceptance criteria met, no shortcuts (skipped or weakened tests, suppressed checks), and the code reads like the surrounding code.
- **Haiku gets up to three failed attempts** before escalating. An attempt is the initial run or one targeted follow-up instruction to the same subagent. After the third attempt misses the bar, discard the work and rerun the ticket on `sonnet`.
- **Escalate Haiku at once** when the orchestrator judges it has taken a fundamentally wrong direction (e.g. misread the ticket, built in the wrong place, worked around the design instead of following it), since follow-ups won't recover that.
- **Sonnet and Opus get one follow-up** instruction; if the result still misses the bar, escalate (`sonnet` → `opus`). If Opus still misses it, stop and ask the user.
- **Record every escalation** as a comment on the ticket: from which model to which, and why. These comments feed the retro.
- **Keep the spec's status current**: after each claim, merge or ticket close, run `scripts/agents/issue-status.py <spec>` (see "Status view for maps and specs" in `docs/agents/issue-tracker.md`).

## Retro at the end of every session

`retro` is user-invoked only (`disable-model-invocation`), so the agent cannot start it. Close the final report of every implementation session (and every wayfinder session) by asking the user to run `/retro`. When it runs, apply the candidates the user accepts (checks, coding standards, docs, ticket-writing guidance, model defaults) and commit them, so the process improves with each session. Escalations recorded on tickets are a primary input: repeated escalations of a ticket kind mean tickets of that kind should be cut smaller or default to a higher model.

## Explaining pull requests visually

Whenever a PR's change is easier to grasp as a picture than as prose (a new module boundary, a changed call flow, a restructured class hierarchy, a before/after of generated code or a mismatch message, a CI or release pipeline), read `.agents/skills/show-me/SKILL.md` and apply it to the PR body: the smallest view that makes the point, e.g. a Mermaid diagram, a call tree, a module tree or a before/after snippet. `show-me` itself is user-invoked only; when a richer, interactive view would help the reviewer, end the PR report by suggesting the user run `/show-me` on the PR.
