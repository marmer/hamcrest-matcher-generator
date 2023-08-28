![CI-Build](https://github.com/marmer/hamcrest-matcher-generator/workflows/CI-Build/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.marmer.testutils/hamcrest-matcher-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.marmer.testutils/hamcrest-matcher-generator)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.github.marmer.testutils:hamcrest-matcher-generator)
[![Code Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=coverage)](https://sonarcloud.io/component_measures?id=io.github.marmer.testutils:hamcrest-matcher-generator&metric=Coverage)
[![Technical Dept](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=sqale_index)](https://sonarcloud.io/project/issues?facetMode=effort&id=io.github.marmer.testutils:hamcrest-matcher-generator&resolved=false&types=CODE_SMELL)

[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=security_rating)](https://sonarcloud.io/component_measures?id=io.github.marmer.testutils:hamcrest-matcher-generator&metric=Security)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=sqale_rating)](https://sonarcloud.io/component_measures?id=io.github.marmer.testutils:hamcrest-matcher-generator&metric=Maintainability)
[![Reliability](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=io.github.marmer.testutils:hamcrest-matcher-generator&metric=Reliability)

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
All you need to do is to add one or two dependencies and an Annotation for the configuration for those types the matchers have
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

If you wish to use it in your production code as well, you should declare the scope as "provided" to avoid unnecessary dependencies. In
this case, you will have to add another dependency that will be used by the generated code.

```xml

<dependency>
    <groupId>io.github.marmer.testutils</groupId>
    <artifactId>hamcrest-matcher-generator-annotationprocessor</artifactId>
    <version>${hamcrest-matcher-generator.version}</version>
    <scope>provided</scope>
    <optional>true</optional>
</dependency>
<dependency>
<groupId>io.github.marmer.testutils</groupId>
<artifactId>hamcrest-matcher-generator-dependencies</artifactId>
<version>${hamcrest-matcher-generator.version}</version>
<scope>test</scope>
</dependency>
```

Configuration
-------------
Simply create a Class or Interface with one or more `@MatcherConfiguration` and add either fully-qualified class names or
packages for the types you wish to generate matchers for e.g.

```java

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

You could simply move the generated test sources into a well-known location right after the moment of generation.

#### Example for Maven:

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        <execution>
            <phase>process-test-sources</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <target>
                    <move todir="${project.basedir}/target/generated-test-sources/test-annotations"
                        force="true">
                        <fileset dir="${project.basedir}/target/generated-sources/kapt/test/"/>
                    </move>
                </target>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The drawback is, now you *have to* compile your project with your build tool for a fresh generation.

JDK
---

JDK11 or higher is required to use the generated source code.

Generated sources may also work with earlier JDK Versinos. This configuration will not be tested, however, so there is no guarantee!

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

This project uses semantic versioning. See https://semver.org/

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
