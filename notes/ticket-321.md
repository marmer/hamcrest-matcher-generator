# Ticket 321: second arc42 review after the architecture polish (#203)

Reviewed state: `origin/main` at 486b92a7. #203 base for the code diff: `c80849d6~1` (first #203 refactor, #332/#340);
the #203 refactors are #332, #334, #335, #475, #476 and the pin #477 (ADR 0007).
Changed names checked by grep over `docs/src`, `docs/adr`, `GLOSSARY.md`, README, CONTRIBUTING, MAINTAINING:
`ConfiguredNames`, `ConfigurationSite`, `PlannedType`, `TypeExpansion`, `TypeHierarchy`, `ProcessorDiagnostics`,
`GeneratedSourceWriter`, `ReferencePresetPlanner`, `AssertionNaming`, `FileShell`, `PropertyRead`, `PropertyAccess`.
Read-only review first, then fixes; one headless pass over the seven dimensions of #320.

## Merged findings (by severity)

| # | Sev | Dimension | Finding | Proposed fix | Status |
|---|---|---|---|---|---|
| 1 | medium | accuracy, diagrams | Chapter 5, `workspace.dsl` CoreComponents and chapter 6 do not mention `ConfiguredNames`, `TypeExpansion`, `ConfigurationSite`, `PlannedType` (#475, #476); the worker's description still says it resolves names itself. | Add both classes to the view and the chapter; describe the two records under the worker. | fixed |
| 2 | low | accuracy | Chapter 5, Important Interfaces: "the AssertJ backend two" files; it returns two per type plus the per-package mixin and representation (chapter 6 says so). | Reword. | fixed |
| 3 | low | accuracy | Chapter 5 AssertJ text omits the package-private `AssertionNaming` and `FileShell` that #203 added. | One paragraph. | fixed |
| 4 | low | rendering | Chapter 4: the three places of reflection by name are `*` items at the level of the decisions list, so they render as three more top-level decisions. | Nest them (`**`). | fixed |
| 5 | low | language | Chapter 10 intro keeps a drafting sentence about "None yet (planned for 6.0.0)"; no scenario uses it. | Reword to the one "None" row. | fixed |
| 6 | low | structure | Chapter 5, `hamcrest-matcher-generator` table row is one cell of about 4000 characters and repeats the Hamcrest whitebox section below. | Move detail to the whitebox section. | deferred (large edit, no wrong statement) |
| 7 | low | structure | Chapter 7: `Required Checks`, `Release Pipeline`, `Credentials` sit as siblings of "Infrastructure Level 1/2" although they are not infrastructure levels. | Regroup under a "Release and CI" heading. | deferred |
| 8 | info | accuracy | ADR 0001 amendment lists the internal `core.model` types without `TypeHierarchy` (chapter 5 names it as a helper). Not a contradiction: it is no type passed to a backend. | none | rejected |
| 9 | info | accuracy | The `GenerationContext` list in chapter 5 follows the record order, not a grouping of the reporting channels. | none | rejected |

Checked and correct: chapters 5, 6, 8 and 9 for `ProcessorDiagnostics`, `GeneratedSourceWriter`, `TypeHierarchy`,
`ReferencePresetPlanner`, `PropertyRead`/`PropertyAccess`; ADR 0001, 0006, 0007 (method names exist, `CoreDependencyRulesTest`
exists); module list, JaCoCo/PIT module lists, `docs.yml` triggers, 20 `extra-files` poms; glossary and chapter 12 in sync
(`check-domain-docs-sync.py` passes); no term in `GLOSSARY.md` names a renamed or removed class.

## Not done headlessly

- PNG screenshots of every view shown to the owner (no SVG renderer in the sandbox: no rsvg, cairosvg, browser).
- `Docs` workflow run on `main` after the last fix (live site).
- Ticket comment and closing: the orchestrator.

## For later tickets

The CoreComponents view now has 10 components; check it on the rendered page before the owner's screenshot round.
