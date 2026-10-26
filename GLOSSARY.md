# Hamcrest Matcher Generator

An annotation processor that generates type-specific test assertions (Hamcrest matchers, AssertJ assertions) for Java beans and records.

## Language

**Property model**:
The library-agnostic description of a configured type and its properties (name, type, accessor, nullability), shared by all backends.
_Avoid_: bean model, metadata

**Property**:
A readable value of a bean (getter) or record (component accessor) that generated assertions can check.
_Avoid_: field, attribute

**Backend**:
A generator that turns the property model into assertion code for one assertion library (Hamcrest or AssertJ).
_Avoid_: plugin, flavour, target

**Configuration**:
The single annotation-based declaration of which types assertions are generated for; it is the same for every backend.
_Avoid_: backend settings

**Reference object**:
An instance of a configured type whose property values serve as the expected values for every property in one whole-object comparison.
_Avoid_: template, prototype object

**Property path**:
The dot-separated route from the asserted object to a checked value, with `[index]` for list items and `[key]` for map entries, used in mismatch messages (`customer.address.city`, `lines[1].quantity`, `bySku["B-2"].quantity`).
_Avoid_: property chain, field path

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
