![CI-Build](https://github.com/marmer/hamcrest-matcher-generator/workflows/CI-Build/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.marmer.testutils/hamcrest-matcher-generator)](https://central.sonatype.com/artifact/io.github.marmer.testutils/hamcrest-matcher-generator)
[![Documentation](https://img.shields.io/badge/docs-arc42-blue)](https://marmer.github.io/hamcrest-matcher-generator/)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=io.github.marmer.testutils:hamcrest-matcher-generator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=io.github.marmer.testutils:hamcrest-matcher-generator)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=io.github.marmer.testutils:hamcrest-matcher-generator)

[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=io.github.marmer.testutils:hamcrest-matcher-generator)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=io.github.marmer.testutils:hamcrest-matcher-generator)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=io.github.marmer.testutils:hamcrest-matcher-generator)

hamcrest-matcher-generator
==========================
This library allows you to generate Hamcrest matchers without forcing you to pollute your production code. 

Bean Property Matcher
---------------------

Have you ever wished you could...

* magically produce Hamcrest Matchers for all (or just some) of your model or service classes?
    * produce such matchers without polluting your production code with test code annotations?
    * generate types which are not part of the current source code?
* test your models with hamcrest in an atomic way with informative error messages?
* have a compile-safe alternative to Hamcrest's "hasProperty" or HasPropertyWithValue?

Properties of Lombok annotated classes are supported as well (tested with version 1.18.4)

How to use
==========
All you need to do is to add a single dependency and an Annotation for the configuration for those types the matchers have
to be generated for.

Dependencies
----------

If you wish to use the matchers in your test code only, simply add this dependency to your project:

```xml

<dependency>
    <groupId>io.github.marmer.testutils</groupId>
    <artifactId>hamcrest-matcher-generator-annotationprocessor</artifactId>
    <version>${hamcrest-matcher-generator.version}</version>
    <scope>test</scope>
</dependency>
```

If you wish to use it in your production code as well, you should declare the scope as "provided" to avoid unnecessary
dependencies. The generated matchers are self-contained — at runtime they only need Hamcrest, no additional library.

```xml

<dependency>
    <groupId>io.github.marmer.testutils</groupId>
    <artifactId>hamcrest-matcher-generator-annotationprocessor</artifactId>
    <version>${hamcrest-matcher-generator.version}</version>
    <scope>provided</scope>
    <optional>true</optional>
</dependency>
```

Configuration
-------------
Simply create a Class or Interface with one or more `@MatcherConfiguration` and add either fully-qualified class names or
packages for the types you wish to generate matchers for e.g.

```java

import io.github.marmer.annotationprocessing.MatcherConfiguration;

@MatcherConfiguration({
    "foo.bar.sample.model.SomePojo",
    "foo.bar.sample.model.ParentPojo",
    "foo.bar.sample.model.SomePojoInterface",
    "foo.bar.sample.model.SomeLombokPojo",
})
public class PackageConfiguration {

}
```

Depending on where you place the configuration file, the generated matchers will be created either within generated-test-sources
(if you place it inside your test sources directory) or in generated-sources (if you place it inside your production code source directory). In maven
 projects, this is the default behavior. It may be different with other build tools or with a non-default configuration, but it
should work for other build tools in a similar way)

Generated result
----------------
Assuming you have a pojo like this one with the configuration above...

```java
    package foo.bar.sample.model;

public class SomePojo extends ParentPojo {

    private String pojoField;

    public String getPojoField() {
        return pojoField;
    }
}
```

... a Matcher named SomePojoMatcher is generated within the same package which you can use in your test in the following way:

```java
    final SomePojo somePojo=new SomePojo();
    somePojo.setPojoField("pojoFieldValue");
    somePojo.setParentField("someParentFieldValue");

    // Assertion
    assertThat(somePojo,isSomePojo()
    .withClass(SomePojo.class)
    .withParentField("someParentFieldValue")
    .withParentField(is(equalTo("someParentFieldValue")))
    .withPojoField("pojoFieldValue")
    .withPojoField(is(equalTo("pojoFieldValue")))
    );
```

This example shows a way to match the class, the values (equality) for the direct field as well as for parent fields and
for matchers for each field.

Reference object matcher
------------------------
To compare a whole object against a reference instance, every generated matcher provides an `is<Type>EqualTo` factory
method (and a `withAllPropertiesOf` instance method). It initializes the matcher with an equality expectation for every
generated property of the reference object. Individual expectations can be overridden afterwards via `reset<Property>()`
and `with<Property>(...)`:

