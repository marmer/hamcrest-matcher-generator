# Shared property model with separate library backends behind a `GeneratorBackend` interface

6.0.0 generates assertions for two libraries (Hamcrest and AssertJ) from one configuration. We split the processor into `…-core` (property model, type discovery, configuration, diagnostics; no Hamcrest or AssertJ on its classpath), one backend module per library (`hamcrest-matcher-generator`, `assertj-assertion-generator`), and `…-annotations`. Backends depend only on core, never core on backends or backends on each other; Maven Enforcer `bannedDependencies` and an ArchUnit test enforce this. Each backend implements a `GeneratorBackend` interface (`TypeModel` → a list of generated source files, because the AssertJ backend writes two classes per type, `AbstractXAssert` and `XAssert`) registered via `ServiceLoader`; every backend on the processor path runs for every configured type, so the user chooses libraries by choosing artifacts. The configuration annotation carries nothing library-specific; the 5.x `@MatcherConfiguration` stays as a deprecated, fully equivalent alias at least through 6.x.

The rejected alternative was one processor with library switches in the configuration: simpler to ship, but it couples Hamcrest and AssertJ code and forces both libraries onto every user's processor path.

## Consequences

- Kotlin getter fixes and record detection live once, in core, and benefit both backends.
- The migration runs behind the existing tests: the compile-testing ITs are ported to Java first and pointed at the Hamcrest backend, the existing end-to-end modules stay green as its acceptance criterion, and the AssertJ backend gets the same two test layers.
- The Kotlin DSL backend is the one exception to "backends never depend on each other" (it builds on the Hamcrest backend), and it widens the `GeneratorBackend` interface's return type to Java or Kotlin source files; see [ADR 0004](0004-kotlin-dsl-backend-on-top-of-hamcrest.md).

## Amendment (2026-11-11): the backend interface is internal

The `GeneratorBackend` interface and the types it passes or returns (`GenerationContext`, `GeneratedSource`, and the property model of package `core.model`: `TypeModel`, `PropertyModel`, `Nullability`) are internal to the project. The project's own backends are their users. They carry no stability promise and may change in any release, a patch release included. The reason is the one given in [ADR 0006](0006-files-written-once-and-backend-spi-channels.md): the interface grew with the AssertJ backend, and it may grow again. ADR 0006 calls the interface "published" and says external backends must adapt; this amendment takes precedence over that wording, since no external backend is promised a stable interface. The package Javadoc of `core.spi` and `core.model` and arc42 chapter 5 state the same rule.
