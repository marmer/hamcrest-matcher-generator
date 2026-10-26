# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Full build (dev profile)
./mvnw clean install

# Build + run integration tests
./mvnw clean verify

# Other JDKs in cloud sessions (the SessionStart hook installs Temurin 17 and the newest GA)
JAVA_HOME=~/.jdks/17 ./mvnw -o clean verify
JAVA_HOME=~/.jdks/latest ./mvnw -o clean verify

# Windows with jabba-managed JDK
JAVA_HOME="/c/Users/MarianoMertinat/.jabba/jdk/corretto@17" PATH="$JAVA_HOME/bin:$PATH" ./mvnw clean install

# Single unit test class
./mvnw test -pl hamcrest-matcher-generator-annotationprocessor -Dtest=ClassName

# Single integration test class
./mvnw failsafe:integration-test -pl hamcrest-matcher-generator-annotationprocessor -Dit.test=ClassName

# Release build (adds GPG signing with the BC signer, sources/Dokka javadoc jars, CycloneDX SBOM; publishes via the Central Portal on `deploy`, needs MAVEN_GPG_KEY/MAVEN_GPG_PASSPHRASE and the `central` server token)
./mvnw verify -P release    # dry run, no publishing
```

Maven profiles and the module list live in the root `pom.xml` (`<modules>`, `<profiles>`); read them there.

## Module Structure

The core modules are `hamcrest-matcher-generator-dependencies` (`@MatcherConfiguration` + `BeanPropertyMatcher` runtime base), `matcher-generator-core` (property model, type discovery, diagnostics, `GeneratorBackend` SPI and the processor entry point; no Hamcrest/AssertJ, enforced by Enforcer `bannedDependencies` and an ArchUnit test) and `hamcrest-matcher-generator-annotationprocessor` (the Hamcrest backend, registered via `ServiceLoader`; its artifact ID is renamed later). Every `*-endtoend-*` module applies the processor to real POJOs/records and exercises the generated matchers; the current set is in the root `pom.xml`.

## Architecture: Generation Flow

1. Compiler detects `@MatcherConfiguration` on a type (SOURCE retention, in `dependencies` module)
2. `MatcherGenerationProcessor.java` (core) — APT entry point, delegates immediately to the worker and loads every `GeneratorBackend` via `ServiceLoader`
3. `MatcherGenerationProcessorWorker.java` (core) — resolves configured class names or package strings to `TypeElement`s, skips already-`@Generated` types, builds an immutable `TypeModel` per type with `TypeModelFactory`, runs every backend on it and writes the returned `GeneratedSource` (Java via `createSourceFile`, Kotlin via `createResource`)
4. `HamcrestGeneratorBackend.java` / `MatcherGenerator.java` (Hamcrest backend, package `...annotationprocessing.hamcrest`) — builds a `<TypeName>Matcher` class via JavaPoet from the `TypeModel`; the generated class extends `TypeSafeMatcher<T>` and wraps an internal `BeanPropertyMatcher<T>` instance

**Generated matcher API** (per property `foo`):
```java
isSomePojo()
    .withFoo("value")         // equality shorthand
    .withFoo(is("value"))     // Hamcrest matcher overload
    .resetFoo()               // remove this property check (v5.1.0)
