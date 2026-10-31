# Research: earning the Scorecard check `Fuzzing`

Research for ticket #282 (spec #104). No fuzzing code is part of this ticket. Observations are dated 2026-10-31.

- **Question**: can the project earn the OpenSSF Scorecard check `Fuzzing` (0/10 today) with a fuzz target that has real
  value, and is that worth doing?
- **Scorecard version looked at**: `v5.6.0`, commit `61e84743356275f6ffca23b1b2f2cb945e19eb96`. This is the version
  that produced the public result for this repository on 2026-10-31 (the `scorecard` field of
  <https://api.securityscorecards.dev/projects/github.com/marmer/hamcrest-matcher-generator>, `date`
  `2026-10-31T18:43:24Z`, total `7.7`, `Fuzzing` `0`, reason `project is not fuzzed`). All source links below are
  pinned to that commit. The project's own workflow uses `ossf/scorecard-action` v2.5.0
  ([scorecard.yml](../../.github/workflows/scorecard.yml)); the public API result is the one that counts for the badge.
- **Status of claims**: "Verified" = read in the primary source named. "Assumption" = not verified, with what would
  verify it.

## 1. What Scorecard counts as fuzzing

Sources (all at commit `61e8474`):

- Check documentation:
  [docs/checks.md, section Fuzzing](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/docs/checks.md#fuzzing)
- Detected tools list:
  [docs/checks/fuzzing/README.md](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/docs/checks/fuzzing/README.md)
- Detection code:
  [checks/raw/fuzzing.go](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/checks/raw/fuzzing.go)
- Probe:
  [probes/fuzzed/impl.go](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/probes/fuzzed/impl.go)
- Scoring:
  [checks/evaluation/fuzzing.go](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/checks/evaluation/fuzzing.go)

Verified:

1. **The score is binary.** The evaluation returns the maximum score (10, "project is fuzzed") when the probe finds at
   least one fuzzer, the minimum (0, "project is not fuzzed") otherwise. One detected tool is enough.
2. **Three detection routes** (check documentation): OSS-Fuzz (the repository has a `project.yaml` hit in the
   OSS-Fuzz project list), ClusterFuzzLite (a file `.clusterfuzzlite/Dockerfile` that holds a command), or a
   language-specific fuzz function in the repository's source.
3. **Java is detected, with exactly one tool: Jazzer.** `languageFuzzSpecs` has an entry for `clients.Java` with file
   pattern `*.java` and the line pattern `com.code_intelligence.jazzer.api.FuzzedDataProvider;`
   ([checks/raw/fuzzing.go](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/checks/raw/fuzzing.go),
   `languageFuzzSpecs`). The tool list names it: "Detection based on the import of
   `com.code_intelligence.jazzer.api.FuzzedDataProvider` in Java files"
   ([docs/checks/fuzzing/README.md](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/docs/checks/fuzzing/README.md)).
4. **jqwik (property-based testing for the JVM) is not detected.** The property-based entries exist only for Erlang,
   Haskell, Elixir, Gleam, JavaScript and TypeScript, C# and F#; none for Java or Kotlin. The word `jqwik` appears
   nowhere in the files above.
5. **Kotlin has no entry** either, so a Kotlin fuzz target would not count.
6. **The check documentation page is out of date for Java.** The Fuzzing section of `docs/checks.md` lists Go, Haskell,
   JavaScript/TypeScript, Erlang and C#/F# but not Jazzer; the code and `docs/checks/fuzzing/README.md` are
   authoritative (the issue text "whether it detects a Java tool is not verified" is answered by the code).
7. **How the line pattern behaves.** The match is a regular expression applied per line (`bytes.Split(content, "\n")`,
   `regexp.Find`), so the dots match any character and the trailing `;` is required: an `import
   com.code_intelligence.jazzer.api.FuzzedDataProvider;` line in any `*.java` file of the repository counts, test
   sources included. A wildcard import (`...api.*;`) or a fully qualified use without an import would not match.
8. **Only "prominent" languages are searched.** `getProminentLanguages` keeps languages whose line count is at least a
   quarter of the average per language. Java is the main language of this repository, so it qualifies (inferred from the
   code and the repository; the exact language statistics come from the hosting platform's API and were not fetched).
9. **Remediation text of the probe** points to the detected-tools list above; its `effort` is `High`
   ([probes/fuzzed/def.yml](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/probes/fuzzed/def.yml)).

Consequence for scoring (computed from the published check scores and the risk weights in `docs/checks.md`, Medium =
5, High = 7.5, Critical = 10, Low = 2.5; the computation reproduces the published `7.7`): the `Fuzzing` check weighs 5
of 97.5, so a 10 raises the total from about 7.7 to about 8.3. Assumption: the weights are those of the aggregation
code; only the reproduced total supports it.

Warning from Scorecard's own text: "A project that fulfills this criterion with other tools may still receive a low
score" ([docs/checks.md](https://github.com/ossf/scorecard/blob/61e84743356275f6ffca23b1b2f2cb945e19eb96/docs/checks.md#fuzzing)).
OSS-Fuzz and ClusterFuzzLite are not options here: OSS-Fuzz needs an accepted project (a review by Google, and the
project must be of significant use), ClusterFuzzLite needs a fuzz-target Docker build and a workflow; both are heavier
than a Jazzer test and neither adds value for a build-time annotation processor (judgement; the OSS-Fuzz acceptance
criteria were not read).

## 2. What Jazzer needs here

Primary sources: [Jazzer README](https://github.com/CodeIntelligenceTesting/jazzer/blob/main/README.md) (read on the
`main` branch on 2026-10-31), [`FuzzTest.java`](https://github.com/CodeIntelligenceTesting/jazzer/blob/main/src/main/java/com/code_intelligence/jazzer/junit/FuzzTest.java),
Maven Central metadata and POM of `com.code-intelligence:jazzer-junit` 0.30.0 (latest release; metadata
`lastUpdated` 2026-02-24).

Verified:

- Jazzer is a coverage-guided, in-process fuzzer for the JVM based on libFuzzer; license Apache-2.0
  (`LICENSE` of the repository).
- The JUnit integration needs JUnit 5.9.0 or newer; the POM of `jazzer-junit` 0.30.0 depends on `jazzer-api`,
  `jazzer`, `junit-jupiter-api`/`-params` 5.9.0 and `junit-platform-commons`/`-launcher` 1.9.0.
- `@FuzzTest` is a JUnit `@ParameterizedTest` with the tag `jazzer`. A fuzz-test method takes `byte[]`, a
  `FuzzedDataProvider`, or typed parameters. Taking a `FuzzedDataProvider` is the form that carries the import Scorecard
  looks for, and it is the natural form for building structured inputs (lists, object graphs).
- **Two modes.** Without `JAZZER_FUZZ` set, a fuzz test runs in *regression mode*: like an ordinary parameterized
  test it runs once per input file under `src/test/resources/<package>/<TestClass>Inputs/<method>` (crash reproducers)
  and per JUnit seed source. With `JAZZER_FUZZ=1` it runs in *fuzzing mode* (one fuzz test per run, mutates inputs,
  stores new coverage in `.cifuzz-corpus/` and crashes in the inputs directory).
- Class-file major version of `jazzer-api` 0.30.0 (`FuzzedDataProvider`) and `jazzer-junit` 0.30.0 (`FuzzTest`) is 52
  (Java 8), checked with `javap -v` on the jars from Maven Central, so they load on JDK 17.

Assumptions (verify in the implementation ticket, they decide whether the plan works):

- **A1**: regression mode on JDK 17 and JDK 21 needs no native code and no `-javaagent`, so a plain surefire run works
  unchanged. The README describes regression mode as behaving "like a traditional unit test"; whether the engine jar
  `jazzer` is loaded or initialised in that mode was not checked. Verify by running the new test with `./mvnw test`.
- **A2**: the `jazzer-junit` extensions work under surefire 3.5.2 with the project's `@{argLine}` JaCoCo agent. Verify
  by the same run.
- **A3**: `pitest-junit5-plugin` 1.2.3 runs `@FuzzTest` methods (a `@ParameterizedTest` meta-annotation) in the
  `mutation` profile. If it does not, exclude the fuzz test class from PIT by `excludedTestClasses`, or tag-filter
  `jazzer`. Verify with `./mvnw verify -Pmutation -pl hamcrest-matcher-generator` (about 10 minutes for all modules).
- **A4**: dependency-review, SCANOSS and Renovate accept the new test-scope dependency without configuration; license
  is Apache-2.0, like the project's.

## 3. Candidate targets

All three are in the Hamcrest backend or the core. Whatever is chosen, the fuzz test is a `@FuzzTest` that takes a
`FuzzedDataProvider`, lives in the module's `src/test/java` and runs in regression mode in the normal unit-test
phase. Fuzzing mode is run by hand (or optionally by a scheduled workflow, which this research does not recommend, see
section 4).

### Target A: unordered collection matching (`ItemsInAnyOrder`) against a brute-force oracle

- **Code**: `hamcrest-matcher-generator/.../hamcrest/ItemsInAnyOrder.java` (`assignMatchersToItems` and `claim`, a
  hand-written augmenting-path maximum bipartite matching, 159 lines), used by `CollectionMatchers` and the generated
  matchers.
- **Input fuzzed**: a list of items (small integers or small beans, from the data provider) and a list of item matchers
  (each `equalTo(k)`, `anything()`, a range, or a bean matcher with one property), both sized 0 to about 6 so that all
  permutations can be tried.
- **Invariant**: the matcher reports a match if and only if a one-to-one assignment of matchers to all items exists
  (checked by brute force over permutations); on a mismatch, `describeMismatch` does not throw and names exactly the
  unmatched items and matchers (counts agree with the oracle). Order of the input lists never changes the verdict.
- **Expected value**: low to moderate. The algorithm is the textbook augmenting path and its Javadoc says the result
  must not depend on claim order, so a regression test for it probably exists (not verified; read `CollectionMatchersTest`
  and `ItemsInAnyOrder` tests in the implementation ticket). A fuzz test could still find an error in the path where a
  matcher with the completeness check is considered (`matchesByValues`) or in the closest-candidate message code
  (`describeMissingAndTakeClosest`, which mutates `unused`). Honest estimate: most likely finds nothing; protects the
  algorithm against a future rewrite.
- **Build and CI cost on JDK 17**: lowest of the three. One test-scope dependency, one test class (about 100 lines),
  pure in-memory, regression mode adds milliseconds to the unit-test phase. No new workflow.
- **JaCoCo / PIT**: JaCoCo gates (`LINE` and `BRANCH` 0.99 on the merged data, root `pom.xml`, `jacoco-check`) can only
  rise, since the test runs only seeds in regression mode and exercises code that is already covered. PIT:
  `targetTests` is `io.github.marmer.*`, so the test joins the PIT run (assumption A3); extra tests cannot lower the
  `mutationThreshold` of 90 and may kill more mutants in `ItemsInAnyOrder`.

### Target B: reference matching and value rendering on random, possibly cyclic object graphs (`ReferenceMatching`, `PropertyDescription`, `PathWalk`)

- **Code**: `ReferenceMatching.matcherFor` (builds the matcher behind the generated `isXLike(reference)`, handles cyclic
  references through a thread-local map), `PropertyDescription` (renders values, `MAX_DEPTH` 5), `PathWalk` (the
  checking walk, counts a pair met again as equal).
- **Input fuzzed**: a graph of a few hand-written bean types (with collection, map, optional-null and back-reference
  properties), assembled from the data provider: node count, which property points to which node, including
  self-loops and cycles. Fixtures that have a generated matcher already exist for tests (`BeanFixtures`); the fuzz test
  needs bean types with matchers built by hand through the public `BeanPropertyMatcher` API, since the hamcrest module
  does not run its own processor on its test sources (assumption; check `BeanFixtures` and the module's compiler
  configuration).
- **Invariants**: (1) building the reference matcher for any graph terminates without exception or stack overflow;
  (2) reflexivity: the matcher built from a graph matches that same graph; (3) mutating one reachable property value
  makes the matcher mismatch, and `describeMismatch` and `describeTo` terminate without exception and (4) never print
  via `toString()` of a generated type (the rule `PropertyDescription` states).
- **Expected value**: the highest of the three. Cyclic graphs, shared references and the identity-keyed thread-local
  state are the kind of code where hand-written tests cover a few shapes and a generator finds the rest (stack
  overflow, a thread-local not cleaned up after an exception, depth cut-off interacting with cycles). The class
  Javadoc documents the cycle handling, so it was designed for; whether bugs remain is unknown.
- **Build and CI cost on JDK 17**: moderate. Same dependency as A, but the input builder and the oracle (a hand-written
  deep-equality walk with cycle detection) are the larger part: about 200 lines of test code that itself needs care, or
  the oracle degrades to the weaker invariants (1), (3) and (4) above. Regression mode stays in milliseconds.
- **JaCoCo / PIT**: same as A. Risk: the extra test may be slower in PIT, which re-runs covering tests per mutant;
  keep the number of seed inputs small.

### Target C: type discovery and generation in the processor (`TypeModelFactory`, `MatcherGenerationProcessorWorker`)

- **Code**: `matcher-generator-core` (`TypeModelFactory`, 353 lines; `MatcherGenerationProcessorWorker`, 707 lines).
- **Input fuzzed**: Java source text of a type, assembled from the data provider (property names, types including
  generics, arrays, nested types, nullability annotations, getters/setters/boolean `is`), compiled in memory with the
  existing Google Compile Testing harness with the processor.
- **Invariant**: the processor never throws and never reports an internal error; every generated source compiles; types
  the processor accepts yield a `TypeModel` whose properties match the declared getters.
- **Expected value**: potentially the highest for real bugs (javac's type model is the part of this project that has
  the most corner cases), and also the highest chance of false alarms from invalid generated inputs (the generator
  needs to emit only valid Java, otherwise every failure is noise).
- **Build and CI cost on JDK 17**: highest. One javac run per input (tens to hundreds of milliseconds), so fuzzing mode
  yields a few executions per second instead of thousands, which makes coverage-guided fuzzing weak. Regression mode is
  fine with a few seeds. The harness needs the `--add-opens=jdk.compiler/...` flags that failsafe sets in
  [pom.xml](../../pom.xml); the unit-test phase (surefire) does not set them, so the fuzz test would have to be a
  failsafe integration test or surefire would need the flags (assumption; compare `surefire` and `failsafe` argLine in
  the root pom). Coverage-guided instrumentation of javac itself would also be a cost in fuzzing mode.
- **JaCoCo / PIT**: it would run in the integration-test phase, so it is part of the merged JaCoCo data (can only
  help); PIT runs the integration tests too (`jvmArgs` repeat the add-opens flags), so a slow fuzz test adds
  directly to the roughly 10 minute mutation job.

## 4. Recommendation

**Implement, small: Target A first, as the carrier of the `Fuzzing` score; do not fuzz in CI.** Reasons:

- It is the cheapest way to earn the +0.5 (7.7 to about 8.3) on a binary check with a test that checks a real
  invariant (assignment is a maximum matching, equal to brute force) instead of an empty import. A and B share the
  same dependency and set-up, so B is a natural follow-up ticket if A shows that the set-up works and the owner wants
  more bug-finding value; C is not worth its cost now.
- The honest expectation is that A finds no bug. The value is a regression-mode test that protects the matching
  algorithm and a documented way to fuzz on demand. If the owner does not want a test-scope dependency on a
  third-party fuzzer for 0.5 score points, accepting the finding is just as defensible: the project's other
  verification layers (golden ITs, end-to-end modules, PIT at a 90 threshold, JaCoCo at 0.99) already exceed what a
  fuzzer would add for a build-time processor with no untrusted runtime input. In that case record it as an accepted
  finding ("the project is verified by property-style oracle tests and mutation testing; Scorecard detects only
  Jazzer for Java").
- Not recommended: a CI fuzzing workflow (ClusterFuzzLite or a scheduled `JAZZER_FUZZ=1` job). Scorecard counts the
  import only, a workflow adds pinned actions, permissions and maintenance, and the targets are not an attack surface
  (the library matches test objects; the processor runs at the user's build time on the user's own sources).

### Implementation ticket (text for the orchestrator to create)

Title: `test: fuzz test for unordered collection matching (Jazzer) to earn the Scorecard Fuzzing check`

Sub-issue of #104, blocking #147.

```markdown
## Parent

#104

**Implementer model:** sonnet

Reason: small, but the oracle test and the first Jazzer set-up (assumptions A1 to A3 of the research) need judgement.

## What to build

A Jazzer `@FuzzTest` for the unordered collection matcher (`ItemsInAnyOrder`) in the `hamcrest-matcher-generator`
module, so the Scorecard check `Fuzzing` (0/10 on 2026-10-31) detects Jazzer. Research with sources, the Scorecard
version looked at, the target and its assumptions: `docs/research/fuzzing.md` (read sections 1, 2 and the "Target A"
part of section 3 first). The test takes a `com.code_intelligence.jazzer.api.FuzzedDataProvider`, builds a list of
items and a list of item matchers (sizes 0 to 6, from the data provider) and checks: the matcher matches if and only if
a one-to-one assignment of matchers to all items exists (brute force over permutations); `describeMismatch` does not
throw and its counts of missing and unexpected items agree with the oracle; the verdict does not depend on the order
of the input lists. No production code changes unless the test finds a bug (then fix it test-first and say so).

## Acceptance criteria

- [ ] A test-scope dependency on `com.code-intelligence:jazzer-junit` (version in the root `dependencyManagement`, a
      property for Renovate like the other versions); no other module gets it.
- [ ] The fuzz test class has a line `import com.code_intelligence.jazzer.api.FuzzedDataProvider;` (exactly this
      import, no wildcard: Scorecard's regex `com.code_intelligence.jazzer.api.FuzzedDataProvider;` is applied per
      line to `*.java` files) and a `@FuzzTest` method that uses the provider.
- [ ] A seed set (JUnit `@MethodSource` seeds or files under `<TestClass>Inputs/`) covers: empty lists, equal sizes with
      one possible assignment, a case where greedy claiming fails and an augmenting path is needed, surplus items,
      surplus matchers.
- [ ] The test runs in regression mode in the normal unit-test phase on JDK 17 and JDK 21 with no extra JVM flags
      beyond the existing ones, and is seen red once by breaking `claim` (e.g. drop the recursive `claim(...)` call)
      with the failure line quoted in the report.
- [ ] Assumptions A1 to A4 of the research are checked and the results written into `docs/research/fuzzing.md`
      (section 2) in the same PR; if PIT cannot run `@FuzzTest`, the exclusion is configured and documented there.
- [ ] `./mvnw verify` on JDK 21 and 17 is green with the JaCoCo gate (0.99) unchanged; `./mvnw verify -Pmutation -pl
      hamcrest-matcher-generator` is green with the PIT threshold unchanged.
- [ ] `CONTRIBUTING.md` (or the testing section of the docs, wherever tests are described) gets two lines: how to run
      the fuzz test in fuzzing mode (`JAZZER_FUZZ=1`, which command, where the corpus goes) and that the corpus
      directory `.cifuzz-corpus/` is in `.gitignore`; crash reproducers found go to the `Inputs` directory with a test.
- [ ] After the change is on `main`, the next Scorecard run (weekly, `.github/workflows/scorecard.yml`) shows
      `Fuzzing` 10 on `https://api.securityscorecards.dev/projects/github.com/marmer/hamcrest-matcher-generator`
      (owner or orchestrator confirms; not a gate for the PR).
- [ ] No CI fuzzing workflow is added.

## Blocked by

None (can start immediately).
```

## 5. Open points

- Whether Scorecard's language statistics (hosting platform API) count Java as prominent for this repository was not
  fetched; Java is the main source language, so the risk is low. The first Scorecard run after the change settles it.
- Assumptions A1 to A4 above.
- The ticket acceptance criterion "the implementation ticket is created, a sub-issue of #104 and blocking #147" is the
  orchestrator's step; if the owner prefers to accept the finding, the text of section 4's second bullet is the
  record for the "accepted findings" ticket.
