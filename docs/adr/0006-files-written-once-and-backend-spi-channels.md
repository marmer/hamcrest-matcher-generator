# Files written once per package, and the backend SPI channels that report it

Decided 2026-10-29 in ticket #189 (spec #104), implemented in PR #252.

A backend may write a file that exists once per target package: the AssertJ `SoftAssertionsProvider` mixin and `PropertyRepresentation`. Such a file only knows the types generated in the same processing round. A later round of the same compilation, or a second compilation of the package (main and test, KAPT), either asked the Filer for a source it had already written ("Attempt to recreate") or silently left types out. The decision: the file is written once per package, the first round decides its content, and every later round or compilation gets a diagnostic instead of a second file. Two configuration classes of one package in one round still produce one file with the types of both. The name stays `SoftAssertionsProvider`; it is not named per configuration.

To make the failure modes visible, the published backend SPI grew:

- `GenerationContext.reportVerboseNote(String)`: a compiler note that the core prints only with `-Amatchergenerator.verbose`. The Hamcrest backend uses it for one note per property that `isXLike(reference)` compares with `equals` because no matcher for its type is generated in the round.
- `GenerationContext.classExistsOutsideOwnOutput(String)`: whether a class is on the class path or source path without being in the output directory of the running compilation. The AssertJ backend uses it for the mixin only: it notes a `SoftAssertionsProvider` written by another compilation. `PropertyRepresentation` gets no class-path note.
- `GeneratedSource.qualifiedName()`.
- The 3-argument convenience constructor of `GenerationContext` drops verbose notes and answers `false` for `classExistsOutsideOwnOutput`.
- The rule "a source an earlier round wrote is not written again, the core warns at the annotation instead" is generic in the core worker and applies to every `GeneratedSource` of every backend, not only to the files written once per package.

## Considered Options

- **Name the mixin per configuration**: rejected. The name `SoftAssertionsProvider` depends only on the package, so a test class that implements it keeps compiling when a configuration or type is added; a per-configuration name would change with the configuration layout and break users on a refactoring. It also would not solve the real problem: two configurations in one package and one round already share one mixin, and a later round or compilation would still lack the earlier types. It would contradict what chapters 5, 6 and the glossary already described (one mixin per package, `PropertyRepresentation` written once per package as well).
- **Leave it silent** (the state before): rejected, because the Filer exception and the silently missing types leave no trace the user can follow.

## Consequences

- A later round that adds types to a package whose file an earlier round wrote gets one warning at the annotation, naming the file and the types it does not know. A build with `-Xlint:all -Werror` (or `-Werror` generally) fails on it; the user adds the types to the configuration of the first round or generates the configuration class before the matcher generator runs.
- The second-compilation case is a NOTE, not a warning, and it cannot tell every case apart: the processor sees only that a class of that name is on the class path outside its own output. A class left by a previous, non clean build of the same module is excluded by looking into the class output, but other legitimate constellations (for example a dependency that ships its own mixin on purpose) get the same note. The mixin of the second compilation does not contain the types of the first. The class-path NOTE covers the mixin only; the representation of a second compilation is written without a note, and it does not know the types of the first either.
- The "not written again" warning of the core worker covers every source of every backend, so a later round gets it for `PropertyRepresentation` as well as for the mixin.
- `GenerationContext` is a record, so the two new components change its canonical constructor. External backends written against the published SPI must adapt, or switch to the 3-argument convenience constructor in their tests.
- The behaviour is described in arc42 chapter 6, "Files written once per package"; the glossary entries "Soft assertions mixin" and "Reference object" point there.

## Open points (ideas, not decisions)

- A `Diagnostics` interface instead of the growing list of channels (`errors`, `notes`, `warnings`, `verboseNotes`) in `GenerationContext`.
- Unwrapping `Optional` for the `isXLike` preset: `MatcherGenerator.presetOf` unwraps arrays, collections, lists and the value type of maps, but not `Optional<X>`. `Optional` is a `java.` type, so `unmatchedUserType` treats it as a library type: the property is compared with `equals` and no verbose note is printed, even when `X` is a user class without a generated matcher.