```

## KAPT Configuration (Kotlin end-to-end modules)

Both `endtoend-mixed-kotlin-java` and `endtoend-plain-kotlin` run the annotation processor via KAPT:

- `test-kapt` execution generates `.java` matcher files into `target/generated-sources/kapt/test/`
- `build-helper-maven-plugin:add-test-source` registers that directory so **javac** compiles the generated matchers
- `test-compile` kotlin execution includes that dir in `sourceDirs` for Kotlin resolution

The flow must be: KAPT generates → `add-test-source` registers → javac compiles. Moving files with antrun after javac registration breaks this ordering.

## Testing Approach

Annotation processor tests (file `MatcherGenerationProcessorIT.java` in `hamcrest-matcher-generator-annotationprocessor/src/test/java`, class `MatcherGenerationProcessorIT`: use the class name for `-Dit.test`) use **Google Compile Testing**: they compile in-memory Java source strings and assert on the generated output. Expected generated sources are **golden files** under `src/test/resources/io/github/marmer/annotationprocessing/MatcherGenerationProcessorIT/<scenario>/`, compared character for character: `assertGeneratedSource` reads the golden file and `assertEquals`s it against the generated file's `getCharContent(true)`, so any whitespace difference fails. This runs as a Maven Failsafe integration test.

Core processor tests (`CoreProcessorIT` in `matcher-generator-core`, run it with `-pl matcher-generator-core -Dit.test=CoreProcessorIT`) use the same harness with a **recording test backend** (registered via `META-INF/services` in the core test resources) and assert on the recorded `TypeModel` instead of generated Hamcrest code.

Runtime tests (`BeanPropertyMatcherTest.java`) are plain JUnit 5 unit tests.

End-to-end modules are the final validation layer: they apply `@MatcherConfiguration` to real POJOs and exercise the generated matchers in actual test assertions.

## Known Pitfalls

- **compile-testing on Java 17** requires `--add-opens=jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED` in Failsafe `<argLine>` — already configured once in the root pom's `pluginManagement` for `maven-failsafe-plugin`.
- **`docs` module needs JDK 21+**: it is only built when the root profile `docs` (`<jdk>[21,)</jdk>`) is active, so a JDK 17 build skips it silently.
- **JUnit Platform version mismatch**: Surefire 3.5.2 needs `junit-platform-launcher:1.12.1` explicitly on the classpath — already in root pom.
- **KAPT generated sources not found**: use `build-helper-maven-plugin:add-test-source` to register `target/generated-sources/kapt/test` *before* the compile phase; do not use antrun to move files.
- **JDK 25 implicit annotation processing disabled**: `@AutoService` was removed from `MatcherGenerationProcessor.java`; a manual `META-INF/services/javax.annotation.processing.Processor` resource is used instead. The `endtoend-plain-java-minimal` module uses `<annotationProcessorPaths>` to declare the AP explicitly.
- **Lombok on newer JDKs**: each JDK needs a Lombok release that supports it (JDK 25 ≥ 1.18.44, JDK 27 ≥ 1.18.48); an unsupported JDK fails in javac with `ExceptionInInitializerError: com.sun.tools.javac.tree.EndPosTable`.
- **Agent edits to `.claude/settings*.json` are blocked** in auto mode (Self-Modification): give the owner the exact snippet to add instead of loading `update-config`.
- **Maven Central HTTP 429** (rate limit, frequent with parallel subagents sharing `~/.m2`): Maven may store the 429 error text as a `.pom` or `.jar` in `~/.m2/repository`, failing later builds ("Non-parseable POM", `NoClassDefFoundError`). Delete that version directory and rebuild offline with `-o`; the SessionStart hook pre-fetches dependencies for that and logs to /tmp/session-start.log; if ~/.m2 is empty, read that log first.

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues (marmer/hamcrest-matcher-generator), managed via `gh api` (REST) or `scripts/agents/issue.py`; `gh issue`/`gh pr` fail in cloud sessions (GraphQL blocked). See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root (created lazily). The human-facing arc42 docs (`docs/src/docs/asciidoc/architecture/index.adoc`, chapters in `docs/src/docs/asciidoc/architecture/_chapters/`, built by the `docs` Maven module) must be kept in sync with them. See `docs/agents/domain.md`.

### Implementation process

Implementation tickets name their implementer model (default Haiku, escalated by the orchestrator on poor quality), PR bodies explain changes visually in `show-me` style where that helps, and every session's final report asks the user to run `/retro` (user-invoked only). See `docs/agents/implementation.md`.
