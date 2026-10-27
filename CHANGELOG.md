# Changelog

This project uses semantic versioning. See https://semver.org/

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
