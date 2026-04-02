Guide me through implementing a new feature in the annotation processor using test-driven development (TDD).

## Step 1 — Understand the goal
Ask me: What should the annotation processor generate differently or additionally?
Examples: "generate a method X for type Y", "handle generic type Z", "support Lombok annotation W".

## Step 2 — RED: Write the failing APT integration test
In `hamcrest-matcher-generator-annotationprocessor/src/test/kotlin/io/github/marmer/annotationprocessing/MatcherGenerationProcessorWorkerIT.kt`, add a test that:
1. Compiles a minimal source class
2. Asserts the expected generated output (method exists, class structure, etc.)

```kotlin
@Test
fun `should generate X for Y`() {
    val sourceClass = JavaFileObjects.forSourceLines("pkg.SomeClass",
        "package pkg;",
        "public class SomeClass { /* minimal example */ }")

    val result = Compiler.javac()
        .withProcessors(MatcherGenerationProcessor())
        .compile(sourceClass)

    assertThat(result, compilesWithoutError())
    // assert generated source contains expected code
}
```

Run only the APT module tests:
```bash
./mvnw clean verify -pl hamcrest-matcher-generator-annotationprocessor
```
**The test must fail. Do not proceed until it does.**

## Step 3 — GREEN: Implement the minimal change
Modify `MatcherGenerator.kt` or `MatcherGenerationProcessorWorker.kt` to make the failing test pass.
Write the smallest possible change — no extras, no refactoring yet.

Run again:
```bash
./mvnw clean verify -pl hamcrest-matcher-generator-annotationprocessor
```
**The test must now pass.**

## Step 4 — Add an end-to-end scenario (optional but recommended)
Follow `/add-e2e-scenario` to add a concrete usage example for the new feature.

## Step 5 — REFACTOR
Clean up the implementation. No behavior changes allowed.

Run the full suite:
```bash
./mvnw clean verify
```
All modules must pass before the feature is complete.
