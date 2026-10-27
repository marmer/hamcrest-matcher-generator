# Keep the Hamcrest identity, add neutral sibling coordinates, stay source-compatible with 5.x

6.0.0 keeps the project and repository name `hamcrest-matcher-generator` (display name "Matcher Generator") and the group ID `io.github.marmer.testutils`. The new modules get neutral artifact IDs: `matcher-generator-core`, `matcher-generator-annotations`, `hamcrest-matcher-generator` (unchanged, Hamcrest backend and runtime), and `assertj-assertion-generator`. The old `hamcrest-matcher-generator-dependencies` and `hamcrest-matcher-generator-annotationprocessor` artifacts become bridge jars (see the amendment below). New Java packages follow the group ID's owner prefix: `io.github.marmer.matchergenerator.{core,annotations,hamcrest,assertj}`. The library-neutral configuration annotation is `@GenerateMatchers` with the 5.x attributes. The deprecated `@MatcherConfiguration` is the only type that stays in the old package `io.github.marmer.testutils.generators.beanmatcher.dependencies`. `BeanPropertyMatcher` moves to the Hamcrest package, because generated code is regenerated anyway.

Compatibility goal: a 5.x user test compiles after a version bump alone, with the same generated class names, packages, and `isX()`/`withFoo`/`resetFoo` API. The artifact switch is the only allowed break, and the bridge jars cover it. Everything else that breaks goes into the migration guide.

Namespace: `io.github.marmer` survived the OSSRH shutdown. It is listed as verified in the Central Portal under the owner's migrated legacy account (checked 2026-10-24), so the `online.marmer` fallback is not needed.

Rejected: a neutral repository rename (it loses the established name for little gain) and a uniform `hamcrest-matcher-generator-*` prefix (it is misleading for the AssertJ artifact).

## Amendment (2026-10-27): bridge jars instead of relocation POMs

The first version of this decision published the two 5.x artifacts as pom-only relocations. `maven-compiler-plugin` before 3.12 (the project's own 3.8.1, and probably most 5.x users') does not follow a relocation in `<annotationProcessorPaths>` and fails with "Missing: ...hamcrest-matcher-generator-annotationprocessor:jar". That broke the version-bump promise for exactly the users it was made for, and the 5.x consumer build passed only with a compiler plugin override.

`hamcrest-matcher-generator-dependencies` and `hamcrest-matcher-generator-annotationprocessor` are therefore jar-packaged bridge modules. Each publishes a code-free jar (manifest, LICENSE, NOTICE, third-party list, sources and javadoc jar) and depends on `hamcrest-matcher-generator` at the same version. Old and new compiler plugins find a jar on the processor path, the processor arrives transitively, and dependency users get the Hamcrest backend transitively as before. The consumer build runs the frozen 5.x suite with the project's compiler plugin and no override.

Rejected: keeping the relocation POMs and documenting a compiler plugin upgrade (it breaks the version bump for users on older plugins).
