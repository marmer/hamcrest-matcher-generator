# AssertJ extension model for generated custom assertions

Research for issue #57, part of the 6.0.0 Wayfinder map (#52). Researched on 2026-10-22 against
AssertJ Core 3.27.7 (latest stable) and 4.0.0-M1, and against the assertj-generator `main` branch.

**Question.** How are custom AssertJ assertions conventionally built (base classes, self types,
entry points, soft assertions, BDD style, `InstanceOfAssertFactory`)? What did
assertj-assertions-generator produce, and is it still maintained? Which AssertJ versions run on
Java 11? What should generated assertions for a bean or record look like so they feel native to
AssertJ users? What must a generator avoid clashing with?

Sections 1-6 are **facts**, each with a source. Section 7 is a **recommendation** and includes a
sketch of a generated class. Section 8 lists facts that later tickets depend on.

---

## 1. Versions and Java baseline (facts)

| Artifact | Latest | Java | Source |
|---|---|---|---|
| `org.assertj:assertj-core` 3.x | **3.27.7** | "AssertJ Core requires Java 8 or higher" | [docs §2.4.1](https://assertj.github.io/doc/#assertj-core-java-versions), [Maven Central metadata](https://repo1.maven.org/maven2/org/assertj/assertj-core/maven-metadata.xml) |
| `org.assertj:assertj-core` 4.x | **4.0.0-M1** (milestone, published 2025-03-09) | "Raise required Java version to 17" (#3447); "binary incompatible with the previous major version" | [4.0.0-M1 release notes](https://github.com/assertj/assertj/releases/tag/assertj-build-4.0.0-M1), [Maven Central dir](https://repo1.maven.org/maven2/org/assertj/assertj-core/4.0.0-M1/) |

- **Java 11 is supported by the whole AssertJ 3.x line, and only by that line.** AssertJ 4 requires Java 17.
- AssertJ Core says it is "compatible with Kotlin 1.9 or higher"
  ([docs §Kotlin](https://assertj.github.io/doc/#assertj-core-kotlin)).
- Context from this repo: `docs/modernization-plan-6.0.0.md` already sets a **Java 17** baseline
  for 6.0.0. With that baseline, generated code can target either AssertJ 3.27.x or 4.x. The
  project only needs to avoid APIs that 4.x removes or deprecates. 4.0.0-M1 marks every
  deprecation with `forRemoval = true`
  ([release notes](https://github.com/assertj/assertj/releases/tag/assertj-build-4.0.0-M1)).

## 2. How a custom assertion class is structured (facts)

Source: [docs §2.6.2 "Custom Assertions"](https://assertj.github.io/doc/#assertj-core-custom-assertions).

- "create a class inheriting from `AbstractAssert` and add your custom assertions methods. Add a
  static method `assertThat` to provide a handy entry point." The first generic parameter is the
  class itself ("needed for assertion chaining"). The second is the type under test:
  `class TolkienCharacterAssert extends AbstractAssert<TolkienCharacterAssert, TolkienCharacter>`.
  The constructor calls `super(actual, TolkienCharacterAssert.class)`
  ([§creation](https://assertj.github.io/doc/#assertj-core-custom-assertions-creation)).
- The documented pattern for each assertion method is:
  1. call `isNotNull()`
  2. compare the value
  3. on mismatch, call `failWithMessage("Expected character's name to be <%s> but was <%s>", ...)`
  4. `return this`
- `AbstractAssert` exposes `protected final ACTUAL actual` and `protected final SELF myself`. Its
  failure helpers are `failWithMessage` / `failure(String, Object...)` (both String.format based),
  `failWithActualExpectedAndMessage` / `failureWithActualExpected(actual, expected, fmt, args...)`,
  and `throwAssertionError(ErrorMessageFactory)`
  ([AbstractAssert.java @3.27.7](https://github.com/assertj/assertj/blob/assertj-build-3.27.7/assertj-core/src/main/java/org/assertj/core/api/AbstractAssert.java),
  lines ~90, 144, 177, 207, 246, 270).
  - `failure(...)` honours `as(...)` descriptions and `overridingErrorMessage(...)`. It formats the
    arguments with plain `String.format`, so they are **not** rendered through the
    `Representation`.
  - `failureWithActualExpected(...)` builds the error through `assertionErrorCreator` with
    actual and expected values. With opentest4j present, that gives IDE "click to see difference"
    support. Its JavaDoc recommends `throw failure(...)` over `failWithMessage(...)` so that the
    compiler can tell the statement never returns.
  - AssertJ's own messages are `ErrorMessageFactory` subclasses of `BasicErrorMessageFactory`.
    These format their arguments through the active `Representation`
    ([BasicErrorMessageFactory.java](https://github.com/assertj/assertj/blob/assertj-build-3.27.7/assertj-core/src/main/java/org/assertj/core/error/BasicErrorMessageFactory.java)).
    Example: `"%nExpecting%n  %s%nto have a property or a field named %s with value%n  %s%nbut value was:%n  %s%n..."`
    ([ShouldHavePropertyOrFieldWithValue.java](https://github.com/assertj/assertj/blob/assertj-build-3.27.7/assertj-core/src/main/java/org/assertj/core/error/ShouldHavePropertyOrFieldWithValue.java)).
- `AbstractObjectAssert` (which the generator extends; see §5) adds object-level features on top
  of `AbstractAssert`:
  - `extracting(...)` (by name, or by `Function`, optionally with an `InstanceOfAssertFactory`)
  - `returns(expected, Function)` / `doesNotReturn`
  - `usingRecursiveComparison()`, `usingRecursiveAssertion()`
  - `hasFieldOrProperty`, `hasFieldOrPropertyWithValue`, `hasNoNullFieldsOrProperties`,
    `hasAllNullFieldsOrProperties(Except)`, `hasOnlyFields`, `usingComparatorForType/Fields`

  ([AbstractObjectAssert javadoc 3.27.7](https://javadoc.io/static/org.assertj/assertj-core/3.27.7/org/assertj/core/api/AbstractObjectAssert.html)).
- Recursive comparison starts with `usingRecursiveComparison()`. It "is meant to replace
  `isEqualToComparingFieldByFieldRecursively`"
  ([docs §2.5.13](https://assertj.github.io/doc/#assertj-core-recursive-comparison)). The
  `isEqualToComparing*` and `isEqualToIgnoring*` methods are the older API.
- A `Representation` controls how values are formatted. It can be set globally
  (`Assertions.useRepresentation`) or per assertion (`withRepresentation`)
  ([docs §2.5.7](https://assertj.github.io/doc/#assertj-core-representation)).

## 3. Entry points, BDD and soft assertions (facts)

**Aggregated entry point.** The docs suggest "create a CustomAssertions class providing static
`assertThat` methods for each of your assertions classes" (example name `MyProjectAssertions`),
used next to `import static org.assertj.core.api.Assertions.assertThat`. The entry class may
extend AssertJ's `Assertions` "if and only if you have one entry point class". If two entry
classes both inherit from `Assertions`, `assertThat(String)` becomes ambiguous
([§entry point](https://assertj.github.io/doc/#assertj-core-custom-assertions-entry-point)).

**BDD.** `org.assertj.core.api.BDDAssertions.then` is an alternative entry point that replaces
`assertThat` ([docs](https://assertj.github.io/doc/#alternative-entry-points)). A BDD soft variant
`BDDSoftAssertions` also exists
([docs](https://assertj.github.io/doc/#assertj-core-bdd-soft-assertions)).

**Soft assertions for custom types (since 3.16.0).**
([§combining](https://assertj.github.io/doc/#assertj-core-soft-assertions-combining))
1. Declare an interface `XSoftAssertionsProvider extends SoftAssertionsProvider` with
   `default XAssert assertThat(X actual) { return proxy(XAssert.class, X.class, actual); }`.
2. Write a concrete class that `extends SoftAssertions` (to keep standard soft assertions), or
   `AbstractSoftAssertions`, or `BDDSoftAssertions`, and `implements` any number of such provider
   interfaces. Java's single inheritance makes the interface approach the composable one.
3. JUnit 5 `SoftAssertionsExtension` can inject any `SoftAssertionsProvider` implementation that
   has a default constructor
   ([§JUnit 5](https://assertj.github.io/doc/#assertj-core-junit5-soft-assertions)).

**Constraint from the soft proxy.** `SoftProxies.createSoftAssertionProxy` subclasses the assert
class with ByteBuddy and then calls `proxyClass.getConstructor(actualClass).newInstance(actual)`
([SoftProxies.java](https://github.com/assertj/assertj/blob/assertj-build-3.27.7/assertj-core/src/main/java/org/assertj/core/api/SoftProxies.java),
lines 125-138). Therefore a soft-assertable custom assert class:
- must be **non-final**
- must have a **public constructor whose single parameter type is exactly the `actualClass`**
  passed to `proxy(...)`

The `proxy` signature is
`<SELF extends Assert<? extends SELF, ? extends ACTUAL>, ACTUAL> SELF proxy(Class<SELF>, Class<ACTUAL>, ACTUAL)`
([javadoc](https://javadoc.io/static/org.assertj/assertj-core/3.27.7/org/assertj/core/api/SoftAssertionsProvider.html)).

**`InstanceOfAssertFactory`** (since 3.13.0) is an `AssertFactory<Object, ASSERT>` decorator that
casts the value before delegating. Its constructors are:
- `(Class<T> type, AssertFactory<T, ASSERT> delegate)`
- `(Class<T> rawClass, Type[] typeArguments, AssertFactory<T, ASSERT> delegate)`, since 3.26.0,
  for generic types

([javadoc](https://javadoc.io/static/org.assertj/assertj-core/3.27.7/org/assertj/core/api/InstanceOfAssertFactory.html)).

It plugs into navigation: `first(as(STRING))`, `singleElement(as(...))`,
`extracting(fn, factory)` and `asInstanceOf(factory)`
([docs](https://assertj.github.io/doc/#assertj-core-group-navigation-fle)). AssertJ calls it "the
preferred way to create type-specific assertions in AssertJ API". The older
`assertThat(iterable, XAssert::new)` is `@Deprecated` in its favour
([Assertions.java](https://github.com/assertj/assertj/blob/assertj-build-3.27.7/assertj-core/src/main/java/org/assertj/core/api/Assertions.java),
lines ~680-705).

## 4. Overloads and name collisions (facts)

AssertJ's `Assertions` already has generic catch-all overloads (`Assertions.java` @3.27.7):
- `<T> ObjectAssert<T> assertThat(T)` (line 934)
- `<T extends Comparable<? super T>> AbstractComparableAssert<?, T> assertThat(T)` (line 3815)
- `<T extends Throwable> ... assertThat(T)` (line 1285)
- `assertThat(Iterable<? extends E>)` (line 3486)
- the escape hatches `assertThatObject(T)` and `assertThatComparable(...)`

What this means for generated code:
- **Overloads from different static imports combine rather than shadow.** JLS 17 §6.4.1 says a
  single-static-import shadows an on-demand static import only for a method "named n with
  signature s" ([JLS §6.4.1](https://docs.oracle.com/javase/specs/jls/se17/html/jls-6.html#jls-6.4.1)).
  So `import static org.assertj.core.api.Assertions.*` together with
  `import static my.PojoAssert.assertThat` works. Java's most-specific-method rule picks
  `assertThat(Pojo)` over the generic overloads, including when `Pojo` implements `Comparable`
  or `Iterable` (JLS §15.12.2.5).
- **Real ambiguity sources:**
  - Two generated entry classes that both extend `org.assertj.core.api.Assertions` (documented in
    §3).
  - An actual value whose static type is a subtype of two configured, unrelated types, for
    example a class implementing two configured interfaces `A` and `B`. Then
    `assertThat(A)` vs `assertThat(B)` is ambiguous.
  - `assertThat(null)`.
- **Simple-name clash.** The dormant generator named its entry classes `Assertions`,
  `SoftAssertions`, `BddAssertions` and so on (§5). Those names collide with
  `org.assertj.core.api.Assertions` / `SoftAssertions` in regular imports and IDE auto-import.
- **Method-name clashes inside the assert class.** Generated `has<Prop>` / `is<Prop>` names can
  hit inherited methods (lists from the javadoc pages linked in §2):
  - from `AbstractAssert`: `hasToString`, `hasSameClassAs`, `hasSameHashCodeAs`, `isNull`,
    `isNotNull`, `isIn`, `isNot`, `isEqualTo`, `isSameAs`, `isInstanceOf…`, `isOfAnyClassIn`,
    `matches`, `satisfies`, `has(Condition)`, `as`, `describedAs`, `extracting`
  - from `AbstractObjectAssert`: `hasFieldOrProperty`, `hasFieldOrPropertyWithValue`,
    `hasNoNullFieldsOrProperties(Except)`, `hasAllNullFieldsOrProperties(Except)`, `hasOnlyFields`,
    `returns`, `doesNotReturn`

  For example, a bean property `sameClassAs` of type `Object` would produce
  `hasSameClassAs(Object)` and silently override the inherited method. Property names that are
  Java keywords also need a safe parameter name (the generator uses `${property_safe}`).

## 5. What assertj-assertions-generator produced, and its status (facts)

**Status.**
- The repo now lives at `assertj/assertj-generator`.
  - Last **stable** release: 2.2.1 (tag dated 2021-02-01).
  - Since then only **milestones**: 3.0.0-M3 (2024-10), M4/M5 (2025-05) and M6 (2026-06-27),
    published as `org.assertj:assertj-generator`
    ([tags](https://github.com/assertj/assertj-generator/tags),
    [Maven Central assertj-generator](https://repo1.maven.org/maven2/org/assertj/assertj-generator/maven-metadata.xml),
    [assertj-assertions-generator](https://repo1.maven.org/maven2/org/assertj/assertj-assertions-generator/maven-metadata.xml)).
  - Recent `main` commits are almost all Dependabot bumps and release plumbing
    ([commit feed](https://github.com/assertj/assertj-generator/commits/main.atom)).
  - `main` compiles with `maven.compiler.release` 17 against assertj-core 3.27.7
    ([pom](https://github.com/assertj/assertj-generator/blob/main/assertj-generator/pom.xml)).
- The **Maven plugin** `assertj-assertions-generator-maven-plugin` is effectively abandoned.
  - Last release 2.2.0 on 2019-03-10; last commit 2020-06-22
    ([Maven Central](https://repo1.maven.org/maven2/org/assertj/assertj-assertions-generator-maven-plugin/maven-metadata.xml),
    [commit feed](https://github.com/assertj/assertj-assertions-generator-maven-plugin/commits/main.atom)).
  - It works on compiled classes and writes to `src/test/generated-assertions`
    ([docs §2.8](https://assertj.github.io/doc/#assertj-samples)).
  - It is not an annotation processor.
- The README still points to the old documentation at joel-costigliola.github.io
  ([README](https://github.com/assertj/assertj-generator/blob/main/README.md)).

**Output shape.** These come from the templates in
[`assertj-generator/src/main/resources/templates`](https://github.com/assertj/assertj-generator/tree/main/assertj-generator/src/main/resources/templates).

- **Flat class** (`custom_assertion_class_template.txt`):
  - `@javax.annotation.processing.Generated(value="assertj-assertions-generator") public class XAssert extends AbstractObjectAssert<XAssert, X>`
  - public constructor `XAssert(X actual)` calling `super(actual, XAssert.class)`
  - `@org.assertj.core.util.CheckReturnValue public static XAssert assertThat(X actual)`
- **Hierarchical mode** (`custom_abstract_assertion_class_template.txt` +
  `custom_hierarchical_assertion_class_template.txt`):
  - `public abstract class AbstractXAssert<S extends AbstractXAssert<S, A>, A extends X> extends ${super_assertion_class}<S, A>`
    with a protected `(A actual, Class<S> selfType)` constructor. The super class is the parent
    type's `Abstract…Assert`, so the assertion classes mirror the domain type hierarchy.
  - A concrete `XAssert extends AbstractXAssert<XAssert, X>` with the JavaDoc note "not final to
    allow Soft assertions proxy".
- **Per-property methods:**
  - `has<Prop>(T value)` (`has_assertion_template.txt`): `isNotNull()`, compare with
    `Objects.deepEquals`, then
    `failWithMessage("\nExpecting <prop> of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>", actual, expected, actualValue)`,
    then `return ${myself}`.
  - Primitive variants use `!=`.
  - Real-number variants exist (`has_assertion_template_for_real_number*.txt`).
  - Iterable and array properties get `has<Prop>(E...)`, `has<Prop>(Collection<? extends E>)`
    plus `hasOnly…`, `doesNotHave…`, `hasNo…`. These delegate to the **internal** API
    `org.assertj.core.internal.Iterables.instance().assertContains(info, …)`
    (`has_elements_assertion_template_for_iterable.txt`).
  - Boolean properties get `is<Prop>()` / `isNot<Prop>()` (`is_assertion_template.txt`).
- **Entry points:**
  - classes named `Assertions` (static `assertThat`), `BddAssertions` (`then`),
    `SoftAssertions extends org.assertj.core.api.SoftAssertions` with
    `proxy(XAssert.class, X.class, actual)`, plus JUnit 4 rule and AutoCloseable soft variants
    (`*_entry_point_class_template.txt`)
  - soft entry points use **inheritance**, not the 3.16 `SoftAssertionsProvider` interfaces

## 6. Native-feel idioms that already exist (facts)

- Property checks without generated code: `assertThat(p).returns("x", from(Pojo::getFoo))`
  ([Assertions.java](https://github.com/assertj/assertj/blob/assertj-build-3.27.7/assertj-core/src/main/java/org/assertj/core/api/Assertions.java),
  ~line 2734), and `hasFieldOrPropertyWithValue("foo", "x")`.
- Navigation: `extracting(Pojo::getFoo, as(STRING)).startsWith("x")`. `extracting` copies the
  assertion state (`withAssertionState(myself)`) and an auto description
  ([AbstractAssert.java](https://github.com/assertj/assertj/blob/assertj-build-3.27.7/assertj-core/src/main/java/org/assertj/core/api/AbstractAssert.java),
  ~lines 1260-1297).
- "Satisfying" naming precedent for nested consumer checks: `hasValueSatisfying`
  (Optional/AtomicReference), `isInstanceOfSatisfying`
  ([docs release notes](https://assertj.github.io/doc/#assertj-core-release-notes)).
- A Hamcrest bridge exists in core: `org.assertj.core.api.HamcrestCondition.matching(matcher)`
  ([docs](https://assertj.github.io/doc/), release notes "Improve HamcrestCondition generic type
  inference").

---

## 7. Recommendation: shape of generated AssertJ assertions

This section is opinion based on the facts above.

1. **Two classes per type, following the generator's hierarchical mode.**
   - `AbstractPojoAssert<SELF extends AbstractPojoAssert<SELF, ACTUAL>, ACTUAL extends Pojo> extends AbstractObjectAssert<SELF, ACTUAL>`
     holds all property methods and returns `myself`. Users can subclass it and add hand-written
     methods.
   - A concrete, **non-final** `PojoAssert extends AbstractPojoAssert<PojoAssert, Pojo>` with a
     **public `PojoAssert(Pojo actual)`** constructor. That constructor is required by the soft
     proxy.
   - Extend `AbstractObjectAssert`, not `AbstractAssert`, so that `extracting`, `returns`,
     `usingRecursiveComparison()` and the field/property checks are inherited for free. Recursive
     comparison then needs no generated code. It switches to AssertJ's `RecursiveComparisonAssert`,
     which is expected behaviour.
   - Mirroring the configured type hierarchy (abstract parent assert as super class) is optional.
     Defer it unless the Hamcrest side does the same.
2. **Per-property API:** the AssertJ analogue of `withFoo(value)` / `withFoo(matcher)`.
   - `hasFoo(T expected)`: equality via `java.util.Objects.deepEquals` for array-safe equality,
     which matches Hamcrest `equalTo` semantics and the generator. Primitives are boxed or compared
     directly.
   - `hasFooSatisfying(Consumer<? super T> requirements)`: the flexible overload, following the
     `…Satisfying` convention. Use a distinct name, not an overload of `hasFoo`, to avoid
     lambda/`null` ambiguity.
   - Optional, boolean properties only: `isFoo()` / `isNotFoo()`.
   - No `resetFoo` equivalent. AssertJ assertions are eager, so there is no accumulated state to
     reset.
   - Do not generate typed collection helpers that use `org.assertj.core.internal.*`. Users can
     navigate with `extracting(Pojo::getItems, as(LIST))`.
3. **Error messages.**
   - Use `throw failureWithActualExpected(actualValue, expected, "%nExpecting %s of:%n  %s%nto be:%n  %s%nbut was:%n  %s", …)`,
     which keeps IDE diffs.
   - Or use `throwAssertionError(new BasicErrorMessageFactory(...))` so that values go through the
     `Representation`. Choose one; both honour `as()` and `overridingErrorMessage()`.
   - Follow AssertJ's message style: `%n`, the "Expecting … to be … but was …" wording, and
     two-space indented values.
   - Always call `isNotNull()` first.
4. **Entry points** (all generated in a cleanly separated `assertj` backend).
   - `PojoAssert.assertThat(Pojo)` static. Optionally `then(Pojo)` on the same class for BDD
     users.
   - `public static final InstanceOfAssertFactory<Pojo, PojoAssert> POJO = new InstanceOfAssertFactory<>(Pojo.class, PojoAssert::new)`
     so that users can write `assertThat(order).extracting(Order::getCustomer, as(PojoAssert.POJO)).hasName("x")`
     and `assertThat(list).first(as(POJO))`. For generic types use the `Type[]` constructor
     (≥ 3.26.0) or skip.
   - Per configuration: an aggregated `<Prefix>Assertions` class with static `assertThat`
     overloads. Do **not** extend `org.assertj.core.api.Assertions` and do **not** name it
     `Assertions`.
   - A `<Prefix>SoftAssertionsProvider extends SoftAssertionsProvider` interface with `default`
     `proxy(...)` methods, so that users can mix it into their own `SoftAssertions` subclasses or
     inject it via `SoftAssertionsExtension`. Optionally add a ready-made
     `<Prefix>SoftAssertions extends SoftAssertions implements <Prefix>SoftAssertionsProvider`.
5. **Avoid unstable or internal API.**
   - Avoid `org.assertj.core.internal.*`, `org.assertj.core.util.CheckReturnValue` (a util-package
     annotation) and any `@Deprecated` method, because 4.x will remove deprecations.
   - Build against 3.27.x and verify against 4.0.0-Mx. Both run on the 6.0.0 Java 17 baseline.
6. **Collision guard** in the generator, in the spirit of the existing `isConflictingProperty`.
   - Skip, or rename with a warning, any generated method whose name and erasure match an
     inherited `AbstractObjectAssert`/`AbstractAssert` method.
   - Emit a `@Generated` annotation, which the worker already uses to skip generated types.
   - Keep the `Assert` suffix distinct from Hamcrest's `Matcher` suffix so that both backends can
     coexist in one package.

**Sketch** of the output for `record Pojo(String foo, int bar, boolean active)` or an equivalent
bean:

```java
@Generated("io.github.marmer.annotationprocessing...")
public abstract class AbstractPojoAssert<SELF extends AbstractPojoAssert<SELF, ACTUAL>, ACTUAL extends Pojo>
    extends AbstractObjectAssert<SELF, ACTUAL> {

  protected AbstractPojoAssert(ACTUAL actual, Class<?> selfType) { super(actual, selfType); }

  public SELF hasFoo(String expected) {
    isNotNull();
    String actualFoo = actual.foo();            // getFoo() for beans
    if (!java.util.Objects.deepEquals(actualFoo, expected)) {
      throw failureWithActualExpected(actualFoo, expected,
          "%nExpecting foo of:%n  %s%nto be:%n  %s%nbut was:%n  %s", actual, expected, actualFoo);
    }
    return myself;
  }

  public SELF hasFooSatisfying(Consumer<? super String> requirements) {
    isNotNull();
    requireNonNull(requirements, "requirements");
    requirements.accept(actual.foo());          // or: extracting(Pojo::foo, ...) to keep description
    return myself;
  }

  public SELF hasBar(int expected) { /* same pattern, primitive compare */ return myself; }
  public SELF isActive()    { isNotNull(); if (!actual.active()) throw failure("%nExpecting %s to be active but was not.", actual); return myself; }
  public SELF isNotActive() { /* inverse */ return myself; }
}

@Generated("...")
public class PojoAssert extends AbstractPojoAssert<PojoAssert, Pojo> {   // non-final: soft proxy subclasses it
  public static final InstanceOfAssertFactory<Pojo, PojoAssert> POJO =
      new InstanceOfAssertFactory<>(Pojo.class, PojoAssert::new);

  public PojoAssert(Pojo actual) { super(actual, PojoAssert.class); }  // exact (Pojo) ctor: required by SoftProxies

  public static PojoAssert assertThat(Pojo actual) { return new PojoAssert(actual); }
  public static PojoAssert then(Pojo actual)       { return new PojoAssert(actual); }
}

// per configuration (names are a proposal, not final)
public interface MyDomainSoftAssertionsProvider extends SoftAssertionsProvider {
  default PojoAssert assertThat(Pojo actual) { return proxy(PojoAssert.class, Pojo.class, actual); }
}
public final class MyDomainAssertions {
  private MyDomainAssertions() {}
  public static PojoAssert assertThat(Pojo actual) { return PojoAssert.assertThat(actual); }
}
```

Usage:
- `assertThat(pojo).hasFoo("x").hasBar(42).isActive()`
- `assertThat(pojo).hasFooSatisfying(f -> assertThat(f).startsWith("x"))`
- `assertThat(pojo).usingRecursiveComparison().ignoringFields("bar").isEqualTo(expected)`
  (inherited)
- `softly.assertThat(pojo).hasFoo("x")`

## 8. Facts that the architecture and naming tickets depend on

- AssertJ 3.x runs on Java 8+, so Java 11 users can use it. AssertJ 4.x (4.0.0-M1) needs Java 17
  and is binary incompatible with 3.x. Target 3.27.x and avoid deprecated and internal API so that
  4.x also works.
- Soft-assertion support requires a non-final assert class with a public constructor taking
  exactly the actual type (`getConstructor(actualClass)` in `SoftProxies`).
- Extending `AbstractObjectAssert<SELF, ACTUAL>` gives `extracting`, `returns` and recursive
  comparison for free. A self-typed `Abstract…Assert` keeps the classes extensible.
- Composable soft and BDD entry points use the `SoftAssertionsProvider` interface (3.16+), not
  inheritance from `SoftAssertions`.
- `InstanceOfAssertFactory` is AssertJ's preferred hook for type-specific navigation. Generic
  types need the `Type[]` constructor (≥ 3.26.0).
- Naming constraints:
  - never name an entry class `Assertions` or `SoftAssertions`
  - never extend `org.assertj.core.api.Assertions` (ambiguity when there is more than one entry
    class)
  - guard `has*`/`is*` names against inherited `AbstractAssert`/`AbstractObjectAssert` methods
  - static `assertThat(Pojo)` coexists with `Assertions.*` imports (JLS §6.4.1 shadows only
    identical signatures)
- The assertj generator offers nothing reusable for an APT backend. It works on compiled classes,
  only milestone releases have appeared since 2021, its Maven plugin is unmaintained since 2019,
  and its templates use internal API. It is useful only as a reference for naming (`XAssert`,
  `hasX`, `isX/isNotX`) and for the abstract/concrete class split.
