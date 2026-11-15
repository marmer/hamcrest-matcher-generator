# Changelog

This project uses semantic versioning. See https://semver.org/

## [6.0.0](https://github.com/marmer/hamcrest-matcher-generator/compare/5.1.0...6.0.0) (2026-11-15)


### ⚠ BREAKING CHANGES

* 6.0.0 batches 1-3: core + Hamcrest backend SPI, new coordinates, diagnostics, Central release pipeline, docs site ([#148](https://github.com/marmer/hamcrest-matcher-generator/issues/148))

### Features

* 5.x artifacts as bridge jars instead of relocation POMs ([#160](https://github.com/marmer/hamcrest-matcher-generator/issues/160)) ([0e18db5](https://github.com/marmer/hamcrest-matcher-generator/commit/0e18db57c18437ce3903a940796f7ae7ba34c70a))
* 6.0.0 batches 1-3: core + Hamcrest backend SPI, new coordinates, diagnostics, Central release pipeline, docs site ([#148](https://github.com/marmer/hamcrest-matcher-generator/issues/148)) ([f9efac4](https://github.com/marmer/hamcrest-matcher-generator/commit/f9efac4b4a154017de6fa05d87731839c4a9d77e))
* **annotations:** SOURCE retention for @GenerateMatchers ([#295](https://github.com/marmer/hamcrest-matcher-generator/issues/295)) ([#296](https://github.com/marmer/hamcrest-matcher-generator/issues/296)) ([cb2084d](https://github.com/marmer/hamcrest-matcher-generator/commit/cb2084d7d0014cfc88132041aa3e43399f6903ae))
* **assertj:** AssertJ backend tracer bullet with AbstractXAssert and XAssert ([#182](https://github.com/marmer/hamcrest-matcher-generator/issues/182)) ([c5e5af6](https://github.com/marmer/hamcrest-matcher-generator/commit/c5e5af611ae66543492aa006678a61429c44b9d3))
* **assertj:** boolean assertions, element-type paths and inherited-name clash errors ([#184](https://github.com/marmer/hamcrest-matcher-generator/issues/184)) ([dff2288](https://github.com/marmer/hamcrest-matcher-generator/commit/dff2288ea515181a0202290c96c0445034157db7))
* **assertj:** completeness check with ignoringFoo, satisfiesCheckingAllProperties and hasAllPropertiesChecked ([#186](https://github.com/marmer/hamcrest-matcher-generator/issues/186)) ([226f94f](https://github.com/marmer/hamcrest-matcher-generator/commit/226f94f28039e432a362b9b6fb717c70e01d0a44))
* **assertj:** generate an opt-in PropertyRepresentation per package ([#197](https://github.com/marmer/hamcrest-matcher-generator/issues/197)) ([4d03cd0](https://github.com/marmer/hamcrest-matcher-generator/commit/4d03cd07eeba551b77ab7443f145d77ea60a5d5b))
* **assertj:** report clashes among generated names and factory constants ([#219](https://github.com/marmer/hamcrest-matcher-generator/issues/219)) ([88f64c8](https://github.com/marmer/hamcrest-matcher-generator/commit/88f64c8052ec7a4cfb16dc699dd681318cdb3e53))
* **assertj:** report generated class names claimed twice in one package as one error ([#257](https://github.com/marmer/hamcrest-matcher-generator/issues/257)) ([563ae5e](https://github.com/marmer/hamcrest-matcher-generator/commit/563ae5e3538704b2f2af0e60a22c34be0c933874))
* **core:** exclude attribute on @GenerateMatchers and the deprecated alias ([#153](https://github.com/marmer/hamcrest-matcher-generator/issues/153)) ([7884480](https://github.com/marmer/hamcrest-matcher-generator/commit/78844802df8c231165ad57cfa6d0561baf41145b))
* **core:** make the mixin and isXLike failure modes across rounds explicit ([#252](https://github.com/marmer/hamcrest-matcher-generator/issues/252)) ([c02267c](https://github.com/marmer/hamcrest-matcher-generator/commit/c02267caf3b9ae9e5b957fd06166f7f929dc1ba1))
* **examples:** Maven Java example modules for Hamcrest and AssertJ ([#223](https://github.com/marmer/hamcrest-matcher-generator/issues/223)) ([39410be](https://github.com/marmer/hamcrest-matcher-generator/commit/39410bec99c24e631d7425759d3a7b6a067c4c0a))
* **examples:** Maven Kotlin example with KAPT and DSL, Gradle Java example with CI job ([#248](https://github.com/marmer/hamcrest-matcher-generator/issues/248)) ([8abcd57](https://github.com/marmer/hamcrest-matcher-generator/commit/8abcd57dc364d0ef9426b3882806d0dcf67a4b08))
* Hamcrest mismatch messages list failing checks with property paths ([#155](https://github.com/marmer/hamcrest-matcher-generator/issues/155)) ([181133d](https://github.com/marmer/hamcrest-matcher-generator/commit/181133d9fd5ae49b9bcf0a5127193c166b6b465f))
* **hamcrest:** generated matchers read properties through accessors ([#361](https://github.com/marmer/hamcrest-matcher-generator/issues/361)) ([#378](https://github.com/marmer/hamcrest-matcher-generator/issues/378)) ([97f1edf](https://github.com/marmer/hamcrest-matcher-generator/commit/97f1edfa0320276bd3dea2e158c51811a7cf1edb))
* **hamcrest:** opt-in completeness check with ignoringFoo() and checkingAllProperties() ([#171](https://github.com/marmer/hamcrest-matcher-generator/issues/171)) ([d3a3575](https://github.com/marmer/hamcrest-matcher-generator/commit/d3a35753c3cd13e2a2eccedc0e1ebd648f2f940f))
* **hamcrest:** reference-object matcher isXLike with deep, cycle-safe presets ([#180](https://github.com/marmer/hamcrest-matcher-generator/issues/180)) ([fa1556d](https://github.com/marmer/hamcrest-matcher-generator/commit/fa1556dfa3a7325075a450fe1e4f0ea732c19218))
* **hamcrest:** runtime collection matchers itemsInOrder, itemsInAnyOrder and entries ([#170](https://github.com/marmer/hamcrest-matcher-generator/issues/170)) ([1a3f27a](https://github.com/marmer/hamcrest-matcher-generator/commit/1a3f27aeb77f50f2dbf5c4820fca199f1d7f1bfa))
* JSpecify nullness in the property model and the Hamcrest output ([#154](https://github.com/marmer/hamcrest-matcher-generator/issues/154)) ([3065e49](https://github.com/marmer/hamcrest-matcher-generator/commit/3065e498d17532a61279ad191d81eb83b4755151))
* **kotlin:** DSL edge cases, clash warnings and compile-failure checks ([#202](https://github.com/marmer/hamcrest-matcher-generator/issues/202)) ([d2ce5d2](https://github.com/marmer/hamcrest-matcher-generator/commit/d2ce5d2d771c3f79f6b75ceab7b19c4d049cf39b))
* **kotlin:** Kotlin DSL backend writing XMatcherDsl.kt under KAPT ([#198](https://github.com/marmer/hamcrest-matcher-generator/issues/198)) ([fbc6d7d](https://github.com/marmer/hamcrest-matcher-generator/commit/fbc6d7d5ce439996c88e93067f0db5199ceeb444))
* license gates, NOTICE and third-party files in every jar ([#156](https://github.com/marmer/hamcrest-matcher-generator/issues/156)) ([6a6a576](https://github.com/marmer/hamcrest-matcher-generator/commit/6a6a576425ea2382f7a569de9597510e10a73cc4))


### Bug Fixes

* **assertj:** a boolean isClass() property is not a name clash ([#338](https://github.com/marmer/hamcrest-matcher-generator/issues/338)) ([#343](https://github.com/marmer/hamcrest-matcher-generator/issues/343)) ([5dff1c7](https://github.com/marmer/hamcrest-matcher-generator/commit/5dff1c77c6ae430db1ae5fb1a5f9952ed5d10c60))
* **assertj:** scope the assertion path by identity and document completeness after navigation ([#244](https://github.com/marmer/hamcrest-matcher-generator/issues/244)) ([e04c32c](https://github.com/marmer/hamcrest-matcher-generator/commit/e04c32cf97ae6f763702f7427e3983dabfdb31c3))
* **core:** a non-public top level type is not generated into another package ([#357](https://github.com/marmer/hamcrest-matcher-generator/issues/357)) ([#368](https://github.com/marmer/hamcrest-matcher-generator/issues/368)) ([01804f9](https://github.com/marmer/hamcrest-matcher-generator/commit/01804f9f6bc9f03aaa54d94f910da030108cccf9))
* **core:** a skipped getter of a named type is a warning ([#359](https://github.com/marmer/hamcrest-matcher-generator/issues/359)) ([#374](https://github.com/marmer/hamcrest-matcher-generator/issues/374)) ([f024225](https://github.com/marmer/hamcrest-matcher-generator/commit/f0242253a8f42bfa0cdc9c6c45637518afe83dca))
* **core:** an unresolvable configured name waits for later rounds ([#360](https://github.com/marmer/hamcrest-matcher-generator/issues/360)) ([#375](https://github.com/marmer/hamcrest-matcher-generator/issues/375)) ([645442a](https://github.com/marmer/hamcrest-matcher-generator/commit/645442aa31b5dea161f039810c6ab505f4745380))
* **core:** detect record components named is… or get… as properties ([#218](https://github.com/marmer/hamcrest-matcher-generator/issues/218)) ([bc22f94](https://github.com/marmer/hamcrest-matcher-generator/commit/bc22f944a4144528ffb1da667e1ab712198a5632))
* **core:** generated sources carry no generation date ([#362](https://github.com/marmer/hamcrest-matcher-generator/issues/362)) ([#381](https://github.com/marmer/hamcrest-matcher-generator/issues/381)) ([1081971](https://github.com/marmer/hamcrest-matcher-generator/commit/1081971935555ea0dd76f62a34e7864783dab8ca))
* **core:** inherited generic properties keep their type argument ([#358](https://github.com/marmer/hamcrest-matcher-generator/issues/358)) ([#373](https://github.com/marmer/hamcrest-matcher-generator/issues/373)) ([f644528](https://github.com/marmer/hamcrest-matcher-generator/commit/f644528f1fe4d797ade0447f8ebf71e2171b2360))
* **deps:** update dependency com.google.guava:guava to v33.7.2-android [security] ([#213](https://github.com/marmer/hamcrest-matcher-generator/issues/213)) ([ea4e7e4](https://github.com/marmer/hamcrest-matcher-generator/commit/ea4e7e4d3e3a324b57ee476fde4ca38f6e847133))
* **deps:** update dependency io.github.classgraph:classgraph to v4.8.112 [security] ([#211](https://github.com/marmer/hamcrest-matcher-generator/issues/211)) ([721beeb](https://github.com/marmer/hamcrest-matcher-generator/commit/721beebc8f8f168542551b60e028bd16fb95dd60))
* **generator:** a getter that declares a checked exception generates compilable code ([#376](https://github.com/marmer/hamcrest-matcher-generator/issues/376)) ([#390](https://github.com/marmer/hamcrest-matcher-generator/issues/390)) ([f9345c5](https://github.com/marmer/hamcrest-matcher-generator/commit/f9345c5edcb336d4fa957930ce7768eee154ee02))
* **hamcrest:** completeness check exempts only the class property ([#333](https://github.com/marmer/hamcrest-matcher-generator/issues/333)) ([#342](https://github.com/marmer/hamcrest-matcher-generator/issues/342)) ([e9c30cb](https://github.com/marmer/hamcrest-matcher-generator/commit/e9c30cb79233f6de2e5479588c1d2c7301ab59a5))
* **hamcrest:** the printing registry does not keep class loaders alive ([#377](https://github.com/marmer/hamcrest-matcher-generator/issues/377)) ([#391](https://github.com/marmer/hamcrest-matcher-generator/issues/391)) ([d52d584](https://github.com/marmer/hamcrest-matcher-generator/commit/d52d5843c1d6ceab45907af8193c0a23e844dc8b))
* **kotlin:** name a nested type with a clashing simple name after its enclosing types ([#224](https://github.com/marmer/hamcrest-matcher-generator/issues/224)) ([b5528b9](https://github.com/marmer/hamcrest-matcher-generator/commit/b5528b97b62b735302fa40bdf9b435c5bbb4b7d2))
* **kotlin:** report Kotlin DSL names that still collide after prefixing as one error ([#240](https://github.com/marmer/hamcrest-matcher-generator/issues/240)) ([396d64c](https://github.com/marmer/hamcrest-matcher-generator/commit/396d64c5f4b4574b4c045590ea38bc6cb702dbc8))
* **kotlin:** the DSL's completeness check ignores the properties the DSL leaves out ([#341](https://github.com/marmer/hamcrest-matcher-generator/issues/341)) ([#345](https://github.com/marmer/hamcrest-matcher-generator/issues/345)) ([dceab0c](https://github.com/marmer/hamcrest-matcher-generator/commit/dceab0c1110c7321d0ae3f877771bdd4bd27fa06))

## [5.1.0](https://github.com/marmer/hamcrest-matcher-generator/compare/e1514ce...5.1.0) (2023-08-28)

* Ability to reset properties with *reset** methods to be able to set/reconfigure individual properties at complex matcher configurations

## [5.0.0](https://github.com/marmer/hamcrest-matcher-generator/compare/3afa566...e1514ce) (2021-06-25)

* Codebase migrated to Java 11 and Kotlin 1.5
* Unnecessary MatcherConfigurations removed
* Useless BasedOn Annotation removed
* Sample modules for plain java, plain kotlin and mixed java-kotlin
* Generics for better IDE and Compiler support when matchers are used
* Fixed: Handling of a duplicate property (e.g. boolean isPropName() and String getPropName())

## [4.2.5](https://github.com/marmer/hamcrest-matcher-generator/compare/4.2.4...3afa566) (2021-03-17)

* No processor changes; re-release of 4.2.4, whose release notes document the inheritance revert

## [4.2.4](https://github.com/marmer/hamcrest-matcher-generator/compare/4.2.3...4.2.4) (2020-11-18)

* Inheritance changes reverted because it looks like an error in hamcrest. Workaround is to cast the first element
  within a list in "contains" to "Matcher" without any generic information
* README uses the `org.hamcrest:hamcrest` 2.2 dependency and the library version property in its dependency snippets

## [4.2.3](https://github.com/marmer/hamcrest-matcher-generator/compare/4.2.2...4.2.3) (2020-10-11)

* Problems with inheritance fixed (Matchers.contains did not work for parent collections with different child types)
* ability to set a package prefix because jigsaw does not allow the generation of types within packages of other modules

## [4.2.2](https://github.com/marmer/hamcrest-matcher-generator/compare/4.2.1...4.2.2) (2019-10-17)

* ability to create/use/handle javax.annotations.Generated annotations with newer JDKs added

## [4.2.1](https://github.com/marmer/hamcrest-matcher-generator/compare/4.2.0...4.2.1) (2019-07-18)

* Handling of arrays at primitives fixed

## [4.2.0](https://github.com/marmer/hamcrest-matcher-generator/compare/4.1.0...4.2.0) (2019-05-03)

* Logging happens with a prefix so that the user is able to distinguish between errors of this annotation processor and
  others
* Annotation-based logs without more concrete elements contain line and column information of the annotated element

## [4.1.0](https://github.com/marmer/hamcrest-matcher-generator/compare/4.0.7...4.1.0) (2019-04-28)

* Possibility added to change the "base package" in which the generated Matchers are placed with some package postfix (which can be a complete package)

## [4.0.7](https://github.com/marmer/hamcrest-matcher-generator/compare/4.0.6...4.0.7) (2019-04-19)

* no more MatcherMatcher...Matcher-ception

## [4.0.6](https://github.com/marmer/hamcrest-matcher-generator/compare/4.0.5...4.0.6) (2019-04-06)

* Inherited properties can change types now

## [4.0.5](https://github.com/marmer/hamcrest-matcher-generator/compare/4.0.4...4.0.5) (2019-03-23)

* Handling of generic superclasses
* Handling of generic superinterfaces
* better handling of primitive properties
* more stability on unknown errors

## [4.0.4](https://github.com/marmer/hamcrest-matcher-generator/compare/4.0.3...4.0.4) (2019-03-22)

* back to compiler warnings for *full* backward compatibility with jdk8

## [4.0.3](https://github.com/marmer/hamcrest-matcher-generator/compare/4.0.2...4.0.3) (2019-03-21)

* Compiler warnings on jdk 11 use removed

## [4.0.2](https://github.com/marmer/hamcrest-matcher-generator/compare/4.0.1...4.0.2) (2019-03-16)

* Generation for matchers at configured inner classes
* Handling of Properties of type org.hamcrest.Matcher
* Handling of generic properties

## [4.0.1](https://github.com/marmer/hamcrest-matcher-generator/compare/4.0.0...4.0.1) (2019-03-12)

* Properties can be inner types too now

## [4.0.0](https://github.com/marmer/hamcrest-matcher-generator/compare/3.2.0...4.0.0) (2019-03-12)

* Reboot of the project
* Generation triggered by an annotation processor to run independent of any build tool
* Packages of matchers for inner classes are generated as inner matchers
* Fixed: matchers are not generated for non-public types
* Fixed: matcher methods are not generated for non-public properties

## [3.2.0](https://github.com/marmer/hamcrest-matcher-generator/compare/9acc0c3...3.2.0) (2018-11-11)

* Naming Strategy for classnames instead of package names implemented (PARENT)

## [3.1.2](https://github.com/marmer/hamcrest-matcher-generator/compare/3.1.1...9acc0c3) (2018-11-10)

* Bug fixed with the generation of some kind at subclasses

## [3.1.1](https://github.com/marmer/hamcrest-matcher-generator/compare/3.1.0...3.1.1) (2018-11-09)

* A little more logging

## [3.1.0](https://github.com/marmer/hamcrest-matcher-generator/compare/3.0.1...3.1.0) (2018-11-08)

* Added error logging for (some) code errors

## [3.0.1](https://github.com/marmer/hamcrest-matcher-generator/compare/3.0.0...3.0.1) (2018-11-07)

* Bugfix: useless class compilation at the end of the generation process removed.

## [3.0.0](https://github.com/marmer/hamcrest-matcher-generator/compare/2.0.0...3.0.0) (2018-11-06)

* Matchers generated with the package naming strategy will and with the postfix "Matcher" again.

## [2.0.0](https://github.com/marmer/hamcrest-matcher-generator/compare/1.2.2...2.0.0) (2018-11-05)

* Support for multiple inner classes with the same name added by using different naming strategies. For backward
  compatibility use <namingStrategy>PLAIN</namingStrategy> which is the old strategy

## 1.3.0 (not released separately)

* Support for Matcher-Generation for Interfaces

## [1.2.2](https://github.com/marmer/hamcrest-matcher-generator/compare/1.2.1...1.2.2) (2017-07-20)

* Excluded java.lang classes from generation because it's not allowed

## [1.2.1](https://github.com/marmer/hamcrest-matcher-generator/compare/1.1.0...1.2.1) (2017-07-20)

* Generated matchers are logged after generation and returned by the generator
* Property documentation added

## [1.1.0](https://github.com/marmer/hamcrest-matcher-generator/compare/1.0.0...1.1.0) (2017-07-19)

* Generation of convenience methods taking values instead of matchers
* Convenience methods are not generated for properties of type Matcher itself
* Signing is skipped by default

## [1.0.0](https://github.com/marmer/hamcrest-matcher-generator/tree/1.0.0) (2017-07-12)

* Initial release
