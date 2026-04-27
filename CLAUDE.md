# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Full build (dev profile, all 5 modules)
./mvnw clean install

# Build + run integration tests
./mvnw clean verify

# Windows with jabba-managed JDK
JAVA_HOME="/c/Users/MarianoMertinat/.jabba/jdk/corretto@17" PATH="$JAVA_HOME/bin:$PATH" ./mvnw clean install

# Single unit test class
./mvnw test -pl hamcrest-matcher-generator-annotationprocessor -Dtest=ClassName

# Single integration test class
./mvnw failsafe:integration-test -pl hamcrest-matcher-generator-annotationprocessor -Dit.test=ClassName

# Release build (adds GPG signing, Javadoc, Nexus staging)
./mvnw clean install -P release
```

Maven profiles: `dev` (default, all 5 modules), `release`.

## Module Structure

```
hamcrest-matcher-generator/
├── hamcrest-matcher-generator-dependencies/         # @MatcherConfiguration annotation + BeanPropertyMatcher runtime base
├── hamcrest-matcher-generator-annotationprocessor/  # APT processor — core generation logic
├── hamcrest-matcher-generator-endtoend-plain-java-minimal/
├── hamcrest-matcher-generator-endtoend-mixed-kotlin-java/
└── hamcrest-matcher-generator-endtoend-plain-kotlin/
```

## Architecture: Generation Flow

1. Compiler detects `@MatcherConfiguration` on a type (SOURCE retention, in `dependencies` module)
2. `MatcherGenerationProcessor.java` — APT entry point, delegates immediately to the Kotlin worker
3. `MatcherGenerationProcessorWorker.kt` — resolves configured class names or package strings to `TypeElement`s, skips already-`@Generated` types, passes each to `MatcherGenerator`
4. `MatcherGenerator.kt` — builds a `<TypeName>Matcher` class via JavaPoet; the generated class extends `TypeSafeMatcher<T>` and wraps an internal `BeanPropertyMatcher<T>` instance

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

Annotation processor tests (`MatcherGenerationProcessorWorkerIT.kt`) use **Google Compile Testing**: they compile in-memory Java source strings and assert on the generated output. This runs as a Maven Failsafe integration test.

Runtime tests (`BeanPropertyMatcherTest.java`) are plain JUnit 5 unit tests.

End-to-end modules are the final validation layer: they apply `@MatcherConfiguration` to real POJOs and exercise the generated matchers in actual test assertions.

## Known Pitfalls

- **compile-testing on Java 17** requires `--add-opens=jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED` in Failsafe `<argLine>` — already configured in the annotationprocessor pom.
- **JUnit Platform version mismatch**: Surefire 3.5.2 needs `junit-platform-launcher:1.12.1` explicitly on the classpath — already in root pom.
- **KAPT generated sources not found**: use `build-helper-maven-plugin:add-test-source` to register `target/generated-sources/kapt/test` *before* the compile phase; do not use antrun to move files.
- **JDK 25 implicit annotation processing disabled**: `@AutoService` was removed from `MatcherGenerationProcessor.java`; a manual `META-INF/services/javax.annotation.processing.Processor` resource is used instead. The `endtoend-plain-java-minimal` module uses `<annotationProcessorPaths>` to declare the AP explicitly.
- **Lombok on JDK 25**: requires Lombok ≥ 1.18.44; version 1.18.36 does not support JDK 25.
