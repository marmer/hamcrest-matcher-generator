# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Every check in the right order (Spotless, install, verify on JDK 21 and 17, docs sync); --latest, --release, --fresh, --only <step>
scripts/agents/verify.sh

# Full build (dev profile: adds the end-to-end modules unless -DskipEndtoend is set); verify also runs the integration tests
./mvnw clean install
./mvnw clean verify
./mvnw -DskipEndtoend clean verify

# Docs module (docs profile, JDK 21+ only; a JDK 17 build skips it)
./mvnw -pl docs verify
# Same build as CI's Build job (no tests; run it after changing a pom or .github/workflows/)
./mvnw -B clean install -DskipTests
# Mutation testing (profile mutation, JDK 21+, about 10 min); scripts/ci/pit-scores.py prints the per-module score
./mvnw verify -Pmutation
# Release profile: adds GPG signing with the BC signer, sources and javadoc jars via `maven-javadoc-plugin`, CycloneDX SBOM; publishes via the Central Portal on `deploy`, needs MAVEN_GPG_KEY/MAVEN_GPG_PASSPHRASE and the `central` server token
# Release dry run (verify only, no GPG, no deploy), then check the bundle contents
./mvnw -B -P release verify -DskipTests -Dgpg.skip=true 2>&1 | tee release-dry-run.log
scripts/ci/check-release-artifacts.sh

# Other JDKs in cloud sessions (the SessionStart hook installs Temurin 17 and the newest GA)
JAVA_HOME=~/.jdks/17 ./mvnw -o clean verify
JAVA_HOME=~/.jdks/latest ./mvnw -o clean verify

# Skip the JDK 21+ static-analysis profile (Spotless, Error Prone, NullAway) for a faster local build
./mvnw -o clean install -DskipStaticAnalysis=true

# Windows with jabba-managed JDK
JAVA_HOME="/c/Users/MarianoMertinat/.jabba/jdk/corretto@17" PATH="$JAVA_HOME/bin:$PATH" ./mvnw clean install

# Single unit test class, single integration test class
./mvnw test -pl hamcrest-matcher-generator -Dtest=ClassName
./mvnw failsafe:integration-test -pl hamcrest-matcher-generator -Dit.test=ClassName
```

Maven profiles and the module list live in the root `pom.xml` (`<modules>`, `<profiles>`); read them there.

## Module Structure

The published modules are listed in the root pom's `<modules>`. Their roles: `matcher-generator-annotations` (`@GenerateMatchers` and the deprecated 5.x alias `@MatcherConfiguration`, the only type left in the old package), `matcher-generator-core` (property model, type discovery, diagnostics, `GeneratorBackend` SPI and the processor entry point; no Hamcrest/AssertJ, enforced by Enforcer `bannedDependencies` and an ArchUnit test), `hamcrest-matcher-generator` (the Hamcrest backend, registered via `ServiceLoader`, plus the `BeanPropertyMatcher` runtime library), `assertj-assertion-generator` (the AssertJ backend: `AbstractXAssert`/`XAssert` per type, plus the small `AssertionPath` runtime; depends only on core, enforced by Enforcer `bannedDependencies` and an ArchUnit test) and `hamcrest-matcher-generator-kotlin` (the opt-in Kotlin DSL backend: one `<Type>MatcherDsl` Kotlin file per type, delegating to the Hamcrest matcher, so it is the single backend-to-backend dependency, on the Hamcrest backend; ADR 0004). `hamcrest-matcher-generator-dependencies` and `hamcrest-matcher-generator-annotationprocessor` are code-free bridge jars for 5.x users: jar packaging, a dependency on `hamcrest-matcher-generator` at `${project.version}`, no relocation (ADR 0002; `BridgeJarIT` in each); the parent is `matcher-generator-parent`. Each published jar sets `Automatic-Module-Name` to its package root (`AutomaticModuleNameIT`). The `*-endtoend-*` modules form one reactor group through the aggregator `hamcrest-matcher-generator-endtoend/pom.xml` (their parent, `maven.deploy.skip`, and `-Xlint:all -Werror` for their compilation). A new published backend module needs: the root `<modules>`, `scripts/ci/check-release-artifacts.sh`, `.github/workflows/ci-build.yml` (the `mkdir` source-dir line, the Sonar `--projects` list and the PIT `--expect` list of the `Mutation testing` job), a `mutation` profile in its pom, the core Enforcer exclude list, `AutomaticModuleNameIT` and `LicenseFilesIT`, a `src/test/resources` directory for the golden IT, and for its end-to-end module the aggregator and the release `excludeArtifact` list. The exception is `hamcrest-matcher-generator-endtoend-plain-java-minimal` and `hamcrest-matcher-generator-endtoend-java-records`, which compile with `-Xlint:all,-processing -Werror`: javac's processing lint flags the test sources' own annotations (JUnit, JetBrains) that no processor claims, not generated code, and their poms say so; the root `dev` profile adds that group unless `-DskipEndtoend` is set. The Maven example modules are `example-maven-java-hamcrest`, `example-maven-java-assertj` and `example-maven-kotlin`.
- They are documentation examples: parent `matcher-generator-parent`, in the root `<modules>` unconditionally, `maven.deploy.skip`, listed in the release `excludeArtifact` list.
- Their tagged regions feed the docs and README.
- Like the two named exceptions, they compile with `-Xlint:all,-processing -Werror`.
- `example-maven-kotlin` runs the processors through KAPT with explicit `<sourceDirs>`.

`example-gradle-java` is the Gradle example, outside the reactor.
- It has its own wrapper, with `distributionSha256Sum` pinned; Renovate updates the wrapper.
- The CI job `example-gradle` builds it after `mvn install`.
- It resolves the project's own artifacts from `mavenLocal()` only, so a missing install fails the build.
- release-please bumps its version through `release-please-config.json` (`extra-files`, marker `x-release-please-version`).

## Architecture: Generation Flow

The round-by-round flow (annotation, processor, worker, `TypeModel`, backends, written sources) is arc42 chapter 06, `docs/src/docs/asciidoc/architecture/_chapters/06_runtime_view.adoc`; read it there. The Hamcrest backend lives in package `io.github.marmer.matchergenerator.hamcrest`; its generated `<TypeName>Matcher` extends `TypeSafeMatcher<T>` and wraps a `BeanPropertyMatcher<T>`. The AssertJ backend (`io.github.marmer.matchergenerator.assertj`) generates an `AbstractXAssert<SELF, ACTUAL>` and a concrete `XAssert` per type.

**Generated matcher API** (per property `foo`):
```java
isSomePojo()
    .withFoo("value")         // equality shorthand
    .withFoo(is("value"))     // Hamcrest matcher overload
    .resetFoo()               // remove this property check (v5.1.0)
    .ignoringFoo()            // count this property as addressed by the completeness check
    .checkingAllProperties()  // opt this matcher into the completeness check (off by default)
