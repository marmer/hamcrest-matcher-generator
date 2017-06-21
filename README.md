[![Build Status](https://travis-ci.org/marmer/java-test-helper-generator.svg?branch=master)](https://travis-ci.org/marmer/java-test-helper-generator)
[![codebeat badge](https://codebeat.co/badges/670d9178-beb7-438d-9823-53943c7fdf95)](https://codebeat.co/projects/github-com-marmer-java-test-helper-generator-master)
[![Coverage Status](https://coveralls.io/repos/github/marmer/java-test-helper-generator/badge.svg?branch=master)](https://coveralls.io/github/marmer/java-test-helper-generator?branch=master)
[![Dependency Status](https://www.versioneye.com/user/projects/59415a87368b0800700df4a2/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/59415a87368b0800700df4a2)

dynamic-compilesafe-hamcrest-property-matcher
=============================================
This library (hopefully) provides a collection of helper generators for tests.

Bean Property Matcher
---------------------

Using Hamcrest's Matcher HasPropertyWithValue, better known as Matchers.hasProperty, is awesome if you don't have created the class you want to test yet. The Problem with that kind of Matcher is, the more test you have matching the same property the harder it is to change the property name.

### Negative example

Lets assume you have a bean with a property named `myFancyProperty`. The following line would match if the value is equal to `Fancy value`.

`assertThat(someFancyBean, hasProperty("myFancyProperty", equalTo("Fancy value")));`

Now assume, you have tests all over your project with a line like this one testinh the `myFancyProperty` and you want to change its name to `myAwesomeProperty`. After renaming the property you may not find all the places in your tests to adjust. Now you only have to wait until the related tests fail to find the other places and fix the property name.

This strategy works pretty well, if you have only changed the property name and the test suite runs fast. Otherwise you'll get your feedback late or it get's really confusing and error prone.

### Solution

The idea of this helper is to get your feedback allredy at compile time. So your favorite IDE or build tool can show you all the places you have to change as well in a way like the following:

`assertThat(someFancyBean, is(fancyBean().withMyFancyProperty(equalTo("Fancy Value))));`

You only have to generate the matchers in advance with this tool, probably with a Plugin of your favorite IDE using this lib or with your favorite build tool.

### How-To

TODO