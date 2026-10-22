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
- **Escalate on poor quality**: if the result misses the bar and one targeted follow-up instruction to the same subagent doesn't fix it, discard the attempt and rerun the ticket on the next model up: `haiku` → `sonnet` → `opus`. Don't retry on the same model more than once.
- **Record every escalation** as a comment on the ticket: from which model to which, and why. These comments feed the retro.

## Retro at the end of every session

Finish every implementation session (and every wayfinder session) by calling the Skill tool with `retro`. Present its improvement candidates to the user, apply the ones they accept (checks, coding standards, docs, ticket-writing guidance, model defaults), and commit them, so the process improves with each session. Escalations recorded on tickets are a primary input: repeated escalations of a ticket kind mean tickets of that kind should be cut smaller or default to a higher model.
