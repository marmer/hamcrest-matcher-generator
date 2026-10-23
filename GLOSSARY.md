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
