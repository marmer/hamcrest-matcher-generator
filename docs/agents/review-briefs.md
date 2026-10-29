# Review briefs

The two review prompts that `code-review` (Standards and Spec axes) runs as sub-agents, and how the orchestrator triages what they return. Give each the fixed point (`origin/main` for a ticket, the session's first base for a session-end review) and the ticket or spec number.

## Standards

> Review `git diff <fixed point>...HEAD` against this repo's standards: `CLAUDE.md`, `docs/agents/`, `GLOSSARY.md`, `docs/adr/`. Judge the diff against Fowler's 12 smells as judgement calls (duplicated code, long method, large class, long parameter list, divergent change, shotgun surgery, feature envy, data clumps, primitive obsession, speculative generality, message chains, comments covering for unclear code); where the repo overrides a smell, the repo wins. Skip what tooling enforces (Spotless, Error Prone, NullAway, JaCoCo, the docs sync check). Report under 400 words, one finding per line with file and line.

## Spec

> Fetch the ticket and its comments (`gh api repos/marmer/hamcrest-matcher-generator/issues/<n>` plus `/comments`); grep the spec only as needed. Compare `git diff <fixed point>...HEAD` with them and report: missing or partial criteria, scope creep, and implemented-but-wrong behaviour. Quote the spec line each finding rests on. Report under 400 words.

## Triage of findings

Findings go back to the implementer when they are correctness gaps, missing tests, docs-sync gaps or standard violations. Refactorings beyond the ticket and style nits stay as they are, unless they sit in lines the PR changes anyway; the follow-up names which findings to fix and which stay, with the reason. Ask the user only about findings that need their decision.
