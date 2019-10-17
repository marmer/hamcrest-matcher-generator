[![Build Status](https://travis-ci.org/marmer/hamcrest-matcher-generator.svg?branch=master)](https://travis-ci.org/marmer/hamcrest-matcher-generator)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.marmer.testutils/hamcrest-matcher-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.marmer.testutils/hamcrest-matcher-generator)
 
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.github.marmer.testutils:hamcrest-matcher-generator)
[![Code Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=coverage)](https://sonarcloud.io/component_measures?id=io.github.marmer.testutils:hamcrest-matcher-generator&metric=Coverage)
[![Technical Dept](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=sqale_index)](https://sonarcloud.io/project/issues?facetMode=effort&id=io.github.marmer.testutils:hamcrest-matcher-generator&resolved=false&types=CODE_SMELL)

[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=security_rating)](https://sonarcloud.io/component_measures?id=io.github.marmer.testutils:hamcrest-matcher-generator&metric=Security)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=sqale_rating)](https://sonarcloud.io/component_measures?id=io.github.marmer.testutils:hamcrest-matcher-generator&metric=Maintainability)
[![Reliability](https://sonarcloud.io/api/project_badges/measure?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=io.github.marmer.testutils:hamcrest-matcher-generator&metric=Reliability)

hamcrest-matcher-generator
==========================
This library provides the generation of hamcrest matchers without the need to pollute the production code. 

Bean Property Matcher
---------------------

Ever wanted to...
* ...have some Hamcrest-Matcchers for all (or some) of your Model classes oder Services magically appear?
  * or ever wanted it without to pollute your production code with test code annotations?
  * or types which are not part of the current source code.
* ...test your models with hamcrest in an atomic way with atomic error messages?
* ...have a compile safe alternative to Hamcrests "hasProperty" or HasPropertyWithValue?

Properties of Lombok annotated classes are supported as well (tested with version 1.18.4)

How to use
==========
All you need is to add one or two dependencies and an Annotation for the configuration for what types the matchers have to be generated.

Dependencies
----------

If you want to use the matchers in your testcode only, simply add this dependency to your project.

    <dependency>
        <groupId>io.github.marmer.testutils</groupId>
        <artifactId>hamcrest-matcher-generator-annotationprocessor</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>
    
If you want to use it in your production code, you should use it only "provided" to avoid unnecessary dependencies. In this case, you have to add another dependency used by the generated code.

    <dependency>
        <groupId>io.github.marmer.testutils</groupId>
        <artifactId>hamcrest-matcher-generator-annotationprocessor</artifactId>
        <version>4.2.0</version>
        <scope>provided</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>io.github.marmer.testutils</groupId>
        <artifactId>hamcrest-matcher-generator-dependencies</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>

Configuration
-------------
Simply create a Class or Interface with one or more `@MatcherConfiguration` and add either full qualified Class names or packages for Types you want to generate matchers for e.g.

    @MatcherConfiguration({
            "foo.bar.sample.model.SomePojo",
            "foo.bar.sample.model.ParentPojo",
            "foo.bar.sample.model.SomePojoInterface",
            "foo.bar.sample.model.SomeLombokPojo",
    })
    public class PackageConfiguration {
    }
    
Depending on where you put the configuration the generated matchers will be generated within generated-test-sources you put it in your test sources or in generated-sources if you put it in your production code sources. (At least in maven projects this is the default behavior. It may be different with other build tools or non default configuration, but it should work for other build tools in a similar way)

Generated result
----------------
Assuming you hava a pojo like this one with the configuration above...

    package foo.bar.sample.model;

    public class SomePojo extends ParentPojo {
        private String pojoField;

        public String getPojoField() {
            return pojoField;
        }
    }
    
... a Matcher Named SomePojoMatcher is generated within the same package and you could use it the following way in your test:

    final SomePojo somePojo = new SomePojo();
    somePojo.setPojoField("pojoFieldValue");
    somePojo.setParentField("someParentFieldValue");


    // Assertion
    assertThat(somePojo, isSomePojo()
            .withClass(SomePojo.class)
            .withParentField("someParentFieldValue")
            .withParentField(is(equalTo("someParentFieldValue")))
            .withPojoField("pojoFieldValue")
            .withPojoField(is(equalTo("pojoFieldValue")))
    );

This example shows a way to match the class, the values (equality) for the direct field as well as for parent fields and for matchers for each field.

Requirements
============

Build tool
---------
You can use this library with the buildtool of your choice or even just javac. This library is capable of annotation processing. So the matchers are generated at compile time like it's done with other Libraries (e.g. Lombok or Mapstruct).  

IDE
---
Use the IDE of your choice. Each IDE with annotation processing capabilities should be able to perform the Generation by itself when the project builds. Some IDEs may need a little help. Eclipse for example may be only capable of annotation processing in maven projects if you have installed a maven plugin [m2e-apt](https://marketplace.eclipse.org/content/m2e-apt) but you don't have the IDO to perform the processing. With a little help of the `build-helper-maven-plugin` you can tell the IDE where to look for sources generated by Maven. 

```
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

JDK
---

At least JDK6 is required to use the generated source code but the annotation processor requires JDK8 to create it.

Generated sources may also work with JDK5. But it is and will not be tested so there is no guarantee!

Hamcrest
--------
Because hamcrest matchers are generated, you will need a dependency to hamcrest to be able to use the generated sources of course. In General you shuld be free to choose your version of hamcrest by yourself.

Your Project should be at least of Java version 1.6 and use a hamcrest version of 1.2. The resulting code will not work without hamcrest.

For JDK7+ projects you should (but don't have to) use the following hamcrest version for the generated sources

	<dependency>
		<groupId>org.hamcrest</groupId>
		<artifactId>java-hamcrest</artifactId>
		<version>2.0.0.0</version>
		<scope>test</scope>
	</dependency>	

For JDK6 you may use:

	<dependency>
		<groupId>org.hamcrest</groupId>
		<artifactId>hamcrest-all</artifactId>
		<version>1.3</version>
		<scope>test</scope>
	</dependency>`


### Changelog
This project uses semantic versioning. See https://semver.org/

### Outstanding
* Excludes
* Custom Matcher Postfix (instead of Matcher)
* Handling of naming conflicts (if a Class which ends with the configured Postfix exists already and was not generated)
* possiblility to set a package prefix because jigsaw does not allow the generation of types within packages of other modules

### 4.2.2
* ability to create/use/handle javax.annotations.Generated annotations with newer JDKs added

### 4.2.1
* Handling of arrays of primitives fixed

### 4.2.0
* Logging happens with a prefix so that the user is able to distinguish between errors of this annotation processor and others
* Annotation based logs without more concrete elements contain line and column information of the annotated element  

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
* Fixed: matchers are not generated for non public types
* Fixed: matcher methods are not generated for non public properties

### 3.2.0
* Naming Strategy for classnames instead of package names implemented (PARENT)

### 3.1.2
* Bug fixed with the generation of some kind of subclasses

### 3.1.1
* A little more logging

### 3.1.0
* Added errorlogging for (some) code errors

### 3.0.1
* Bigfix: useless class compilation at the end of the generation process removed.

### 3.0.0 
* Matchers generated with the package naming strategy will and with the postfix "Matcher" again.

### 2.0.0
* Support for multiple inner classes with the same name added by using different naming strategies. For backwart compatibility use <namingStrategy>PLAIN</namingStrategy> which is the old strategy

### 1.3.0
* Support for Matcher-Generation for Interfaces