```java
assertThat(actual, isSomePojoEqualTo(expected));

// equivalent to
assertThat(actual, isSomePojo().withAllPropertiesOf(expected));

// with an overridden expectation for one property
assertThat(actual, isSomePojoEqualTo(expected)
    .resetPojoField()
    .withPojoField("someOtherValue"));
```

Combined with the property-diff mismatch messages, whole-object comparison with a readable diff becomes a one-liner.
The `class` property is not part of the reference comparison (the matcher already checks the instance type).

Exclude configuration
---------------------
Single types or packages (including their subpackages) can be exempted from a package scan with the `exclude` attribute:

```java
@MatcherConfiguration(
    value = "foo.bar.model",
    exclude = {"foo.bar.model.internal", "foo.bar.model.LegacyThing"}
)
public class PackageConfiguration {
}
```

Strict mode
-----------
An opt-in mode that additionally fails when the matched object has generated properties for which *no* expectation was
configured — protecting tests from silently ignoring newly added fields. The mismatch output lists the unchecked
properties (the `class` property is exempt):

```java
assertThat(actual, isSomePojo().strict().withFoo("bar"));
// fails with e.g.:
// baz: unchecked property (strict mode)
```

Kotlin-JVM
----------

The generation works with Kotlin Projekts as well. If you write your testcode in Java, you don't have to change anything
else. If you write them in Kotlin, you have to configure kotlin test-kapt. It works
like [kapt](https://kotlinlang.org/docs/kapt.html).

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
        </sourceDirs>
    </configuration>
</execution>

```

To have javac compile the matchers generated by KAPT (and to make them visible to your IDE), register the KAPT test
output as a test source root with the `build-helper-maven-plugin`:

```xml

<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>add-kapt-test-sources</id>
            <phase>generate-test-sources</phase>
            <goals>
                <goal>add-test-source</goal>
            </goals>
            <configuration>
                <sources>
                    <source>${project.build.directory}/generated-sources/kapt/test</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Requirements
============

Build tool
---------
You can use this library with the build tool of your choice or even with javac. This library is capable of annotation
 processing, and the matchers are generated at compile time, similar to libraries such as Lombok and Mapstruct.

IDE
---
Use the IDE of your choice. Each IDE with annotation processing capabilities should be able to perform the generation automatically when the project builds. Some IDEs may need a little help, however. Eclipse, for example, may only be capable of annotation processing for Maven projects as long as you have already installed a maven plugin [m2e-apt](https://marketplace.eclipse.org/content/m2e-apt). With the support of the `build-helper-maven-plugin`, however, you can configure the IDE to tell it where to look for the sources generated by Maven. 

### Eclipse

Eclipse may be only capable of annotation processing in maven projects if you have installed a maven
plugin [m2e-apt,](https://marketplace.eclipse.org/content/m2e-apt) but you don't have the IDO to perform the processing.
With a little help of the `build-helper-maven-plugin` you can tell the IDE where to look for sources generated by Maven.

```xml

<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>add-test-source</id>
            <phase>generate-test-sources</phase>
            <goals>
                <goal>add-test-source</goal>
            </goals>
            <configuration>
                <sources>
                    <source>${project.build.directory}/generated-sources/test-annotations/source>
                    <source>${project.build.directory}/generated-test-sources/test-annotations/source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin> 
``` 

### Intellij

In Plain Java projects, you don't have to do anything at all. In Mixed Java/Kotlin projects, you don't have to do anything
if all of your tests are written in Java. If the tests are written in Kotlin, a little hack is needed because the IDE
does not know about the default generation paths for test sources generated by kapt, and kapt currently does not allow
generation output paths.

The `build-helper-maven-plugin` configuration shown in the Kotlin-JVM section above makes the generated sources visible
to the IDE, so no additional hack is needed.

JDK
---

JDK17 or higher is required, both for the annotation processor and to use the generated source code. If you are stuck
on an older JDK, stay on version 5.x of this library (JDK11) or 4.x (JDK8).

Hamcrest
--------
Because Hamcrest matchers are generated, you will also need a dependency on the library in order to be able to use the generated sources. You are free to choose your own version of Hamcrest.

This library is built and tested against Hamcrest 3.0. The resulting code will not work without Hamcrest.

You may copy this dependency if you want.

```xml
<dependency>
    <groupId>org.hamcrest</groupId>
    <artifactId>hamcrest</artifactId>
    <version>3.0</version>
    <scope>test</scope>
</dependency>    
```

### Changelog

This project uses semantic versioning. See https://semver.org/

### 6.0.0 (in progress)

Migration guide 5.x → 6.0.0:

* **Java 17 required** (annotation processor and generated code). Java 11 users stay on 5.x.
* **One dependency instead of two**: remove `hamcrest-matcher-generator-dependencies`; keep only
  `hamcrest-matcher-generator-annotationprocessor` (scope `test`, or `provided` for production-code usage).
* **Import change**: `@MatcherConfiguration` moved to `io.github.marmer.annotationprocessing.MatcherConfiguration`
  (inside the annotation-processor artifact).
* Generated matchers are now **self-contained** — they no longer reference a runtime library; only Hamcrest is needed
  at test runtime. Built and tested against Hamcrest 3.0.
* **Mismatch message format changed** to a multi-line property diff: only the *failing* properties are listed, one per
  line, each with expected vs. actual (e.g. `pojoField: expected "bar" but was "baz"`). Assertions on the old message
  text need updating.
* **New: reference object matcher** — `is<Type>EqualTo(expected)` / `withAllPropertiesOf(expected)` initialize the
  matcher with equality expectations for all generated properties of a reference instance.
* **New: exclude configuration** — `@MatcherConfiguration(exclude = ...)` exempts types or (sub)packages from a scan.
* **New: strict mode** — `is<Type>().strict()` additionally fails on generated properties without configured
  expectations and lists them in the mismatch output.

### 5.1.0
* Ability to reset properties with *reset** methods to be able to set/reconfigure individual properties at complex matcher configurations

### 5.0.0

* Codebase migrated to Java 11 and Kotlin 1.5
* Unnecessary MatcherConfigurations removed
* Useless BasedOn Annotation removed
* Sample modules for plain java, plain kotlin and mixed java-kotlin
* Generics for better IDE and Compiler support when matchers are used
* Fixed: Handling of a duplicate property (e.g. boolean isPropName() and String getPropName())

### 4.2.5

* Inheritance changes reverted because it looks like an error in hamcrest. Workaround is to cast the first element
  within a list in "contains" to "Matcher" without any generic information

### 4.2.4

* Inheritance changes reverted because it looks like an error in hamcrest. Workaround is to cast the first element
  within a list in "contains" to "Matcher" without any generic information

### 4.2.3

* Problems with inheritance fixed (Matchers.contains did not work for parent collections with different child types)
* ability to set a package prefix because jigsaw does not allow the generation of types within packages of other modules

### 4.2.2

* ability to create/use/handle javax.annotations.Generated annotations with newer JDKs added

### 4.2.1

* Handling of arrays at primitives fixed

### 4.2.0

* Logging happens with a prefix so that the user is able to distinguish between errors of this annotation processor and
  others
* Annotation-based logs without more concrete elements contain line and column information of the annotated element

### 4.1.0

* Possibility added to change the "base package" in which the generated Matchers are placed with some package postfix (which can be a complete package)

### 4.0.7

* no more MatcherMatcher...Matcher-ception

### 4.0.6

* Inherited properties can change types now

### 4.0.5

* Handling of generic superclasses
* Handling of generic superinterfaces
* better handling of primitive properties
* more stability on unknown errors

### 4.0.4

* back to compiler warnings for *full* backword compatibility with jdk8

### 4.0.3

* Compiler warnings on jdk 11 use removed

### 4.0.2

* Generation for matchers at configured inner classes
* Handling of Properties of type org.hamcrest.Matcher
* Handling of generic properties

### 4.0.1

* Properties can be inner types too now

### 4.0.0

* Reboot of the project
* Generation triggered by an annotation processor to run independent of any build tool
* Packages of matchers for inner classes are generated as inner matchers
* Fixed: matchers are not generated for non-public types
* Fixed: matcher methods are not generated for non-public properties

### 3.2.0

* Naming Strategy for classnames instead of package names implemented (PARENT)

### 3.1.2

* Bug fixed with the generation of some kind at subclasses

### 3.1.1

* A little more logging

### 3.1.0

* Added error logging for (some) code errors

### 3.0.1

* Bugfix: useless class compilation at the end of the generation process removed.

### 3.0.0

* Matchers generated with the package naming strategy will and with the postfix "Matcher" again.

### 2.0.0

* Support for multiple inner classes with the same name added by using different naming strategies. For backwart
  compatibility use <namingStrategy>PLAIN</namingStrategy> which is the old strategy

### 1.3.0

* Support for Matcher-Generation for Interfaces
