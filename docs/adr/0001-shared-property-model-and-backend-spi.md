# Shared property model with separate library backends behind an SPI

6.0.0 generates assertions for two libraries (Hamcrest and AssertJ) from one configuration. We split the processor into `…-core` (property model, type discovery, configuration, diagnostics; no Hamcrest or AssertJ on its classpath), one backend module per library (`hamcrest-matcher-generator`, an AssertJ-named sibling), and `…-annotations`. Backends depend only on core, never core on backends or backends on each other; Maven Enforcer `bannedDependencies` and an ArchUnit test enforce this. Each backend implements a `GeneratorBackend` interface (`TypeModel` → generated source files) registered via `ServiceLoader`; every backend on the processor path runs for every configured type, so the user chooses libraries by choosing artifacts. The configuration annotation carries nothing library-specific; the 5.x `@MatcherConfiguration` stays as a deprecated, fully equivalent alias at least through 6.x.

The rejected alternative was one processor with library switches in the configuration: simpler to ship, but it couples Hamcrest and AssertJ code and forces both libraries onto every user's processor path.

## Consequences

- Kotlin getter fixes and record detection live once, in core, and benefit both backends.
- The migration runs behind the existing tests: the compile-testing ITs are ported to Java first and pointed at the Hamcrest backend, the existing end-to-end modules stay green as its acceptance criterion, and the AssertJ backend gets the same two test layers.
- The Kotlin DSL backend is the one exception to "backends never depend on each other" (it builds on the Hamcrest backend), and it widens the SPI's return type to a Java or Kotlin source file; see [ADR 0004](0004-kotlin-dsl-backend-on-top-of-hamcrest.md).
