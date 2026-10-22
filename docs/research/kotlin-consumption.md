# Consuming a Java annotation processor from Kotlin (state: October 2026)

Research ticket: #55 (part of map #52). Context: 6.0.0 is plain Java with a Java 11 baseline, a KSP
rewrite is out of scope, and a future AssertJ backend also generates Java.

Each section separates **Facts** (with a source) from **Recommendations** (my judgement for this
repo). Statements marked *(unverified)* are reasoned from how the tools work but were not
reproduced here. A local experiment build was not possible because Maven Central rate-limited the
sandbox (HTTP 429).

## Short answer

- KAPT is the only supported way to run a `javax.annotation.processing` processor over Kotlin
  sources. Since Kotlin 2.1.20 the K2 implementation of KAPT is the default in all projects,
  Maven and Gradle alike. It works, it is not deprecated, and it gets no new features.
- KSP cannot run javax processors, and KSP has no official Maven integration. It is not an option
  without a rewrite.
- There is a KAPT-free path. If the model classes are already compiled (for example Kotlin `main`
  classes), a small **Java** `@MatcherConfiguration` class compiled by plain `javac` with the
  processor on the processor path can generate matchers from the class files. The processor
  resolves configured names through `Elements`, so the types do not need to be in the same
  compilation. The generated matchers must then reach Kotlin through a separate module or a Gradle
  `testFixtures` source set.
- JSpecify on generated Java code changes Kotlin call sites from platform types (`T!`) to real
  Kotlin types. Since Kotlin 2.1.0, mismatches are **errors** by default. That is a benefit, but it
  is also a source-compatibility risk: every annotation the generator emits becomes part of the
  Kotlin-visible contract.
- Gradle incremental KAPT needs every processor in the module to declare itself incremental. This
  processor cannot qualify today. `@MatcherConfiguration` has `SOURCE` retention, and Gradle
  requires `CLASS` or `RUNTIME` retention for incremental processors. The processor also discovers
  types by package string, which rules out "isolating". Maven KAPT has no incremental mode at all.
- The biggest Kotlin usability gap is in the processor, not in KAPT. Its property detection drops
  common Kotlin `Boolean` properties (details below).

## 1. KAPT status with K2

### Facts

- KAPT "generates stub files from Kotlin source code and then runs the Java annotation processors
  on those stubs" and "works with both Maven and Gradle".
  <https://kotlinlang.org/docs/kapt.html>
- "Starting with Kotlin 2.1.20, the K2 implementation of the kapt compiler plugin is enabled by
  default for all the projects." The temporary opt-out is `kapt.use.k2=false` in
  `gradle.properties`. Feedback is collected in KT-71439.
  <https://kotlinlang.org/docs/whatsnew2120.html>,
  <https://youtrack.jetbrains.com/issue/KT-71439/K2-kapt-feedback>
- K2 KAPT was first shipped as a preview in Kotlin 1.9.20.
  <https://kotlinlang.org/docs/whatsnew2120.html>,
  <https://kotlinlang.org/docs/whatsnew20.html>
- Maintenance mode: for years the KAPT docs carried the note "kapt is in maintenance mode. We are
  keeping it up-to-date with recent Kotlin and Java releases but have no plans to implement new
  features. Please use the Kotlin Symbol Processing API (KSP) for annotation processing." Google's
  Android guide still says "Kapt is now in maintenance mode".
  <https://book.kotlincn.net/text/kapt.html> (mirror of the kotlinlang page),
  <https://developer.android.com/build/migrate-to-ksp>.
  The current kotlinlang page (dated 07 August 2026) did not show that callout when fetched. It
  still links to "Migrate from kapt to KSP" under "What's next". No removal date has been
  announced.
- Current Kotlin release in the official docs is **2.4.21**. This repo pins `kotlin.version`
  **2.1.20** in the root `pom.xml`, so K2 KAPT is already what the e2e modules exercise.
  <https://kotlinlang.org/docs/maven-configure-project.html>
- Maven: KAPT can be configured explicitly with `kapt` / `test-kapt` executions, as this repo does.
  It can also be configured automatically with `<extensions>true</extensions>` on
  `kotlin-maven-plugin`. The automatic setup "adds `compile`, `test-compile`, `kapt`, and
  `test-kapt` executions" and ensures "Kotlin code is compiled first" and "Java code is compiled
  after Kotlin". It replaces the whole `<executions>` section.
  <https://kotlinlang.org/docs/maven-configure-project.html>,
  <https://kotlinlang.org/docs/kapt.html>
- Maven KAPT writes generated sources to `target/generated-sources/kapt/`.
  <https://kotlinlang.org/docs/kapt.html>
