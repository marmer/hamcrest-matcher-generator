Matcher Generator
=================

[![Maven Central (Hamcrest)](https://img.shields.io/maven-central/v/io.github.marmer.testutils/hamcrest-matcher-generator)](https://central.sonatype.com/artifact/io.github.marmer.testutils/hamcrest-matcher-generator)
[![Maven Central (AssertJ)](https://img.shields.io/maven-central/v/io.github.marmer.testutils/assertj-assertion-generator)](https://central.sonatype.com/artifact/io.github.marmer.testutils/assertj-assertion-generator)
[![javadoc](https://javadoc.io/badge2/io.github.marmer.testutils/hamcrest-matcher-generator/javadoc.svg)](https://javadoc.io/doc/io.github.marmer.testutils/hamcrest-matcher-generator)
[![CI](https://github.com/marmer/hamcrest-matcher-generator/actions/workflows/ci-build.yml/badge.svg)](https://github.com/marmer/hamcrest-matcher-generator/actions/workflows/ci-build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=io.github.marmer.testutils:hamcrest-matcher-generator)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/marmer/hamcrest-matcher-generator/badge)](https://securityscorecards.dev/viewer/?uri=github.com/marmer/hamcrest-matcher-generator)
[![License](https://img.shields.io/github/license/marmer/hamcrest-matcher-generator)](LICENSE)

Generate Hamcrest matchers and AssertJ assertions for your model classes. An annotation processor writes them at compile
time, so your production code stays free of test code.

What you get
------------

* **Hamcrest matchers**: `isCustomer().withName("Ada")`, with any Hamcrest matcher as the argument, a completeness check
  that fails when a property is neither checked nor ignored, and `isCustomerLike(reference)` to compare every property.
* **AssertJ assertions**: `CustomerAssert.assertThat(customer).hasName("Ada")`, with a `...Satisfying` form for nested
  objects and a check that every property is checked or ignored.
* **Java and Kotlin**: the processor runs with javac and, for Kotlin test sources, through KAPT. An opt-in Kotlin DSL wraps
  the Hamcrest matchers.
* **Records and Lombok**: getters of Java records and Lombok-generated properties are supported.

Before and after
----------------

Checking a nested property by hand with Hamcrest:

```java
assertThat(ada.getAddress().getCity(), is("Paris"));
```

The generated matcher checks the same property, next to the rest of the object:

<!-- snippet: example-maven-java-hamcrest/src/test/java/example/hamcrest/CustomerMatcherTest.java#nested-property -->
```java
assertThat(ADA, isCustomer().withAddress(isAddress().withCity("Paris")));
```
<!-- end snippet -->

The generated AssertJ assertion and the Kotlin DSL read the same way:

<!-- snippet: example-maven-java-assertj/src/test/java/example/assertj/CustomerAssertTest.java#basic-assertion -->
```java
CustomerAssert.assertThat(ADA).hasName("Ada");
```
<!-- end snippet -->

<!-- snippet: example-maven-kotlin/src/test/kotlin/example/kotlin/CustomerDslTest.kt#basic-assertion -->
```kotlin
assertThat(ada, isCustomer { name("Ada") })
```
<!-- end snippet -->

Quick start
-----------

Generate the matchers or assertions for the packages of your model classes with `@GenerateMatchers`:

<!-- snippet: example-maven-java-hamcrest/src/main/java/example/hamcrest/configuration/HamcrestConfiguration.java#configuration -->
```java
import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

/** Generates a matcher for every model class in {@code example.hamcrest.model}. */
@GenerateMatchers("example.hamcrest.model")
public final class HamcrestConfiguration {}
```
<!-- end snippet -->

The generated code calls the library of its backend, so add `org.hamcrest:hamcrest` or `org.assertj:assertj-core` too, in the version you use.
Set the versions of the project's artifacts with the properties below; the lines carry the release-please marker, so
the release keeps them current.

```xml
<properties>
    <hamcrest-matcher-generator.version>5.1.0</hamcrest-matcher-generator.version> <!-- x-release-please-version -->
    <assertj-assertion-generator.version>5.1.0</assertj-assertion-generator.version> <!-- x-release-please-version -->
</properties>
```

### Maven

Hamcrest: the dependencies, then the annotation processor in the compiler plugin:

<!-- snippet: example-maven-java-hamcrest/pom.xml#maven-dependencies -->
```xml
<dependency>
    <groupId>io.github.marmer.testutils</groupId>
    <artifactId>hamcrest-matcher-generator</artifactId>
    <version>${hamcrest-matcher-generator.version}</version>
</dependency>
<dependency>
    <groupId>org.hamcrest</groupId>
    <artifactId>hamcrest</artifactId>
</dependency>
```
<!-- end snippet -->

<!-- snippet: example-maven-java-hamcrest/pom.xml#maven-processor -->
```xml
<annotationProcessorPaths combine.children="append">
    <path>
        <groupId>io.github.marmer.testutils</groupId>
        <artifactId>hamcrest-matcher-generator</artifactId>
        <version>${hamcrest-matcher-generator.version}</version>
    </path>
</annotationProcessorPaths>
```
<!-- end snippet -->

AssertJ, the same way:

<!-- snippet: example-maven-java-assertj/pom.xml#maven-dependencies -->
```xml
<dependency>
    <groupId>io.github.marmer.testutils</groupId>
    <artifactId>assertj-assertion-generator</artifactId>
    <version>${assertj-assertion-generator.version}</version>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
</dependency>
```
<!-- end snippet -->

<!-- snippet: example-maven-java-assertj/pom.xml#maven-processor -->
```xml
<annotationProcessorPaths combine.children="append">
    <path>
        <groupId>io.github.marmer.testutils</groupId>
        <artifactId>assertj-assertion-generator</artifactId>
        <version>${assertj-assertion-generator.version}</version>
    </path>
</annotationProcessorPaths>
```
<!-- end snippet -->

### Gradle

<!-- snippet: example-gradle-java/build.gradle.kts#gradle-version -->
```kotlin
val matcherGeneratorVersion = "5.1.0" // x-release-please-version
```
<!-- end snippet -->

<!-- snippet: example-gradle-java/build.gradle.kts#gradle-dependencies -->
```kotlin
implementation("io.github.marmer.testutils:hamcrest-matcher-generator:$matcherGeneratorVersion")
implementation("io.github.marmer.testutils:assertj-assertion-generator:$matcherGeneratorVersion")
```
<!-- end snippet -->

<!-- snippet: example-gradle-java/build.gradle.kts#gradle-processor -->
```kotlin
// Both backends run as annotation processors; the Gradle annotationProcessor configuration puts them on the processor path.
annotationProcessor("io.github.marmer.testutils:hamcrest-matcher-generator:$matcherGeneratorVersion")
annotationProcessor("io.github.marmer.testutils:assertj-assertion-generator:$matcherGeneratorVersion")
```
<!-- end snippet -->

Generated code
--------------

The matchers and assertions generated for your classes belong to you. The project's Apache-2.0 license covers the libraries,
not the code the processor writes from your classes.

Documentation
-------------

* The [user guide](https://marmer.github.io/hamcrest-matcher-generator/guide/getting-started.html) starts with a first green test
  and covers the backends, the configuration and the IDE setup.
* The architecture is documented with arc42 on the [same site](https://marmer.github.io/hamcrest-matcher-generator/).

Coming from 5.x
---------------

Java 11 users stay on 5.x: 6.0.0 needs JDK 17 or higher. The 5.x artifacts `hamcrest-matcher-generator-annotationprocessor`
and `hamcrest-matcher-generator-dependencies` still work as bridge jars: they hold no code and bring in
`hamcrest-matcher-generator` at the same version. `@MatcherConfiguration` remains as a deprecated alias of
`@GenerateMatchers`. Further migration notes go into the [user guide](https://marmer.github.io/hamcrest-matcher-generator/).

Kotlin-JVM
----------

The generation works with Kotlin Projekts as well. If you write your testcode in Java, you don't have to change anything
else. If you write them in Kotlin, you have to configure kotlin test-kapt. It works
like [kapt](https://kotlinlang.org/docs/kapt.html).

Generated matchers are annotated with JSpecify (`@NullMarked`, `@Nullable` on equality values and nullable properties) when
JSpecify is on the classpath, so Kotlin sees real nullable and non-null types and `withFoo(null)` compiles. Kotlin treats
JSpecify mismatches as errors by default; if that gets in your way, `-Xjspecify-annotations=warn` lowers them to warnings.

You can find an example [here](hamcrest-matcher-generator-endtoend-mixed-kotlin-java/pom.xml)

Additionally, you have to add the output directories for the test-compile execution of the kotlin-maven-plugin:

```xml

<execution>
    <id>test-compile</id>
    <goals>
        <goal>test-compile</goal>
    </goals>
    <configuration>
        <sourceDirs>
            <sourceDir>${project.basedir}/src/test/kotlin</sourceDir>
            <sourceDir>${project.basedir}/src/test/java</sourceDir>
            <sourceDir>${project.build.directory}/generated-sources/kapt/test</sourceDir>
            <sourceDir>${project.build.directory}/generated-sources/kaptKotlin/test</sourceDir>
            <sourceDir>${project.build.directory}/generated-test-sources/test-annotations
        </sourceDirs>
    </configuration>
</execution>

```

Requirements
============

Build tool
---------
You can use this library with the build tool of your choice or even with javac. This library is capable of annotation
 processing, and the matchers are generated at compile time, similar to libraries such as Lombok and Mapstruct.

IDE
---
Use the IDE of your choice. The user guide's [IDE setup chapter](https://marmer.github.io/hamcrest-matcher-generator/guide/ide-setup.html) covers Eclipse and IntelliJ IDEA, and its [Kotlin chapter](https://marmer.github.io/hamcrest-matcher-generator/guide/kotlin.html) covers the IntelliJ workaround for Kotlin tests.

JDK
---

JDK 17 or higher is required to use the generated source code (since 6.0.0; 5.x supports JDK 11).

Hamcrest
--------
Because Hamcrest matchers are generated, you will also need a dependency on the library in order to be able to use the generated sources. You are free to choose your own version of Hamcrest.

Your project should use a Hamcrest version starting with 1.2. The resulting code will not work without Hamcrest.

You may copy this dependency if you want.

```xml
<dependency>
    <groupId>org.hamcrest</groupId>
    <artifactId>hamcrest</artifactId>
    <version>2.2</version>
    <scope>test</scope>
</dependency>    
```

### Changelog

The release history is maintained in [CHANGELOG.md](CHANGELOG.md). This project uses semantic versioning. See https://semver.org/

### Maintaining

The maintainer runbook, with the credentials and the playbooks for failed checks and releases, is in [MAINTAINING.md](MAINTAINING.md).

Maintainer
----------

Mariano Mertinat maintains the project. [GitHub](https://github.com/marmer) and
[LinkedIn](https://de.linkedin.com/in/mariano-mertinat)
