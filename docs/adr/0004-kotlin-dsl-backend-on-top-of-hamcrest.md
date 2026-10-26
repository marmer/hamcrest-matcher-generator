# Kotlin DSL as a Kotlin-emitting backend on top of the Hamcrest backend

6.0.0 offers Kotlin users a type-safe DSL (`isOrder { customer { name("Ada") } }`): one `@DslMarker` scope class per generated Hamcrest matcher, which delegates to that matcher. Because the wrapper is per type, it is generated, by a separate opt-in artifact `hamcrest-matcher-generator-kotlin` that is itself a backend behind the `GeneratorBackend` SPI from ADR 0001. To allow this, the SPI returns a generated source file (Java or Kotlin) instead of a `JavaFile`. The Kotlin backend may depend on the Hamcrest backend to reuse its naming (`XMatcher`, `isX`, `withFoo`, `ignoringFoo`); this is the **only** allowed backend-to-backend dependency and is written into the Enforcer and ArchUnit rules. The generator is written in Java and emits Kotlin via KotlinPoet, so production code stays plain Java. It only runs under KAPT, which passes the `kapt.kotlin.generated` output directory; under plain javac it skips with one compiler note.

## Considered Options

- **Naming rules in core**, keeping "backends depend only on core" absolute: rejected, because core would then know Hamcrest naming although it is meant to be library-free.
- **An option of the Hamcrest backend**: rejected, because it would pull Kotlin tooling onto every Hamcrest user's processor path and contradicts "one configuration, no backend-specific options".
- **Generator written in Kotlin, or string templates instead of KotlinPoet**: rejected, the former breaks "production code in plain Java", the latter re-implements keyword escaping, nullability and imports.

## Consequences

- Feasibility is proven on the unmerged branch `prototype/kotlin-dsl-kapt`: under Maven KAPT, a `.kt` file written to `kapt.kotlin.generated` is compiled in the same build and can call the Java matcher generated in the same run; `test-kapt` registers both generated directories itself. An explicit `<sourceDirs>` replaces the defaults and must list them.
- The generated Kotlin's emitted text is tested by golden-file ITs (compile-testing cannot compile Kotlin); compiling and behaviour are proven by the Kotlin end-to-end modules.
- The backend-to-backend dependency is recorded as technical debt, and the KAPT-only generation as a risk, in arc42 chapter 11 (`docs/src/docs/asciidoc/architecture/_chapters/11_technical_risks.adoc`).
