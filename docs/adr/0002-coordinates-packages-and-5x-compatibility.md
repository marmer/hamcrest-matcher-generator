# Keep the Hamcrest identity, add neutral sibling coordinates, stay source-compatible with 5.x

6.0.0 keeps the project and repository name `hamcrest-matcher-generator` (display name "Matcher Generator") and the group ID `io.github.marmer.testutils`. The new modules get neutral artifact IDs: `matcher-generator-core`, `matcher-generator-annotations`, `hamcrest-matcher-generator` (unchanged, Hamcrest backend and runtime), and `assertj-assertion-generator`. The old `hamcrest-matcher-generator-dependencies` artifact becomes a relocation POM. New Java packages follow the group ID's owner prefix: `io.github.marmer.matchergenerator.{core,annotations,hamcrest,assertj}`. The library-neutral configuration annotation is `@GenerateMatchers` with the 5.x attributes. The deprecated `@MatcherConfiguration` is the only type that stays in the old package `io.github.marmer.testutils.generators.beanmatcher.dependencies`. `BeanPropertyMatcher` moves to the Hamcrest package, because generated code is regenerated anyway.

Compatibility goal: a 5.x user test compiles after a version bump alone, with the same generated class names, packages, and `isX()`/`withFoo`/`resetFoo` API. The artifact switch is the only allowed break, and the relocation POM covers it. Everything else that breaks goes into the migration guide.

Namespace: `io.github.marmer` survived the OSSRH shutdown. It is listed as verified in the Central Portal under the owner's migrated legacy account (checked 2026-10-24), so the `online.marmer` fallback is not needed.

Rejected: a neutral repository rename (it loses the established name for little gain) and a uniform `hamcrest-matcher-generator-*` prefix (it is misleading for the AssertJ artifact).
