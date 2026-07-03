# Modernization Plan — hamcrest-matcher-generator 6.0.0

This document captures the agreed target picture for the 6.0.0 modernization
(interview conducted 2026-07-03). It is the reference for implementation and
for the release notes / migration guide.

## Agreed decisions

| # | Topic | Decision |
|---|-------|----------|
| 1 | Versioning | **Major release 6.0.0** — breaking changes are allowed. |
| 2 | Java baseline | **Java 17** minimum, for both the annotation processor and the code it generates. Java 11 users stay on 5.x. |
| 3 | Release infrastructure | Full migration in scope: **OSSRH → Sonatype Central Portal** (`central-publishing-maven-plugin` replaces `nexus-staging-maven-plugin`), remove the dead JCenter repositories, verify/adjust the GitHub Actions release flow. Without this, 6.0.0 cannot be published at all. |
| 4 | Hamcrest | Build against **Hamcrest 3.0**. |
| 5 | Kotlin support | **Keep KAPT** (works with Kotlin 2.x/K2, maintenance mode is acceptable); keep both Kotlin end-to-end modules; sharpen the documentation. No KSP rewrite in this release. |
| 6 | Generated code | **Self-contained matchers**: the generator emits the `BeanPropertyMatcher` logic directly into the generated code (nested static class). At runtime, users only need Hamcrest. |
| 7 | Artifact structure | **Single artifact**: `@MatcherConfiguration` moves into `hamcrest-matcher-generator-annotationprocessor`; the `hamcrest-matcher-generator-dependencies` artifact is removed. Users add exactly one dependency. |
| 8 | Mismatch messages | **Multi-line property diff**: the expected side stays compact (class + configured expectations); the mismatch lists only the *failing* properties, one per line, each with expected vs. actual (e.g. `foo: expected "bar" but was "baz"`). |
| 9 | New features in 6.0.0 | Reference-object matcher, exclude configuration, strict mode (see below). |
| 10 | Kotlin DSL wrapper | Wanted, but **deferred to 6.1.0** as a purely additive follow-up with its own API design round. |
| 11 | Session deliverable | This plan document, committed and opened as a draft PR before implementation starts. |

## Internal dependency/tooling modernization (no user decision needed)

- **JavaPoet 1.13.0 → Palantir fork** (`com.palantir.javapoet`) — upstream is unmaintained.
- **Drop `javax.annotation-api`** — unused; the code already uses the JDK-built-in
  `javax.annotation.processing.Generated`.
- Mockito 3.11 → 5.x, mockito-kotlin current, compile-testing latest, classgraph latest.
- Maven plugins: compiler 3.8.1 → current, gpg, source, javadoc/dokka, jacoco, deploy → current.
- With the Java 17 baseline, the `endtoend-java-records` module no longer needs the
  `java16plus` profile — fold it into the default module set.
- Remove JCenter from `<repositories>`/`<pluginRepositories>` (dead since 2021).

## New feature specifications

### Reference-object matcher

```java
assertThat(actual, isSomePojoEqualTo(expected));
// equivalent to isSomePojo().withAllPropertiesOf(expected)
```

Initializes the matcher with an `equalTo` expectation for every generated property of a
reference instance. Individual checks can be overridden afterwards via `resetFoo()` /
`withFoo(...)`. Combined with the property-diff mismatch format, whole-object comparison
with a readable diff becomes a one-liner.

### Exclude configuration

```java
@MatcherConfiguration(
    value = "foo.bar.model",
    exclude = {"foo.bar.model.internal", "foo.bar.model.LegacyThing"}
)
```

`@MatcherConfiguration` gets an `exclude` attribute (package and fully-qualified class
names) so single types or sub-packages can be exempted from a package scan.

### Strict mode

```java
assertThat(actual, isSomePojo().strict().withFoo("bar"));
```

An opt-in mode that additionally fails when the matched object has generated properties
for which *no* expectation was configured — protecting tests from silently ignoring
newly added fields. The mismatch output lists the unchecked properties.

## Implementation phases

1. **Build & release infrastructure** — remove JCenter, migrate to the Central Portal,
   bump Maven plugins, raise the baseline to Java 17, fold the records module into the
   default profile, get CI green. *(Everything else is undeliverable without this.)*
2. **Dependency modernization** — Palantir JavaPoet migration, drop `javax.annotation-api`,
   Hamcrest 3.0, Mockito 5, remaining test-dependency bumps. Build stays green after each step.
3. **Artifact restructuring** — move `@MatcherConfiguration` into the annotation-processor
   artifact, make generated matchers self-contained, remove the `dependencies` module,
   adapt the end-to-end modules and the README setup instructions.
4. **Mismatch messages** — implement the multi-line property diff in the (now generated)
   matcher logic, with unit and end-to-end coverage of the output format.
5. **Features** — reference-object matcher, exclude configuration, strict mode; each with
   compile-testing ITs and end-to-end coverage; document in README.
6. **Release 6.0.0** — migration guide (see below), release via the new Central Portal
   pipeline. Afterwards: 6.1.0 Kotlin DSL (separate design round).

## Migration guide 5.x → 6.0.0 (draft for release notes)

- **Java 17 required** (processor and generated code).
- **One dependency instead of two**: remove `hamcrest-matcher-generator-dependencies`;
  keep only `hamcrest-matcher-generator-annotationprocessor` (scope `test`, or `provided`
  for production-code usage).
- **Import change**: `@MatcherConfiguration` moves to the annotation-processor artifact
  (new package/coordinates listed in the release notes).
- Generated matchers no longer reference a runtime library — only Hamcrest is needed
  at test runtime.
- Mismatch message format changed (property diff) — assertions on the old message text
  need updating.
