# Hamcrest Matcher Generator

An annotation processor that generates type-specific test assertions (Hamcrest matchers, AssertJ assertions) for Java beans and records.

## Language

**Property model**:
The library-agnostic description of a configured type and its properties (name, type, accessor, nullability), shared by all backends. In code it is `TypeModel` (a whole type, with its nested types), `PropertyModel` (one property) and `TypeModel.Kind` (`POJO` or `RECORD`).
_Avoid_: bean model, metadata

**Nullability**:
Whether a property may be `null`, one of `NULLABLE`, `NON_NULL` and `UNSPECIFIED`. The core derives it from the accessor: primitives are non null, JSpecify `@Nullable`/`@NonNull` and the JetBrains `@Nullable`/`@NotNull` annotations that kotlinc emits decide explicitly, and otherwise the closest `@NullMarked`/`@NullUnmarked` scope does. A type variable stays `UNSPECIFIED`. Generated Hamcrest code is `@NullMarked` with `@Nullable` on equality values and on nullable property types, but only when JSpecify resolves at compile time. Kotlin reports a mismatch with those annotations as an error; `-Xjspecify-annotations=warn` lowers it to a warning.

**Property**:
A readable value of a bean (getter) or record (component accessor) that generated assertions can check. Every generated matcher also lists `class` (from `getClass()`) as a property, so `withClass(...)` and `ignoringClass()` are generated, as in 5.x, but the completeness check does not count `class`. Enums also get `declaringClass`; whether the completeness check should count it is still undecided.
_Avoid_: field, attribute

**Backend**:
A generator that turns the property model into assertion code for one assertion library (Hamcrest or AssertJ).
_Avoid_: plugin, flavour, target

**Assertion class**:
The pair of classes the AssertJ backend generates per configured type: `AbstractXAssert<SELF, ACTUAL>` on `AbstractObjectAssert`, which holds the generated assertions, and the concrete, non-final `XAssert` with a public `XAssert(X)` constructor and a static `assertThat(X)`. Neither is named `Assertions` or `SoftAssertions`, and none extends `org.assertj.core.api.Assertions`, so `assertThat(String)` stays unambiguous.
_Avoid_: assert class, assertion wrapper

**Bridge jar**:
The code-free jar that a 5.x artifact (`hamcrest-matcher-generator-dependencies`, `hamcrest-matcher-generator-annotationprocessor`) is published as in 6.0.0. It depends on `hamcrest-matcher-generator`, so old coordinates still resolve to a jar and the Hamcrest backend arrives transitively. A relocation POM is not used because older compiler plugins do not follow it in a processor path.
_Avoid_: relocation POM, shim

**Configuration**:
The single annotation-based declaration of which types assertions are generated for; it is the same for every backend.
_Avoid_: backend settings

**Reference object**:
An instance of a configured type whose property values serve as the expected values for every property in one whole-object comparison, `isXLike(reference)`. The comparison is deep for properties whose type has a generated matcher (also in lists, other collections, arrays and maps) and by `equals` for the rest, including enums and types without properties. It turns the completeness check on, `withFoo`, `resetFoo` and `ignoringFoo` override single properties, and a cyclic object graph ends because a pair met again counts as equal. Deep comparison applies only to types whose matcher is generated in the same processing round; a type from another round or compilation is compared by `equals`. A back-reference to the root reference object shares the root's matcher, so an override on the root applies there too.
_Avoid_: template, prototype object

**Property path**:
The dot-separated route from the asserted object to a checked value, with `[index]` for list items and `[key]` for map entries, used in Hamcrest mismatch messages (`customer.address.city`, `lines[1].quantity`, `bySku["B-2"].quantity`).
_Avoid_: property chain, field path

**Assertion path**:
The AssertJ counterpart of the **Property path**: the path of an AssertJ failure, rooted at the asserted type (`Order.customer.address.city`). An assertion created inside a `hasFooSatisfying` requirement continues the path of the enclosing property, carried by the runtime class `AssertionPath`. Inside an array, collection or map the path restarts at the element type (`OrderLine.sku`). A missing asserted object reads `Expecting Order.customer not to be null`.
_Avoid_: AssertJ path, nested path

**Generated bean matcher**:
A generated Hamcrest `<Type>Matcher`, identified at runtime by the interface `GeneratedBeanMatcher`. When one is used as the check of a property of another, the runtime flattens it into property paths instead of treating it like a foreign matcher.
_Avoid_: bean matcher, nested matcher

**Collection matcher**:
A runtime matcher for a collection or map property, created by `itemsInOrder(...)`, `itemsInAnyOrder(...)` or `entries(...)` in `CollectionMatchers`. Mismatches name the size, the path of every failing item or entry, and missing, closest and unexpected items or keys. Arrays count as lists. With the completeness check, `itemsInAnyOrder(...)` assigns items by their value checks and reports the properties neither checked nor ignored of an assigned item with its position, as in `contacts[1].phone`.
_Avoid_: collection assertion, container matcher

**One-line rendering**:
The form in which a generated matcher describes itself and its mismatch, `OrderLine {sku "X-9", quantity <3>}`, while it is held by one of Hamcrest's own matchers such as `contains(...)`. Outside such matchers it renders as a block with one failing check per line.
_Avoid_: compact mode, inline mode

**Completeness check**:
The check that every property of a type is either checked or consciously ignored, so a newly added property cannot slip through unasserted.
_Avoid_: strict mode, exhaustiveness

**DSL scope**:
The Kotlin receiver block for one configured type (`isOrder { … }`), in which only that type's property checks can be written; a property of another configured type opens a nested DSL scope (`customer { … }`).
_Avoid_: builder, spec block

**Generated code**:
The source files the processor writes into the user's project at compile time (matchers, assertions, Kotlin DSL files); they belong to the user and are not covered by the project license.
_Avoid_: generated library, output artifact

**Runtime library**:
The published artifact that generated code calls at test time (e.g. the Hamcrest `BeanPropertyMatcher`); like every other artifact of the project, it is under the project license.
_Avoid_: dependencies module, support code
