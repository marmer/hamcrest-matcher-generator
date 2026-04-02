# hamcrest-matcher-generator — Claude Context

## Project Overview
**Purpose**: Compile-time code generator that creates Hamcrest matchers for JavaBean-like classes.
No runtime overhead, no production code pollution.
**Version**: 5.1.0 | **Group ID**: `io.github.marmer.testutils`
**Languages**: Java + Kotlin (hybrid) | **Build**: Maven 3.x (`./mvnw`)
**Branch**: `development` (main branch for PRs)

## Module Structure (5 modules)
```
hamcrest-matcher-generator/
├── hamcrest-matcher-generator-dependencies/         # @MatcherConfiguration + BeanPropertyMatcher
├── hamcrest-matcher-generator-annotationprocessor/  # APT processor (core generation logic)
├── hamcrest-matcher-generator-endtoend-plain-java-minimal/
├── hamcrest-matcher-generator-endtoend-mixed-kotlin-java/
└── hamcrest-matcher-generator-endtoend-plain-kotlin/
```
Maven profiles: `dev` (default, all 5 modules) | `release` (adds GPG signing, Javadoc, Nexus staging)

## Build Commands
```bash
./mvnw clean install          # Full build (dev profile, all modules)
./mvnw clean verify           # Build + integration tests
./mvnw clean install -P release  # Release build with signing
```

## Critical File Paths

### Production Code
| File | Role |
|------|------|
| `hamcrest-matcher-generator-annotationprocessor/src/main/java/io/github/marmer/annotationprocessing/MatcherGenerationProcessor.java` | APT entry point (`@AutoService`, extends `AbstractProcessor`) |
| `hamcrest-matcher-generator-annotationprocessor/src/main/kotlin/io/github/marmer/annotationprocessing/MatcherGenerationProcessorWorker.kt` | Orchestrates processing rounds (~154 lines) |
| `hamcrest-matcher-generator-annotationprocessor/src/main/kotlin/io/github/marmer/annotationprocessing/MatcherGenerator.kt` | Generates matcher classes via JavaPoet (~394 lines) |
| `hamcrest-matcher-generator-dependencies/src/main/java/io/github/marmer/testutils/generators/beanmatcher/dependencies/BeanPropertyMatcher.java` | Runtime base class for generated matchers |
| `hamcrest-matcher-generator-dependencies/src/main/java/io/github/marmer/testutils/generators/beanmatcher/dependencies/MatcherConfiguration.java` | `@MatcherConfiguration` annotation (SOURCE retention) |

### Tests
| File | Role |
|------|------|
| `hamcrest-matcher-generator-annotationprocessor/src/test/kotlin/io/github/marmer/annotationprocessing/MatcherGenerationProcessorWorkerIT.kt` | APT integration tests (Google Compile Testing) |
| `hamcrest-matcher-generator-dependencies/src/test/java/io/github/marmer/testutils/generators/beanmatcher/processing/BeanPropertyMatcherTest.java` | BeanPropertyMatcher unit tests (40+ cases) |
| `hamcrest-matcher-generator-endtoend-plain-java-minimal/src/test/java/foo/bar/sample/model/SomePojoTest.java` | End-to-end usage examples |
| `hamcrest-matcher-generator-endtoend-plain-java-minimal/src/test/java/foo/bar/sample/configuration/PackageConfiguration.java` | Example `@MatcherConfiguration` usage |

## Architecture

### Generation Flow
```
@MatcherConfiguration on test class
         ↓
MatcherGenerationProcessor.process()      [Java, @AutoService(Processor.class)]
         ↓
MatcherGenerationProcessorWorker.process()  [Kotlin]
  - getAllTypeElementsFor(): resolves class names + packages recursively
  - isSelfGenerated(): skips already-generated matchers (checks @Generated)
         ↓
MatcherGenerator.generateMatcherFor(typeElement)  [Kotlin, JavaPoet]
  - Generates TypeSafeMatcher<T> subclass
  - Writes to Filer (target/generated-sources/ or target/generated-test-sources/)
```

### Generated Matcher API
```java
assertThat(somePojo, isSomePojo()
    .withPojoField("value")              // equality shorthand
    .withPojoField(is(equalTo("value"))) // Hamcrest matcher variant
    .resetPojoField()                    // remove property check (v5.1.0+)
);
```

### Generated Matcher Structure
```java
@Generated("io.github.marmer.annotationprocessing.MatcherGenerationProcessor")
public class SomePojoMatcher extends TypeSafeMatcher<SomePojo> {
    private final BeanPropertyMatcher<SomePojo> beanPropertyMatcher;

    public static SomePojoMatcher isSomePojo() { ... }           // static factory
    public SomePojoMatcher withPojoField(Matcher<? super String> matcher) { ... }
    public SomePojoMatcher withPojoField(String value) { ... }   // wraps in equalTo()
    public SomePojoMatcher resetPojoField() { ... }              // clears property check

    // Delegates describeTo/matchesSafely/describeMismatchSafely to BeanPropertyMatcher
}
```

### Property Detection (`MatcherGenerator.kt` ~lines 363-374)
A method qualifies as a property if: public, non-static, no parameters, returns a value,
and name starts with `get` (any return type) or `is` (boolean return).

### BeanPropertyMatcher Runtime Logic
- `Map<String, List<Matcher<?>>>` stores property matchers
- `matchesSafely()`: calls getters via reflection, combines checks with `Matchers.allOf()`
- `with(name, matcher)` / `reset(name)` provide fluent builder API

## Common Tasks
- **Add support for new type pattern**: Modify `MatcherGenerationProcessorWorker.kt`
- **Change generated code structure**: Modify `MatcherGenerator.kt`
- **Change runtime matching logic**: Modify `BeanPropertyMatcher.java`
- **Add end-to-end test scenario**: Add to one of the `endtoend-*` modules
- **Test annotation processor**: Use Google Compile Testing in `MatcherGenerationProcessorWorkerIT.kt`

## Key Design Decisions
1. **Compile-time only**: No runtime overhead; generated code is statically typed
2. **Split modules**: `-dependencies` (small, runtime) vs `-annotationprocessor` (compile-only)
3. **SOURCE retention** for `@MatcherConfiguration`: Not compiled into production bytecode
4. **`@Generated` annotation**: Marks generated code, prevents reprocessing loop
5. **JavaPoet** over string templates: Type-safe code generation, no escaping issues
6. **Kotlin for processor logic**: More concise, extension functions, better null safety
7. **Java for annotation + runtime**: Better compatibility with Java consumers

## Key Dependencies
- **JavaPoet 1.13.0**: AST-based Java source generation
- **ClassGraph 4.8.108**: Classpath scanning
- **Hamcrest 2.2**: Matcher framework
- **Kotlin 1.5.10**: Core processor implementation
- **Lombok 1.18.20**: Optional, provided scope
- **JUnit 5.7.2** + **Mockito 3.11.1**: Testing

## CI/CD
- GitHub Actions: `.github/workflows/ci-build.yml`
- Runs on every push (except master): JDK 11, `mvn clean install verify`, JaCoCo + SonarCloud