- Discovering processors from the compile classpath is deprecated in Maven KAPT. Users get the
  warning "Annotation processors discovery from compile classpath is deprecated" unless
  `kapt.include.compile.classpath=false` is set. Declaring the processor in
  `<annotationProcessorPaths>` is the supported way.
  <https://kotlinlang.org/docs/kapt.html>
- KAPT disables javac annotation processing by default. `keepJavacAnnotationProcessors` (Gradle)
  re-enables it, for example for Lombok.
  <https://kotlinlang.org/docs/kapt.html>
- `correctErrorTypes` (default `false`) controls how unknown types are handled. By default they
  are replaced with `NonExistentClass`. This matters when Kotlin sources reference generated
  matchers in the same compilation.
  <https://kotlinlang.org/docs/kapt.html>
- "kapt is not supported in the IntelliJ build system". IDE users must build through Maven or
  Gradle.
  <https://kotlinlang.org/docs/kapt.html>

### Recommendations

- Keep KAPT as the documented Kotlin path for 6.0.0, as already decided in
  `docs/modernization-plan-6.0.0.md`. Bump `kotlin.version` to a current 2.x release so the e2e
  modules test what users run.
- Document a minimal Maven setup in two variants. The preferred one is `<extensions>true</extensions>`
  plus `<annotationProcessorPaths>`. The explicit one is the `test-kapt` execution this repo uses.
  Also document a Gradle `kaptTest("io.github.marmer.testutils:hamcrest-matcher-generator-annotationprocessor:<v>")`
  snippet. Always show the processor on the processor path, never on the compile classpath.
- Consider switching one Kotlin e2e module to `<extensions>true</extensions>`. That keeps the
  documented "simple" path under test, while the other module keeps the explicit
  `build-helper-maven-plugin` wiring described in `CLAUDE.md`.

## 2. Alternatives to KAPT

### Facts

- KSP processors implement KSP's own `SymbolProcessorProvider` / `SymbolProcessor` API. KSP offers
  a "Reference for Java annotation processor authors" as a porting guide, not a way to run javax
  processors. Running this processor through KSP therefore means rewriting it.
  <https://kotlinlang.org/docs/ksp-overview.html>,
  <https://kotlinlang.org/docs/ksp-reference.html>
- KSP's requirements table lists only the Kotlin Gradle plugin, the Android Gradle Plugin, Gradle
  and the JDK. There is no official Maven plugin.
  <https://kotlinlang.org/docs/ksp-overview.html>
- This processor finds its targets with `Elements.getAllPackageElements(name)` and
  `Elements.getTypeElement(name)`, starting from the strings in `@MatcherConfiguration`. It does not
  rely on the targets being annotated or being part of the current compilation unit
  (`MatcherGenerationProcessorWorker.kt`, `getAllTypeElementsFor`). javac completes such elements
  from class files on the classpath.

### Recommendations

- **Option A (default): KAPT.** Use it when matchers are generated in the same Kotlin module that
  uses them.
- **Option B: javac on compiled Kotlin classes, no KAPT.** Use it when the build allows a separate
  compilation step. Put a Java `@MatcherConfiguration` class in a Java-only module, or in Gradle's
  `java-test-fixtures` source set with `testFixturesAnnotationProcessor(...)`. That source set
  depends on the compiled Kotlin model classes, and the Kotlin tests depend on it. This avoids stub
  generation and its maintenance-mode risk. *(Unverified: needs an e2e check, especially for the
  bytecode edge cases in section 4. In this mode the processor reads Kotlin bytecode, not KAPT
  stubs.)* A plain mixed Maven module does **not** work for option B when Kotlin tests use the
  matchers. Kotlin compiles before javac, so the matchers would not exist yet.
- Do not pursue KSP for 6.x, as already decided.

## 3. JSpecify on generated Java code and Kotlin call sites

### Facts

- Kotlin recognises `org.jspecify.annotations` (`@Nullable`, `@NonNull`, `@NullMarked`,
  `@NullUnmarked`). Annotated Java types are "represented not as platform types, but as actual
  nullable or non-nullable Kotlin types". In a `@NullMarked` class, a `@Nullable` return
  dereferenced without `?.` or `!!` is an error.
  <https://kotlinlang.org/docs/java-interop.html>
- JSpecify is "the only supported flavor that uses `strict` report level by default". The level can
  be lowered with `-Xjspecify-annotations=warn|ignore` or
  `-Xnullability-annotations=@org.jspecify.annotations:warn`.
  <https://kotlinlang.org/docs/java-interop.html>
- The default became `strict` (errors) in Kotlin 2.1.0 (KTLC-11). It had been a warning since
  1.6.0/1.8.20, and `@NonNull` support arrived in 2.0.0.
  <https://kotlinlang.org/docs/compatibility-guide-21.html>
- `@NullMarked` does not apply to type variables. They remain "null-agnostic" until a concrete
  type argument is provided.
  <https://kotlinlang.org/docs/java-interop.html>
