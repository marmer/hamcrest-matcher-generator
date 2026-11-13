# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Every check in the right order (Spotless, install, verify on JDK 21 and 17, docs sync); --latest, --release, --fresh, --only <step>
scripts/agents/verify.sh

# Builds run in parallel by default (.mvn/maven.config, -T 1C); the release, release dry run, mutation and Sonar builds pin -T 1.

# Full build (dev profile: adds the end-to-end modules unless -DskipEndtoend is set); verify also runs the integration tests
./mvnw clean install
./mvnw clean verify
./mvnw -DskipEndtoend clean verify

# Docs module (docs profile, JDK 21+ only; a JDK 17 build skips it)
./mvnw -pl docs verify
# Same build as CI's Build job (no tests; run it after changing a pom or .github/workflows/)
./mvnw -B clean install -DskipTests -DskipITs
# Gradle example (outside the reactor, own wrapper; CI job Example Gradle)
./mvnw -DskipEndtoend -DskipTests -DskipITs install
(cd example-gradle-java && ./gradlew build)
# Version consistency; run it after changing a pom or release-please-config.json
python3 scripts/ci/check-project-version.py
# Mutation testing (profile mutation, JDK 21+, about 10 min); scripts/ci/pit-scores.py prints the per-module score
./mvnw -T 1 verify -Pmutation
# Release dry run (verify only, no GPG, no deploy), then check the bundle contents
./mvnw -B -T 1 -P release verify -DskipTests -DskipITs -Dgpg.skip=true 2>&1 | tee release-dry-run.log
scripts/ci/check-release-artifacts.sh

# Other JDKs in cloud sessions (the SessionStart hook installs pinned Temurin 17 and 27 releases, updated by hand: MAINTAINING.md)
JAVA_HOME=~/.jdks/17 ./mvnw -o clean verify
JAVA_HOME=~/.jdks/latest ./mvnw -o clean verify

# Skip the JDK 21+ static-analysis profile (Spotless, Error Prone, NullAway) for a faster local build
./mvnw -o clean install -DskipStaticAnalysis=true

# Single unit test class, single integration test class
./mvnw test -pl hamcrest-matcher-generator -Dtest=ClassName
./mvnw failsafe:integration-test -pl hamcrest-matcher-generator -Dit.test=ClassName
```

Maven profiles and the module list live in the root `pom.xml` (`<modules>`, `<profiles>`); read them there.

README snippets are copied from the tagged regions of the example modules by `python3 scripts/ci/sync-readme-snippets.py` (run it without arguments after changing an example; the `--check` form fails the `Domain docs in sync` job when they are stale).

## Reference pointers

- Read `docs/agents/modules.md` when adding, moving or renaming a module, changing a module pom or `release-please-config.json`, or touching an example module, the Gradle example or a 5.x bridge jar.
- Read `docs/agents/kapt.md` when changing the processor setup (KAPT) of a Kotlin end-to-end module or `example-maven-kotlin`.
- Read `docs/agents/testing.md` when adding or changing a test, regenerating golden files, running the Jazzer fuzz test or changing an end-to-end module.
- Read `docs/agents/5x-compatibility.md` before touching `hamcrest-matcher-generator-endtoend-5x-compatibility`, its runner or anything that could change the frozen suite's tree.
- Read `docs/src/docs/asciidoc/architecture/_chapters/06_runtime_view.adoc` when tracing a generation round from annotation to written sources.
- Read the user guide in `docs/src/docs/asciidoc/guide/` (`hamcrest-backend.adoc`, `assertj-backend.adoc`, `kotlin.adoc`) for the generated matcher, assertion and Kotlin DSL API.

## Build pitfalls

Read `docs/agents/build-pitfalls.md` when a build fails (HTTP 429, empty `~/.m2`), when building or testing, running a build on Windows or with a non-default JDK, changing poms, CI or workflows, editing `.claude/` settings or hooks, or merging or committing after a squash merge.

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues (marmer/hamcrest-matcher-generator), managed via `gh api` (REST) or `scripts/agents/issue.py`; `gh issue`/`gh pr` fail in cloud sessions (GraphQL blocked). See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root (created lazily). The human-facing arc42 docs (`docs/src/docs/asciidoc/architecture/index.adoc`, chapters in `docs/src/docs/asciidoc/architecture/_chapters/`, built by the `docs` Maven module) must be kept in sync with them. See `docs/agents/domain.md`.

### Implementation process

Implementation tickets name no model (the orchestrator picks it: Haiku by default, escalated on poor quality) and are commissioned one after another unless the orchestrator is sure they will not conflict (other work may run in parallel). PR bodies explain changes visually in `show-me` style where that helps, and every session's final report asks the user to run `/retro` (user-invoked only), except in a session that started `/goRalph`, whose loop runs its own retro. See `docs/agents/implementation.md`.
