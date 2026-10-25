# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`GLOSSARY.md`** at the repo root, or
- **`GLOSSARY-MAP.md`** at the repo root if it exists: it points at one `GLOSSARY.md` per context. Read each one relevant to the topic.
- **`docs/adr/`**: read ADRs that touch the area you're about to work in. In multi-context repos, also check `src/<context>/docs/adr/` for context-scoped decisions.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## arc42 documentation (human-facing)

This repo also has arc42 architecture documentation in `docs/arc42.adoc`, with one AsciiDoc file per chapter in `docs/src/`. It is published to GitHub Pages and is written **primarily for humans**. Read the chapters relevant to the topic as well:

When proposing content for an arc42 section, first read that section's template and examples at `https://docs.arc42.org/section-<n>/` and write in its form (e.g. quality scenarios in 10.2: Q42 short form or SEI long form, specific and measurable).

| Topic                     | arc42 chapter                                                   | Agent-facing counterpart |
| ------------------------- | --------------------------------------------------------------- | ------------------------ |
| Goals, quality goals      | `docs/src/01_introduction_and_goals.adoc`                       | none                     |
| Constraints, context      | `docs/src/02_architecture_constraints.adoc`, `03_context_and_scope.adoc` | none            |
| Building blocks, runtime  | `docs/src/05_building_block_view.adoc`, `06_runtime_view.adoc`  | none                     |
| Cross-cutting concepts    | `docs/src/08_concepts.adoc`                                     | none                     |
| Architecture decisions    | `docs/src/09_architecture_decisions.adoc`                       | `docs/adr/`              |
| Glossary                  | `docs/src/12_glossary.adoc`                                     | `GLOSSARY.md`            |
| Resolved wayfinder decision without an ADR | features/requirements → `01_introduction_and_goals.adoc`; quality goals → `01` (Quality Goals) and `10_quality_requirements.adoc`; quality gates, CI → `08_concepts.adoc`; release, publishing → `07_deployment_view.adoc` | the ticket's resolution comment |

### Redundancy rules

Redundant documentation for agents is welcome, **as long as the human-facing arc42 docs are kept in sync**:

- **Writing a glossary term** to `GLOSSARY.md`: also add or update the matching row in the table in `docs/src/12_glossary.adoc`.
- **Writing an ADR** to `docs/adr/`: also add a human-readable summary to `docs/src/09_architecture_decisions.adoc`: title, status, a short rationale, and a link to the ADR file.
- **Changing architecture** (new module, changed generation flow, changed build setup): update the affected arc42 chapter in the same change, not in a follow-up.
- Write the arc42 entries for a human reader: full sentences, context first, and none of the terse agent-only shorthand.
- Remove any template placeholder rows (e.g. `<Term-1>`) from a chapter when you add its first real entry.
- `scripts/agents/check-domain-docs-sync.py` checks these rules in CI (job `domain-docs-sync` in `.github/workflows/domain-docs-sync.yml`) and can be run locally with `python3 scripts/agents/check-domain-docs-sync.py`.
- If the agent docs and arc42 disagree, don't silently pick one. Surface the conflict and resolve it in both places.

## File structure

Single-context repo (most repos):

```
/
├── GLOSSARY.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

Multi-context repo (presence of `GLOSSARY-MAP.md` at the root):

```
/
├── GLOSSARY-MAP.md
├── docs/adr/                          ← system-wide decisions
└── src/
    ├── ordering/
    │   ├── GLOSSARY.md
    │   └── docs/adr/                  ← context-specific decisions
    └── billing/
        ├── GLOSSARY.md
        └── docs/adr/
```

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `GLOSSARY.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Language

Repo artifacts (code, docs, ADRs, glossary, arc42) and issue-tracker content are written in English; chat replies follow the user's language. The owner writes German: reply in German, also when the invocation carries no prose (a bare `/wayfinder 52`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders), but worth reopening because…_