- Without annotations, Java types are platform types (`T!`). Kotlin then relaxes null checks and
  reports no errors.
  <https://kotlinlang.org/docs/java-interop.html>
- JSpecify annotations are `TYPE_USE` with `RUNTIME` retention. Placement follows type-use rules,
  for example `Map.@Nullable Entry` and `String @Nullable []`. Generic parameters that may be
  nullable need a nullable bound, such as `<T extends @Nullable Object>`. The current release is
  1.0.0 (`org.jspecify:jspecify`).
  <https://jspecify.dev/docs/api/org/jspecify/annotations/Nullable.html>,
  <https://jspecify.dev/docs/user-guide/>,
  <https://jspecify.dev/docs/start-here/>
- Type-use nullability on type arguments requires Java 8+ targets, and `TYPE_USE` takes priority
  over other targets. This works with the Java 11 baseline.
  <https://kotlinlang.org/docs/java-interop.html>

### What this means for the generated API *(reasoned, unverified)*

| Generated member | Today (no annotations) | With `@NullMarked` + targeted `@Nullable` |
|---|---|---|
| `static FooMatcher isFoo()` | `FooMatcher!` | `FooMatcher`, non-null |
| `withBar(T value)`, equality shorthand | `T!`, `null` accepted | must be `@Nullable T` because "is null" is a legit check. Otherwise `withBar(null)` becomes a **compile error** in Kotlin |
| `withBar(Matcher<? super T> m)` | `Matcher<in T!>!` | `Matcher<in T>`, non-null |
| fluent return `this` | `FooMatcher!` | `FooMatcher` |

- Overload ambiguity: today `withBar(null)` is ambiguous between the value overload and the
  `Matcher` overload in Java, and probably in Kotlin too. With a `@Nullable` value parameter and a
  non-null `Matcher` parameter, Kotlin should resolve `withBar(null)` to the value overload. That
  is an ergonomic win *(unverified, add an e2e test)*.
- Hamcrest itself is not JSpecify-annotated, so `Matcher<…>` arguments created by
  `org.hamcrest.Matchers` stay platform types at Kotlin call sites. That is harmless.
- Because the default level is `strict`, adding or changing an annotation in a later release can
  break Kotlin builds. Users can only escape with a global compiler flag. Annotations must be
  decided once and covered by tests.
- The JSpecify jar must be on the Kotlin compile classpath wherever generated matchers are
  compiled or consumed, otherwise the annotations cannot be resolved. Making it a transitive
  `compile`-scope dependency of `hamcrest-matcher-generator-dependencies` handles that.

### Recommendations

- Adopt JSpecify in 6.0.0 if the feature-selection ticket accepts it. Emit `@NullMarked` on each
  generated matcher class and make the equality-shorthand parameter `@Nullable`. Everything else
  stays non-null. Apply the same to the runtime `BeanPropertyMatcher` public API and to the future
  AssertJ backend so both backends present one Kotlin contract.
- Add a Kotlin e2e test that compiles `withBar(null)`, `withBar(nullValue())` and
  `withBar("x")`. It should also assert that `isFoo()` is typed non-null, for example by assigning
  it to a non-null `val` with an explicit type.
- Mention `-Xjspecify-annotations=warn` in the docs as the escape hatch.

## 4. Kotlin data classes as the processor sees them

### Facts

- A Kotlin property compiles to a getter named by prepending `get`, a setter for `var`, and a
  private backing field. "If the name of the property starts with `is` … the name of the getter is
  the same as the property name … This rule applies for properties of any type, not just
  `Boolean`."
  <https://kotlinlang.org/docs/java-to-kotlin-interop.html>
- KAPT runs processors on Java stubs generated from Kotlin sources. Types that are only on the
  classpath, for example already compiled `main` classes, are read from bytecode.
  <https://kotlinlang.org/docs/kapt.html>
- Functions using inline value classes are mangled with a hash suffix (`compute-<hashcode>`).
  <https://kotlinlang.org/docs/inline-classes.html>
- `@JvmRecord` data classes produce real records with record-style accessors (`name()`), and they
  require JVM target 16 or higher.
  <https://kotlinlang.org/docs/jvm-records.html>
- The processor's getter rule in `MatcherGenerator.kt` (`hasReturnTypeWithMatchingPropertyPrefix`)
  accepts `get*` only when the return type is **not** primitive `boolean`, and `is*` only when it
  **is** primitive `boolean`.

### Consequences for Kotlin users (derived from the facts above)

