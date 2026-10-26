# PIT mutation testing for a Java 11 annotation processor

Research done while grilling issue #62 ("Quality gates: what blocks a merge and what runs locally", part of map #52). Researched 2026-10-25 by a subagent against Maven Central, pitest.org, the hcoles/pitest source (HEAD f7fe5ad, 2026-10-14 = 1.30.1 tag), Arcmutate docs and SonarSource forum posts; backfilled from the subagent's report. PIT was **not** run against this project: section 7 is reasoning from the source, not measurement.

**Context:** Java 11 Maven multi-module library (annotation processor + small runtime), JUnit 5; the processor is tested mostly with Google compile-testing run by maven-failsafe as `*IT` classes; CI on GitHub-hosted runners, analysis tools on JDK 21+.

## Short answer

- **PIT 1.30.1** (pitest-maven, 2026-10-14) runs on JDK 11+ and is CI-tested on 11, 21 and 25; **pitest-junit5-plugin 1.2.3**.
- Thresholds (`mutationThreshold`, `coverageThreshold`, `testStrengthThreshold`, `maxSurviving`) **fail the Maven build**.
- PIT finds and runs tests itself: `*IT` classes in `target/test-classes` are candidates, but **failsafe config (e.g. `--add-opens` in `<argLine>`) is not read**; repeat it in PIT's `jvmArgs`/`argLine`.
- Speed levers: `threads`, history files (experimental, cacheable), narrow `targetTests`. Free changed-code-only analysis is gone (removed in 1.18.0); only commercial Arcmutate offers it.
- **SonarQube Cloud does not show PIT results.**

## 1. Versions and JDK support

