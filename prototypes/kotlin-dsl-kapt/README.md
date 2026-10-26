# Prototype: Java annotation processor emitting Kotlin via KAPT (Maven)

Throwaway feasibility check, never merged. Question: can a **Java** annotation
processor, run by **KAPT under kotlin-maven-plugin** (Maven, not Gradle), emit a
`.kt` file that kotlinc compiles in the same build and that references a Java
class generated in the same KAPT run? And does it still run under plain javac?

## Result (Kotlin 2.1.20, Maven 3.9.9, JDK 21)

| Question | Answer |
|---|---|
| (a) `.kt` written to `kapt.kotlin.generated` compiled by kotlinc in the same build | **Yes** |
| (b) that `.kt` references a Java class the same processor wrote via `Filer` in the same KAPT run | **Yes** |
| (c) under plain javac, `kapt.kotlin.generated` is absent (`getOptions().get(..) == null`) | **Yes**, so the processor can detect it and skip the Kotlin output |

- Default dir in Maven: `${project.build.directory}/generated-sources/kaptKotlin/test`
  for `test-kapt` (`.../kaptKotlin/main` for `kapt`). The processor gets it as an absolute path.
- Java via `Filer` lands in `target/generated-sources/kapt/test`.
- `test-kapt` registers **both** dirs as test source roots of the Maven project.
  kotlinc `test-compile` with default `sourceDirs` and javac `testCompile` both pick
  them up. No build-helper-maven-plugin and no antrun needed.

## Modules

- `processor`: `@Gen` + `GenProcessor` (plain Java, no KotlinPoet). For each `@Gen` type `Foo`, it
  writes `FooMatcher.java` via `Filer` and, only if `kapt.kotlin.generated` is set,
  `FooDsl.kt` (`fun foo(block: FooMatcher.() -> Unit): FooMatcher`) with `java.nio.file.Files`.
- `consumer-kotlin`: Kotlin test sources, `test-kapt`; the test calls the generated `foo { }` DSL.
- `consumer-java`: plain javac with `annotationProcessorPaths`; the test asserts the option was
  `null` and no `.kt` file exists anywhere under `target/`.

## Working consumer config (see `consumer-kotlin/pom.xml`)

```xml
<build>
  <testSourceDirectory>src/test/kotlin</testSourceDirectory>
  <plugins>
    <plugin>
      <groupId>org.jetbrains.kotlin</groupId><artifactId>kotlin-maven-plugin</artifactId>
      <executions>
        <execution><id>test-kapt</id><goals><goal>test-kapt</goal></goals>
          <configuration><annotationProcessorPaths><annotationProcessorPath>
            <groupId>…</groupId><artifactId>processor</artifactId><version>…</version>
          </annotationProcessorPath></annotationProcessorPaths></configuration>
        </execution>
        <execution><id>test-compile</id><goals><goal>test-compile</goal></goals></execution>
      </executions>
    </plugin>
    <!-- Kotlin-first ordering: disable default-(test)Compile, re-add javac as new executions -->
    <plugin><artifactId>maven-compiler-plugin</artifactId> <configuration><proc>none</proc></configuration> …</plugin>
  </plugins>
</build>
```

The alternative also works, as in the repo's endtoend-plain-kotlin module: explicit `<sourceDirs>` on `test-compile` listing
`src/test/kotlin`, `target/generated-sources/kapt/test` and `target/generated-sources/kaptKotlin/test`.

## Gotchas

1. **Explicit `<sourceDirs>` replaces the defaults.** With only `src/test/kotlin` listed,
   the KAPT-registered dirs disappear and kotlinc fails: `Unresolved reference 'FooMatcher'` / `'foo'`.
   Either list all three dirs or omit `<sourceDirs>` and set `<testSourceDirectory>`.
2. With no `<sourceDirs>` and no `<testSourceDirectory>`, kotlinc compiles only the generated
   dirs (`FooDslKt`) and silently skips `src/test/kotlin`. The build stays green with 0 tests.
3. Kotlin-first ordering matters as soon as the generated Java references Kotlin types
   (`FooMatcher.matches(demo.Foo)` here): javac must run after kotlinc. The repo's root pom already does this.
4. Declaring `kapt.kotlin.generated` in `getSupportedOptions()` is optional. `getOptions()` returns it either way, and
   removing it printed no warning in this setup. It's kept for clarity.
5. Write the `.kt` with `Files` into the option's dir, not through `Filer`. The `Filer` only knows the Java/class/resource outputs.
6. `<proc>none</proc>` is set on javac in the KAPT module as a precaution, because on JDK < 23 javac would otherwise discover
   the processor on the test classpath. Not tested without it in the Kotlin-first layout. In the first (javac-first) run,
   javac ran without it and produced no duplicate-file error, because the `@Gen` type is Kotlin, so javac never sees it.
7. Offline build quirks in this container (not KAPT-related): artifacts in `~/.m2` are tracked under repo id
   `mavenCentral`, so `-o` refuses them for `central`; run with
   `-Daether.enhancedLocalRepository.trackingFilename=_none`. `maven-compiler-plugin` 3.13.0 dependencies aren't cached; use 3.8.1.
   `kotlin-test-junit5` pulls JUnit 5.10.1, which isn't cached; import `junit-bom` 5.12.1.
8. KotlinPoet (`com.squareup:kotlinpoet`) is **not** in `~/.m2` (only `javapoet`), so this prototype uses string templates.

## Run

```bash
../../mvnw -o -Daether.enhancedLocalRepository.trackingFilename=_none clean install
```
(Run `install` rather than `verify`, so `annotationProcessorPaths` can resolve `processor` from `~/.m2`. Reactor resolution wasn't tested.)
