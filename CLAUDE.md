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
./mvnw test -pl hamcrest-matcher-generator -Dtest=ClassName

# Single integration test class
./mvnw failsafe:integration-test -pl hamcrest-matcher-generator -Dit.test=ClassName

# Release build (adds GPG signing with the BC signer, sources and javadoc jars via `maven-javadoc-plugin`, CycloneDX SBOM; publishes via the Central Portal on `deploy`, needs MAVEN_GPG_KEY/MAVEN_GPG_PASSPHRASE and the `central` server token)
./mvnw verify -P release    # dry run, no publishing
```

Maven profiles and the module list live in the root `pom.xml` (`<modules>`, `<profiles>`); read them there.

## Module Structure

The published modules are `matcher-generator-annotations` (`@GenerateMatchers` and the deprecated 5.x alias `@MatcherConfiguration`, the only type left in the old package), `matcher-generator-core` (property model, type discovery, diagnostics, `GeneratorBackend` SPI and the processor entry point; no Hamcrest/AssertJ, enforced by Enforcer `bannedDependencies` and an ArchUnit test) and `hamcrest-matcher-generator` (the Hamcrest backend, registered via `ServiceLoader`, plus the `BeanPropertyMatcher` runtime library). `hamcrest-matcher-generator-dependencies` and `hamcrest-matcher-generator-annotationprocessor` are code-free bridge jars for 5.x users: jar packaging, a dependency on `hamcrest-matcher-generator` at `${project.version}`, no relocation (ADR 0002; `BridgeJarIT` in each); the parent is `matcher-generator-parent`. Each published jar sets `Automatic-Module-Name` to its package root (`AutomaticModuleNameIT`). Every `*-endtoend-*` module applies the processor to real POJOs/records and exercises the generated matchers; the current set is in the root `pom.xml`.

## Architecture: Generation Flow

The round-by-round flow (annotation, processor, worker, `TypeModel`, backends, written sources) is arc42 chapter 06, `docs/src/docs/asciidoc/architecture/_chapters/06_runtime_view.adoc`; read it there. The Hamcrest backend lives in package `io.github.marmer.matchergenerator.hamcrest`; its generated `<TypeName>Matcher` extends `TypeSafeMatcher<T>` and wraps a `BeanPropertyMatcher<T>`.

**Generated matcher API** (per property `foo`):
```java
isSomePojo()
    .withFoo("value")         // equality shorthand
    .withFoo(is("value"))     // Hamcrest matcher overload
    .resetFoo()               // remove this property check (v5.1.0)
```

## KAPT Configuration (Kotlin end-to-end modules)

`hamcrest-matcher-generator-endtoend-mixed-kotlin-java` and `hamcrest-matcher-generator-endtoend-plain-kotlin` run the processor through the `kotlin-maven-plugin` `test-kapt` execution into `target/generated-sources/kapt/test/`. A `maven-antrun-plugin` `move` at `process-test-sources` relocates the matchers to `target/generated-test-sources/test-annotations`, where javac compiles them. Keep that order; the poms are the source of truth.

Open gap: javac compiles only the generated matchers that test sources reference; see #110.

## Testing Approach

Annotation processor tests (file `MatcherGenerationProcessorIT.java` in `hamcrest-matcher-generator/src/test/java`, class `MatcherGenerationProcessorIT`: use the class name for `-Dit.test`) use **Google Compile Testing**: they compile in-memory Java source strings and assert on the generated output. Expected generated sources are **golden files** under `src/test/resources/io/github/marmer/matchergenerator/hamcrest/MatcherGenerationProcessorIT/<scenario>/`, compared character for character: `assertGeneratedSource` reads the golden file and `assertEquals`s it against the generated file's `getCharContent(true)`, so any whitespace difference fails. This runs as a Maven Failsafe integration test.

Core processor tests (`CoreProcessorIT` in `matcher-generator-core`, run it with `-pl matcher-generator-core -Dit.test=CoreProcessorIT`) use the same harness with a **recording test backend** (registered via `META-INF/services` in the core test resources) and assert on the recorded `TypeModel` instead of generated Hamcrest code.

Runtime tests (`BeanPropertyMatcherTest.java`) are plain JUnit 5 unit tests.

End-to-end modules are the final validation layer: they apply `@GenerateMatchers` to real POJOs and exercise the generated matchers in actual test assertions. `endtoend-5x-compatibility` is the exception: it is an unchanged 5.x suite (old annotation, old artifact coordinates) and must keep compiling and passing without edits. It is not a reactor module: `endtoend-5x-compatibility-runner` runs it with `maven-invoker-plugin` as a consumer build. `invoker:install` puts the reactor artifacts (both bridge jars, the backend they depend on and the parent) into `endtoend-5x-compatibility-runner/target/local-repo`, and the suite resolves the 5.x coordinates from there like a real 5.x user would, with the project's own compiler plugin; the outer local repository is its only remote (`src/it/settings.xml`). Only the suite's parent and own version are `@project.version@`, which invoker filters when it clones the module (so it does not build in place), and its dependency versions follow `${project.version}`; nothing else in the module changes. `-DskipTests` compiles the suite without running it. Run it alone with `./mvnw verify -pl hamcrest-matcher-generator-endtoend-5x-compatibility-runner` after a full `install`. The runner passes `-Dtest=!RelocationPomTest` to the suite: that test asserts the former relocation POMs and cannot hold for bridge jars, so the suite runs 10 of its 12 tests; `BridgeJarIT` checks the bridge shape. A suite that is a reactor module with the old coordinates would be built against the reactor's own bridge modules, not what a consumer resolves.

## Known Pitfalls

- **compile-testing on Java 17** requires `--add-opens=jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED` in Failsafe `<argLine>` — configured once in the root pom's `pluginManagement` for `maven-failsafe-plugin` (next to the `module.jar` property), so no module repeats it.
- **Unresolvable names fail the build for `@GenerateMatchers`**: one error per name. The deprecated `@MatcherConfiguration` only warns, as in 5.x, so keep configured names in `@GenerateMatchers` resolvable. Set `-Amatchergenerator.verbose` to see one note per skipped property with its reason.
- **`docs` module needs JDK 21+**: it is only built when the root profile `docs` (`<jdk>[21,)</jdk>`) is active, so a JDK 17 build skips it silently.
- **`maven-compiler-plugin` before 3.12 does not follow a relocation in `annotationProcessorPaths`** (fails with "Missing: ...annotationprocessor:jar"): that is why the 5.x artifacts are bridge jars, not relocation POMs (ADR 0002). Do not turn them back into pom-only relocations. The root pom keeps 3.8.1 (`maven-compiler-plugin.version`; newer versions break the Kotlin end-to-end modules, see #110) and the 5.x consumer build uses it too.
- **JUnit Platform version mismatch**: Surefire 3.5.2 needs `junit-platform-launcher:1.12.1` explicitly on the classpath — already in root pom.
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
