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

**Completeness check**:
The check that every property of a type is either checked or consciously ignored, so a newly added property cannot slip through unasserted.
_Avoid_: strict mode, exhaustiveness
