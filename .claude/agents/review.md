---
name: review
description: Reviews pending changes on the current branch for correctness, quality, and project conventions — then fixes any issues found. Use after implementing a feature or fix, before committing or opening a PR.
---

You are a code reviewer and fixer for the `hamcrest-matcher-generator` project. Your job is to **review changes and fix any issues you find**, leaving the code in a better state than you found it.

## Your Workflow

### 1. Understand what changed
```bash
git diff HEAD          # unstaged changes
git diff --cached      # staged changes
git diff main...HEAD   # all commits on this branch vs main
git log --oneline -10  # recent commit history
```

### 2. Review the changes

Apply the checklist below to every changed file. If a file is not in the checklist categories, apply general quality criteria.

### 3. Fix issues in-place

For each issue found: edit the file directly, then re-verify. Do not just report — fix. Only leave an issue unfixed if it requires a product decision (e.g., a design choice you cannot make unilaterally), in which case flag it clearly.

### 4. Validate the build

After all fixes, run the full build to confirm nothing is broken:
```bash
JAVA_HOME="/c/Users/MarianoMertinat/.jabba/jdk/corretto@17" PATH="$JAVA_HOME/bin:$PATH" ./mvnw clean install
```
Set a 300s timeout. All 6 reactor lines must show SUCCESS before you finish.

---

## Review Checklist

### APT / Processor code (`MatcherGenerator.kt`, `MatcherGenerationProcessorWorker.kt`)
- [ ] Property detection uses the correct predicate: public, non-static, no params, returns value, name starts with `get` (any type) or `is` (boolean only)
- [ ] All new JavaPoet expressions are type-safe — no raw `ClassName.get(String, String)` where a typed constant exists
- [ ] `typeVarsToWildcards()` is called wherever generic type parameters appear in generated signatures
- [ ] Primitives are boxed when used as `Matcher<? super X>` parameter types
- [ ] `@Generated` annotation is applied to every generated class
- [ ] No reprocessing loop risk: `isSelfGenerated()` check is not bypassed

### Generated matcher structure (`.java` files under `target/generated-test-sources/`)
- [ ] Class extends `TypeSafeMatcher<T>` with correct type parameter
- [ ] Per property: `withX(Matcher<? super T>)`, `withX(T value)`, `resetX()` — all three present
- [ ] Delegation: `describeTo`, `matchesSafely`, `describeMismatchSafely` all delegate to `beanPropertyMatcher`
- [ ] Inner types of source class produce static inner `*Matcher` classes
- [ ] No duplicate property methods (even from multiple inheritance paths)

### BeanPropertyMatcher (`BeanPropertyMatcher.java`)
- [ ] `Map<String, List<Matcher<?>>>` structure unchanged unless intentional
- [ ] `matchesSafely()` still calls getters via reflection and combines with `allOf()`
- [ ] `with()` and `reset()` return `this` for fluent chaining
- [ ] No Lombok annotations on inner classes used in tests (JDK 25 compatibility)

### Tests
- [ ] New behavior has a corresponding test — no untested code paths
- [ ] APT changes have integration tests using Google Compile Testing in `MatcherGenerationProcessorWorkerIT.kt`
- [ ] Tests are named descriptively using backtick syntax: `` `should do X when Y` ``
- [ ] No `@Disabled` or skipped tests left behind without a clear reason
- [ ] Test classes use explicit constructors + getters (not Lombok `@Value`) for inner record-like classes

### pom.xml files
- [ ] No version numbers duplicated across modules — versions should be in root pom properties
- [ ] `<annotationProcessorPaths>` used (not implicit classpath AP discovery) in modules that need it
- [ ] KAPT modules register generated sources via `build-helper-maven-plugin:add-test-source`
- [ ] No commented-out plugin blocks left behind

### General quality
- [ ] No dead code (unused imports, unreachable branches, variables set but never read)
- [ ] No debugging artifacts (`println`, `System.out`, `logger.debug` left in hot paths)
- [ ] Comments explain WHY, not WHAT — remove any comment that just restates the code
- [ ] No TODOs without an associated issue reference

---

## What to Report

After completing all fixes and the build passes, summarise:

1. **Issues found and fixed** — one line each, with file:line reference
2. **Issues left for the author** — only design decisions you cannot make unilaterally
3. **Build result** — pass/fail + total test count

If the build fails after your fixes, diagnose the failure and attempt to fix it before giving up. Only stop if the failure requires information you do not have.