isSomePojoLike(reference)     // presets every property deeply from a reference object, turns the completeness check on; withFoo/resetFoo/ignoringFoo override it
```

**Generated AssertJ API** (per property `foo`, package `io.github.marmer.matchergenerator.assertj`):
```java
XAssert.assertThat(x)
    .hasFoo("value")                       // deep equality, "Expecting X.foo to be: ... but was: ..."
    .hasFooSatisfying(foo -> FooAssert.assertThat(foo).hasBar(1))   // path X.foo.bar
    .isFoo() / .isNotFoo()                 // boolean properties only
    .ignoringFoo()                         // count foo as addressed by the completeness check
    .hasAllPropertiesChecked()             // every property of the chain checked or ignored
    .satisfiesCheckingAllProperties(a -> a.hasFoo(1).ignoringBar())   // same check, scoped to the lambda
assertThat(x).extracting(X::getFoo, as(FOO))   // FOO: public static InstanceOfAssertFactory<Foo, FooAssert> on every XAssert
// per target package one generated mixin `SoftAssertionsProvider` (extends AssertJ's) with default assertThat(X)/then(X) per type; a SoftAssertions subclass implements it
// per target package one generated `PropertyRepresentation extends StandardRepresentation`; the user registers it with `Assertions.useRepresentation(..)`, never automatically
```
The backend's golden ITs are `AssertionGenerationIT` (`-Dit.test=AssertionGenerationIT`, goldens under `assertj-assertion-generator/src/test/resources/.../AssertionGenerationIT/<scenario>/`); the end-to-end module is `hamcrest-matcher-generator-endtoend-assertj`.

**Generated Kotlin DSL** (opt-in `hamcrest-matcher-generator-kotlin`, KAPT only, one `<Type>MatcherDsl` Kotlin file per type; scope classes carry `@MatcherDsl`):
```kotlin
isOrder { id(42L); customer { name("Ada") }; checkingAllProperties() }   // ignoringName() counts a property as addressed
```

## KAPT Configuration (Kotlin end-to-end modules)

`hamcrest-matcher-generator-endtoend-mixed-kotlin-java` and `hamcrest-matcher-generator-endtoend-plain-kotlin` run the processor through the `kotlin-maven-plugin` `test-kapt` execution into `target/generated-sources/kapt/test/`. A `build-helper-maven-plugin` `add-test-source` registers that directory as a test source root, so javac compiles every generated matcher, not only the ones Java tests reference (#110). The poms are the source of truth.

## Testing Approach

Annotation processor tests (file `MatcherGenerationProcessorIT.java` in `hamcrest-matcher-generator/src/test/java`, class `MatcherGenerationProcessorIT`: use the class name for `-Dit.test`) use **Google Compile Testing**: they compile in-memory Java source strings and assert on the generated output. Expected generated sources are **golden files** under `src/test/resources/io/github/marmer/matchergenerator/hamcrest/MatcherGenerationProcessorIT/<scenario>/`, compared character for character: `assertGeneratedSource` reads the golden file and `assertEquals`s it against the generated file's `getCharContent(true)`, so any whitespace difference fails. This runs as a Maven Failsafe integration test.

Core processor tests (`CoreProcessorIT` in `matcher-generator-core`, run it with `-pl matcher-generator-core -Dit.test=CoreProcessorIT`) use the same harness with a **recording test backend** (registered via `META-INF/services` in the core test resources) and assert on the recorded `TypeModel` instead of generated Hamcrest code.

Runtime tests (`BeanPropertyMatcherTest.java`) are plain JUnit 5 unit tests. Hamcrest mismatch messages are tested against golden `.txt` files in `BeanPropertyMatcherMessageTest` (and `PropertyDescriptionTest` for value rendering).

Regenerate golden files with `-Dgolden.update=true` and review `git diff --stat` and the diff of `src/test/resources` (it also rewrites unrelated goldens when other output changed); never edit them by script, and never revert them with `git checkout` (that drops hand edits too): regenerate. After a scripted change `git diff --numstat` shows 0 deleted lines when only additions were meant. Hamcrest goldens: reinstall core first (`-pl hamcrest-matcher-generator` reads core from `~/.m2`). JavaPoet quirks in the AssertJ goldens: `$W` leaves a trailing space after `=` (leave long field initializers unwrapped) and the output carries `import java.lang.X;` lines.

The plain-kotlin end-to-end module checks compile failures through maven-invoker consumer builds under `src/it` (`right-scope`, `wrong-scope`, `wrong-value-type`, BeanShell `postbuild.bsh` hooks); `-DskipTests` skips them, and they must pass on a fresh local repository (`scripts/agents/verify.sh --only fresh-repo`).

End-to-end modules are the final validation layer: they apply `@GenerateMatchers` to real POJOs and exercise the generated matchers in actual test assertions. `endtoend-5x-compatibility` is the exception: it is the 5.x suite (old annotation, old artifact coordinates), frozen except for the owner-approved deviations (version references filtered by the consumer build, `RelocationPomTest` deleted with #159), with the tree hash as the guard; it must keep compiling and passing. It is not a reactor module: `endtoend-5x-compatibility-runner` runs it with `maven-invoker-plugin` as a consumer build. `invoker:install` puts the reactor artifacts (both bridge jars, the backend they depend on and the parent) into `endtoend-5x-compatibility-runner/target/local-repo`, and the suite resolves the 5.x coordinates from there like a real 5.x user would, with the project's own compiler plugin; the outer local repository is its only remote (`src/it/settings.xml`). Only the suite's parent and own version are `@project.version@`, which invoker filters when it clones the module (so it does not build in place), and its dependency versions follow `${project.version}`; nothing else in the module changes. `-DskipTests` compiles the suite without running it. Run it alone with `./mvnw verify -pl hamcrest-matcher-generator-endtoend-5x-compatibility-runner` after a full `install`. The suite runs every test it has; `RelocationPomTest` was deleted with the owner's approval (#159) because it asserted the former relocation POMs and cannot hold for bridge jars, whose shape `BridgeJarIT` checks. A suite that is a reactor module with the old coordinates would be built against the reactor's own bridge modules, not what a consumer resolves.

## Build pitfalls

Read `docs/agents/build-pitfalls.md` when a build fails (HTTP 429, empty `~/.m2`), when building or testing, changing poms, CI or workflows, editing `.claude/` settings or hooks, or merging or committing after a squash merge.

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues (marmer/hamcrest-matcher-generator), managed via `gh api` (REST) or `scripts/agents/issue.py`; `gh issue`/`gh pr` fail in cloud sessions (GraphQL blocked). See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root (created lazily). The human-facing arc42 docs (`docs/src/docs/asciidoc/architecture/index.adoc`, chapters in `docs/src/docs/asciidoc/architecture/_chapters/`, built by the `docs` Maven module) must be kept in sync with them. See `docs/agents/domain.md`.

### Implementation process

Implementation tickets name their implementer model (default Haiku, escalated by the orchestrator on poor quality) and are commissioned one after another unless the orchestrator is sure they will not conflict (other work may run in parallel). PR bodies explain changes visually in `show-me` style where that helps, and every session's final report asks the user to run `/retro` (user-invoked only). See `docs/agents/implementation.md`.
