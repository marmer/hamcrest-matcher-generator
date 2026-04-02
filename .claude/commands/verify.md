Run the full Maven build including integration tests:

```
./mvnw clean verify
```

This runs all 3 test layers:
1. Unit tests (`BeanPropertyMatcherTest.java`) in `-dependencies` module
2. Annotation processor integration tests (`MatcherGenerationProcessorWorkerIT.kt`) using Google Compile Testing
3. End-to-end tests in all 3 `endtoend-*` modules (plain-java-minimal, mixed-kotlin-java, plain-kotlin)

Report:
- Overall pass/fail status
- Which test layer failed (if any)
- Specific failing test names and error messages
- JaCoCo coverage summary if available

If tests fail, analyze the root cause. For APT integration test failures, check if the generated code matches expectations. For E2E failures, check if the generated matcher API changed.
