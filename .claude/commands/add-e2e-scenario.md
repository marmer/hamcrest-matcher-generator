Guide me through adding a new end-to-end test scenario using test-driven development (TDD).
The test is written first — it will fail to compile until the production code catches up.

Steps to follow:

1. **Ask me** what behavior or edge case the scenario should demonstrate (e.g., a specific type of generics, a Lombok annotation, an interface, an inner class pattern).

2. **Determine** which end-to-end module fits best:
   - `hamcrest-matcher-generator-endtoend-plain-java-minimal/` — plain Java POJOs
   - `hamcrest-matcher-generator-endtoend-mixed-kotlin-java/` — mixed Kotlin/Java
   - `hamcrest-matcher-generator-endtoend-plain-kotlin/` — pure Kotlin

3. **RED — Write the test first** in `src/test/java|kotlin/.../model/`. The test expresses the desired matcher API and will fail to compile because neither the model nor the generated matcher exist yet:
   ```java
   @Test
   void myNewScenario_shouldMatchAsExpected() {
       MyNewType instance = new MyNewType();
       instance.setMyProperty("value");

       assertThat(instance, isMyNewType()
           .withMyProperty("value")
       );
   }
   ```
   Run `./mvnw clean verify` — confirm the build fails with a compilation error (expected).

4. **Create** the model class (POJO/data class) in `src/test/java|kotlin/.../model/` with the getters referenced in the test.

5. **Register** the type in the `@MatcherConfiguration` class under `src/test/java|kotlin/.../configuration/`.

6. **GREEN — Run** `./mvnw clean verify` — the annotation processor now generates the matcher and the test should compile and pass.

7. **REFACTOR** if needed — clean up test or model without changing behavior, then verify again.

Key files to reference:
- Model example: `hamcrest-matcher-generator-endtoend-plain-java-minimal/src/test/java/foo/bar/sample/model/SomePojo.java`
- Config example: `hamcrest-matcher-generator-endtoend-plain-java-minimal/src/test/java/foo/bar/sample/configuration/PackageConfiguration.java`
- Test example: `hamcrest-matcher-generator-endtoend-plain-java-minimal/src/test/java/foo/bar/sample/model/SomePojoTest.java`
