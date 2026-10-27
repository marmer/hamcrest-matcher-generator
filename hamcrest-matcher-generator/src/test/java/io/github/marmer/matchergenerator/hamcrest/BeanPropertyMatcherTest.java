package io.github.marmer.matchergenerator.hamcrest;

import lombok.Value;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


class BeanPropertyMatcherTest {

    @Test
    void testMatches_OnlyWithMatchingTypeInitialized_ShouldNotMatchInstanceOfDifferentType()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        // Execution
        final boolean matches = classUnderTest.matches(
                new AnotherClassWithSingleProperty("someValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Test
    void testMatches_OnlyWithMatchingTypeInitialized_ShouldMatchInstanceOfSameType()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        // Execution
        final boolean matches = classUnderTest.matches(new ClassWithSingleProperty("someValue"));

        // Assertion
        assertThat("matches", matches, is(true));
    }

    @Test
    void testMatches_InitializedWithDynamicPropertyAndCalledWithMatchingProperty_ShouldMatch()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        // Execution
        final boolean matches = classUnderTest.with("someProperty", is(equalTo("someValue")))
                .matches(new ClassWithSingleProperty("someValue"));

        // Assertion
        assertThat("matches", matches, is(true));
    }

    @Test
    void testMatches_InitializedWithDynamicPropertyAndCalledWithNotMatchingProperty_ShouldNotMatch()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        // Execution
        final boolean matches = classUnderTest.with("someProperty", is(equalTo("someValue")))
                .matches(
                        new ClassWithSingleProperty(
                                "someDifferentValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Test
    void testMatches_InitializedWithDynamicPropertyAndCallWithNotExistingProperty_ShouldNotMatch()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        // Execution
        final boolean matches = classUnderTest.with("differentProperty").matches(
                new ClassWithSingleProperty("someValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Test
    void testMatches_InitializedWithDynamicPropertyAndCallWithExistingProperty_ShouldMatch()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        // Execution
        final boolean matches = classUnderTest.with("someProperty").matches(
                new ClassWithSingleProperty("someValue"));

        // Assertion
        assertThat("matches", matches, is(true));
    }

    @Test
    void testMatches_InitializedWithDynamicPropertyWithInnerMatcherAndCallWithMotExistingPropertiesAtSecondPlace_ShouldMatch()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(ClassTwoProperties.class);

        // Execution
        final boolean matches = classUnderTest.with("firstProperty", equalTo("firstPropertyValue"))
                .with("notExistingProperty").matches(
                        new ClassTwoProperties("firstPropertyValue",
                                "secondPropertyValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Test
    void testMatches_InitializedWithDynamicPropertyWithoutInnerMatcherAndCallWithTwoExistingProperties_ShouldMatch()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(ClassTwoProperties.class);

        // Execution
        final boolean matches = classUnderTest.with("secondProperty")
                .with("firstProperty", equalTo("firstPropertyValue"))
                .matches(
                        new ClassTwoProperties("firstPropertyValue",
                                "secondPropertyValue"));

        // Assertion
        assertThat("matches", matches, is(true));
    }

    @Test
    void testMatches_InitializedWithDynamicPropertyWithoutInnerMatcherAndCallWithNotExistingPropertiesAtSecondPlace_ShouldNotMatch()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(ClassTwoProperties.class);

        // Execution
        final boolean matches = classUnderTest.with("secondProperty")
                .with("notExistingProperty",
                        equalTo("firstPropertyValue")).matches(
                        new ClassTwoProperties("firstPropertyValue",
                                "secondPropertyValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Test
    void testMatches_InitializedWithDynamicPropertyWithoutInnerMatcherAndCallWithExistingPropertiesAtSecondPlaceButNotMatching_ShouldNotMatch()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(ClassTwoProperties.class);

        // Execution
        final boolean matches = classUnderTest.with("secondProperty")
                .with("firstProperty", equalTo("notMatchingValue"))
                .matches(
                        new ClassTwoProperties("firstPropertyValue",
                                "secondPropertyValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Value
    public static class ClassWithSingleProperty {
        private String someProperty;
    }

    @Value
    public static class AnotherClassWithSingleProperty {
        private String someProperty;
    }

    @Value
    public static class ClassTwoProperties {
        private String firstProperty;
        private String secondProperty;
    }
}