- **pitest / pitest-maven 1.30.1** (2026-10-14, ASM 9.11). 1.30.0 followed 1.25.9; the changelog says a release was mislabelled 1.29.10. https://repo1.maven.org/maven2/org/pitest/pitest-maven/maven-metadata.xml, https://github.com/hcoles/pitest/releases
- **Minimum JDK 11** since 1.18.0 (#1380); the 1.30.1 jars are class-file major 55. The FAQ's "Java 8 or above" is outdated. https://github.com/hcoles/pitest/blob/master/README.md, https://pitest.org/faq/
- PIT's own CI matrix: JDK 11, 21, 25 (`.github/workflows/ci.yml`). Java 25 mutator fixes in 1.25.8 (#1485), ASM upgrades for 24 (#1355) and 22 (#1260). Running on JDK 21+ against `--release 11` bytecode: no specific statement, nothing suggests a problem.
- **pitest-junit5-plugin 1.2.3** (2025-05-20), needs PIT ≥ 1.19.4; since 1.2.2 `junit-platform-launcher` is provided and resolved by pitest-maven. Declare it as a dependency of the pitest-maven plugin. No JUnit 6 statement (irrelevant here). https://github.com/pitest/pitest-junit5-plugin

## 2. Thresholds

All checked in `PitMojo`; a miss throws `MojoFailureException`, so the build fails.
- `mutationThreshold`: killed / all mutants (uncovered mutants count against it).
- `coverageThreshold`: line coverage %.
- `testStrengthThreshold`: killed / (killed + survived), ignoring uncovered mutants.
- `maxSurviving`: fail if more than N survive; default -1 (off); only in the source, not on pitest.org.
- Integer thresholds by default (0 = off); `thresholdPrecision` (1.25.4+) allows decimals. Integer rounding can hide about 1 point of regression.

Sources: https://pitest.org/quickstart/maven/, https://github.com/hcoles/pitest/blob/master/pitest-maven/src/main/java/org/pitest/maven/PitMojo.java

## 3. Failsafe and `*IT` tests, cross-module tests, aggregation

- PIT discovers tests itself. Default `targetTests` = packages found in `target/test-classes`, so `*IT` classes there are candidates; restrict with `targetTests` / `excludedTestClasses`.
- It reads only **surefire** config (`excludes`, groups, `testFailureIgnore`, `argLine`, environment) via `SurefireConfigConverter`, **never failsafe's**. The `--add-opens` flags needed by compile-testing on JDK 17+ must be repeated in PIT's `jvmArgs`/`argLine` (from source, not tested).
- **Cross-module:** parameter `crossModule` ("limited support" since 1.17.1) mutates the module plus the reactor modules it depends on; a class may then appear in several reports, and aggregation is "undefined" unless each class is reported once. Third-party alternative: PitMP.
- **`report-aggregate`** (default phase prepare-package) from a separate module depending on the others; each module needs `exportLineCoverage=true` and XML output. Also `report-aggregate-module` (`aggregator=true`).

Sources: https://pitest.org/quickstart/maven/, `MojoToReportOptionsConverter.java`, `SurefireConfigConverter.java`, `PitAggregationMojo.java` in https://github.com/hcoles/pitest/tree/master/pitest-maven/src/main/java/org/pitest/maven

## 4. Speed options

- **Incremental analysis:** `historyInputFile` / `historyOutputFile` (may be the same file) or `withHistory` (uses `java.io.tmpdir`, overrides explicit paths). Docs call it "experimental"; some shortcuts can give wrong results because only superclass and outer-class changes are tracked. Nothing prevents caching the file between CI runs (e.g. `actions/cache` on an explicit path); history files were incompatible across PIT versions in the past, so **include the PIT version in the cache key**. Since 1.23.0 the default history analysis is a plugin (#1455). https://pitest.org/quickstart/incremental_analysis/
- `threads` (default 1); timeouts `timeoutFactor` 1.25 and `timeoutConstant` 4000 ms.
- **Mutator groups:** DEFAULTS = INVERT_NEGS, MATH, VOID_METHOD_CALLS, REMOVE_CONDITIONALS_ORDER_ELSE, REMOVE_CONDITIONALS_EQUAL_ELSE, CONDITIONALS_BOUNDARY, INCREMENTS, RETURNS. STRONGER = DEFAULTS + EXPERIMENTAL_SWITCH, REMOVE_CONDITIONALS_ORDER_IF, REMOVE_CONDITIONALS_EQUAL_IF. Also OLD_DEFAULTS, ALL. https://pitest.org/quickstart/mutators/
- `dryRun` (1.17.3+) for setting PIT up without running mutants; `mutationUnitSize`.
- **Descartes** (extreme mutation): eu.stamp-project:descartes 1.3.4 (2025-09-19); compatibility with PIT 1.30.x unconfirmed (README example shows pitest-maven 1.20.2 and a typo "1.3.34"). https://github.com/STAMP-project/pitest-descartes
- **Changed-code-only on PRs:** free `scmMutationCoverage` removed in 1.18.0 (#1379). Current route: Arcmutate `com.arcmutate:pitest-git-maven-plugin` 2.4.0 (`-DextraFeatures="+GIT(from[master])"`), needs `arcmutate-licence.txt`; "Free licenses for open-source projects", terms not found; fork-PR behaviour unconfirmed. Arcmutate also sells Base, Accelerator and a Kotlin plugin. https://docs.arcmutate.com/docs/git-integration.html, https://www.arcmutate.com/

## 5. Equivalent mutants and exclusions

- `excludedMethods` (name globs), `excludedClasses` (globs; an exact name does not match inner classes, add `*`), `excludedTestClasses`, `avoidCallsTo` (default covers logging; FLOGCALL feature).
- **Annotation filter `FANN`** (on by default) skips classes/methods annotated `Generated`, `DoNotMutate` or `CoverageIgnore` (simple name), **CLASS or RUNTIME retention only**; `javax.annotation.processing.Generated` is SOURCE-retention and invisible to PIT. Configurable via `+FANN(annotation[...])`. Source: `pitest-entry/.../intercept/annotations/ExcludedAnnotationInterceptorFactory.java`
- **Kotlin:** 1.25.0 added an "equivalent" mutant status (#1471); core has only the basic `FKOTLIN` junk filter (line 0, data-class code). Without the commercial Arcmutate Kotlin plugin PIT logs "Project uses kotlin, but the Arcmutate kotlin plugin is not present." (inferred to be a warning); expect junk mutants such as null-check intrinsics (inference). Source: `pitest-entry/.../verify/KotlinVerifierFactory.java`

## 6. Sonar

- SonarSource staff (2020): "no plans to support mutation coverage metrics in SonarCloud"; community plugins "can only be used with a SonarQube installation". https://community.sonarsource.com/t/mutation-testing-metrics/28507
- Later replies: SonarQube Cloud supports no additional plugins. https://community.sonarsource.com/t/does-sonarqube-cloud-support-third-party-plugins-like-the-mulesoft-sonarqube-plugin/138134
- sonar-pitest-plugin is discontinued; successor mutation-analysis-plugin is SonarQube Server only. https://community.sonarsource.com/t/new-release-mutation-analysis-1-4/7007

## 7. Annotation processors and compile-testing under PIT (inference)

No published runtime figures and no pitest issue about compile-testing or annotation processors were found.
1. **Run time:** PIT first runs all tests for coverage, then the covering tests per mutant in child JVMs. Every compile-testing test runs javac in memory and most processor mutants are covered by many of them, so expect long runs; levers are `threads`, history, narrow `targetTests`; raise `timeoutConstant` against timeouts on loaded runners (FAQ).
2. **Missing `--add-opens`:** see section 3.
3. **Mutant insertion:** PIT redefines loaded classes in the child JVM rather than using a new classloader (FAQ). Tests passing a processor instance (`withProcessors(new ...)`) use the mutated class; if javac loaded the processor via its own processor-path classloader from `META-INF/services`, mutants could be missed (not verified).
4. **Static initializers** are not re-run; `mutateStaticInitializers` defaults to false.

Source: https://pitest.org/faq/

## Not confirmed

- Arcmutate open-source licence terms and fork-PR behaviour.
- Descartes 1.3.4 with PIT 1.30.x.
- pitest-junit5-plugin with JUnit 6.
- An official SonarQube Cloud page on the plugin restriction (evidence is staff forum replies).
- All compile-testing runtime claims in section 7.