| Kotlin declaration | JVM getter | Processor today |
|---|---|---|
| `val name: String` | `getName()` | `withName`, OK |
| `val nickname: String?` | `getNickname()` | `withNickname`, OK |
| `val enabled: Boolean` | `getEnabled(): boolean` | **skipped**: `get` + primitive boolean is rejected |
| `val isActive: Boolean` | `isActive(): boolean` | `withActive`. A Kotlin user may expect `withIsActive` |
| `val isOptional: Boolean?` | `isOptional(): Boolean` | **skipped**: `is` + boxed Boolean is rejected |
| `val isLabel: String` | `isLabel(): String` | **skipped** |
| `val id: SomeValueClass` | mangled getter *(unverified)* | at risk of an invalid generated name, or silently skipped if KAPT stubs omit it |
| `@JvmRecord data class` | `name()` | handled by the record path, as long as KAPT stubs or bytecode expose it as a record *(unverified for stubs)* |
| `componentN()`, `copy(...)` | not `get`/`is`, or has parameters | ignored, OK |

The current e2e modules only use `data class X(val someProp: String)`, so none of these cases are
covered.

### Recommendations

- Accept `get*` with any non-void return type, including `boolean`, and `is*` with
  `boolean`/`java.lang.Boolean`. This follows the JavaBeans rule plus the common boxed case. It
  helps Java users too.
- Decide explicitly how to name Kotlin `is*` properties. Keep `withActive` for JavaBeans
  consistency and document it, or detect Kotlin classes (`@kotlin.Metadata`) to name them
  `withIsActive`. Keeping it and documenting it is the cheaper choice.
- Skip methods whose names are not valid Java identifiers (`SourceVersion.isIdentifier`) with a
  note, so mangled value-class getters never produce uncompilable code.
- Add one probe data class with exactly the declarations in the table to both Kotlin e2e modules.

## 5. Gradle incremental annotation processing

### Facts

- "With Gradle, kapt supports incremental annotation processing by default". It only works if
  incremental compilation is enabled and "All annotation processors in the build are incremental".
  It can be turned off with `kapt.incremental.apt=false`. "Incremental annotation processing for
  kapt isn't supported in Maven or the CLI."
  <https://kotlinlang.org/docs/kapt.html>
- A processor opts in through `META-INF/gradle/incremental.annotation.processors` with the
  category `isolating`, `aggregating` or `dynamic`. Processors that do not opt in trigger full
  reprocessing. Both categories may only read `CLASS` or `RUNTIME` retention annotations, must
  write through `Filer`, and must not use compiler-specific APIs. An isolating processor must base
  each output only on its annotated type's AST, with exactly one originating element per file.
  <https://docs.gradle.org/current/userguide/java_plugin.html> (section "Incremental annotation
  processing")
- KAPT's incremental mode (experimental since 1.3.30) falls back to non-incremental processing for
  a module when any processor is non-incremental.
  <https://blog.jetbrains.com/kotlin/2019/04/kotlin-1-3-30-released/>
- This repo: `@MatcherConfiguration` and its nested annotations are `@Retention(SOURCE)`
  (`MatcherConfiguration.java`). The processor writes through `Filer` and already passes
  originating elements (`MatcherGenerator.kt`, `addOriginatingElement`). It has no
  `incremental.annotation.processors` file.

### Recommendations

- Do not declare the processor incremental in 6.0.0. Today it does not qualify (`SOURCE`
  retention), and it could only ever be **aggregating**, because outputs depend on package contents
  rather than on one annotated AST. Declaring it wrongly would cause silent stale matchers, which is
  worse than a full reprocess.
- Practical impact is small. Matchers are usually generated in the test source set
  (`kaptTest`/`test-kapt`), and changes in `main` are classpath ABI changes that trigger
  reprocessing anyway.
- If incremental support is wanted later, change the annotation retention to `CLASS`. That is
  binary-harmless and could be done in 6.0.0 to keep the door open. Then register `aggregating`
  and add a Gradle e2e that edits a model class and checks that the matcher is regenerated.

## 6. What the library must provide for a good Kotlin experience (no KSP)

1. **Correct property detection for Kotlin-shaped getters** (section 4). This is the main gap and
   the cheapest fix.
2. **Generated Java that is `@NullMarked`, with a `@Nullable` equality value**, plus the same on
   the runtime API, so Kotlin sees real types and `withX(null)` stays legal (section 3).
3. **Documented, copy-paste KAPT setups** for Maven (extensions and explicit) and Gradle
   (`kaptTest`), always using the processor path. Also document the no-KAPT option B for
   multi-module and `testFixtures` builds.
4. **e2e coverage** of the probe data class and the null-call cases in both Kotlin modules on a
   current Kotlin 2.x.
5. **Stable naming rules** (`is*` handling, skipped invalid identifiers) documented as part of the
   contract, because they are the Kotlin-visible API.
6. Optional, later (6.1.0 Kotlin DSL ticket): a thin Kotlin extension layer, for example
   `isFoo { name("x") }`. It would be written by hand against the Java API, not generated, and it
   relies on items 1 and 2 being in place.
