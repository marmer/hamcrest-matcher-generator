# Review briefs

The review prompts: two that `code-review` (Standards and Spec axes) runs as sub-agents, and the delta check that closes a fix. Also how the orchestrator triages what they return. Give each the fixed point (`origin/main` for a ticket, the session's first base for a session-end review, the commit the review looked at for the delta check) and the ticket or spec number.

## Standards

> Review `git diff <fixed point>...HEAD` against this repo's standards: `CLAUDE.md`, `docs/agents/`, `GLOSSARY.md`, `docs/adr/`. Judge the diff against Fowler's 12 smells as judgement calls (duplicated code, long method, large class, long parameter list, divergent change, shotgun surgery, feature envy, data clumps, primitive obsession, speculative generality, message chains, comments covering for unclear code); where the repo overrides a smell, the repo wins. Skip what tooling enforces (Spotless, Error Prone, NullAway, JaCoCo, the docs sync check). Report under 400 words, one finding per line with file and line.

## Spec

> Fetch the ticket and its comments (`gh api repos/marmer/hamcrest-matcher-generator/issues/<n>` plus `/comments`); grep the spec only as needed. Compare `git diff <fixed point>...HEAD` with them and report: missing or partial criteria, scope creep, and implemented-but-wrong behaviour. Quote the spec line each finding rests on. Report under 400 words.

## Delta check

Run by a fresh agent (see the principle in `docs/agents/implementation.md`) after the implementer has fixed the findings. Give it the ticket number `<n>` with the reviewed commit and the findings; for a session-end or spec-wide review, give the spec number instead, and for a fix with no ticket or spec (e.g. a retro fix), drop the criteria sentence from the brief.

> Review `git diff <reviewed commit>..HEAD`, where `<reviewed commit>` is the HEAD the review looked at. This is a delta check, not a full re-review: check only the findings below and the lines this diff changes. For each finding, report fixed or not fixed, quoting its file:line. Then report new defects in the changed lines, one per line with file and line. Fetch the ticket and its comments (`gh api repos/marmer/hamcrest-matcher-generator/issues/<n>` plus `/comments`), grep the spec only as needed, and report each acceptance criterion that the changed lines break, quoting the criterion. Under 300 words. Change nothing.
>
> Findings: <each finding, with file and line, as the review wrote it>

## Triage of findings

Findings go back to the implementer when they are correctness gaps, missing tests, docs-sync gaps or standard violations. Refactorings beyond the ticket and style nits stay as they are, unless they sit in lines the PR changes anyway; the follow-up names which findings to fix and which stay, with the reason. A finding that stays has one of two fates: dropped with its reason, or a follow-up ticket when it needs an owner decision, exceeds the ticket or is a real defect the ticket did not cover. The orchestrator writes that ticket by hand rather than only mentioning it in the report: the ticket shape of `.agents/skills/to-tickets/SKILL.md`, acceptance criteria per `docs/agents/implementation.md`, `Part of #<spec>` or a sub-issue link, then the graph step of the follow-up rule under Blocking in `docs/agents/issue-tracker.md`. When several follow-ups accumulate or their cut is unclear, the final report asks the owner to run `/to-tickets` (user-invoked only). Ask the user only about findings that need their decision. Triage ends with the delta check above, on the fixes it sends back.
