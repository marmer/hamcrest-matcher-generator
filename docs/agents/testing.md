# Testing approach

Annotation processor tests (file `MatcherGenerationProcessorIT.java` in `hamcrest-matcher-generator/src/test/java`, class `MatcherGenerationProcessorIT`: use the class name for `-Dit.test`) use **Google Compile Testing**: they compile in-memory Java source strings and assert on the generated output. Expected generated sources are **golden files** under `src/test/resources/io/github/marmer/matchergenerator/hamcrest/MatcherGenerationProcessorIT/<scenario>/`, compared character for character: `assertGeneratedSource` reads the golden file and `assertEquals`s it against the generated file's `getCharContent(true)`, so any whitespace difference fails. This runs as a Maven Failsafe integration test.

Core processor tests (`CoreProcessorIT` in `matcher-generator-core`, run it with `-pl matcher-generator-core -Dit.test=CoreProcessorIT`) use the same harness with a **recording test backend** (registered via `META-INF/services` in the core test resources) and assert on the recorded `TypeModel` instead of generated Hamcrest code.

The backend's golden ITs are `AssertionGenerationIT` (`-Dit.test=AssertionGenerationIT`, goldens under `assertj-assertion-generator/src/test/resources/.../AssertionGenerationIT/<scenario>/`); the end-to-end module is `hamcrest-matcher-generator-endtoend-assertj`.

Runtime tests (`BeanPropertyMatcherTest.java`) are plain JUnit unit tests. Hamcrest mismatch messages are tested against golden `.txt` files in `BeanPropertyMatcherMessageTest` (and `PropertyDescriptionTest` for value rendering).

`ItemsInAnyOrderFuzzTest` in `hamcrest-matcher-generator` is a Jazzer oracle test of `itemsInAnyOrder`: its seeds replay in the normal test phase, and `JAZZER_FUZZ=1 ./mvnw test -pl hamcrest-matcher-generator -Dtest=ItemsInAnyOrderFuzzTest#itemsInAnyOrderAgreesWithTheBruteForceOracle` fuzzes on demand (corpus `.cifuzz-corpus/` is git-ignored; details in CONTRIBUTING.md).

Regenerate golden files with `-Dgolden.update=true` and review `git diff --stat` and the diff of `src/test/resources` (it also rewrites unrelated goldens when other output changed); never edit them by script, and never revert them with `git checkout` (that drops hand edits too): regenerate. After a scripted change `git diff --numstat` shows 0 deleted lines when only additions were meant. Hamcrest goldens: reinstall core first (`-pl hamcrest-matcher-generator` reads core from `~/.m2`). JavaPoet quirks in the AssertJ goldens: `$W` leaves a trailing space after `=` (leave long field initializers unwrapped) and the output carries `import java.lang.X;` lines.

The plain-kotlin end-to-end module checks compile failures through maven-invoker consumer builds under `src/it` (`right-scope`, `wrong-scope`, `wrong-value-type`, BeanShell `postbuild.bsh` hooks); `-DskipTests` skips them, and they must pass on a fresh local repository (`scripts/agents/verify.sh --only fresh-repo`).

End-to-end modules are the final validation layer: they apply `@GenerateMatchers` to real POJOs and exercise the generated matchers in actual test assertions. The 5.x suite is the exception, see `docs/agents/5x-compatibility.md`.
