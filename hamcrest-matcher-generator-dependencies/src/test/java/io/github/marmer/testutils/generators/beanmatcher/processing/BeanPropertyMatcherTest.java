package io.github.marmer.testutils.generators.beanmatcher.processing;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import lombok.Value;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


class BeanPropertyMatcherTest {
    private static final String AND = " and ";

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
    void testDescribeTo_OnlyWithMatchingTypeInitialized_DescriptionTextShouldContainInstanceOfDescriptionText()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);
        final Description description = new StringDescription();

        // Execution
        classUnderTest.describeTo(description);

        // Assertion
        assertThat("Matcher description Text",
                description.toString(),
                containsString(instanceOfDescriptionText(ClassWithSingleProperty.class)));
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
    void testDescribeTo_InitializedWithDynamicPropertyAndCallWithExistingPropertyAndInnerMatcher_ShouldContainIsInstanceDescription()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        final Description description = new StringDescription();

        // Execution
        classUnderTest.with("someProperty", equalTo("someValue")).describeTo(description);

        // Assertion
        assertThat("Matcher description Text",
                description.toString(),
                containsString(instanceOfDescriptionText(ClassWithSingleProperty.class)));
    }

    @Test
    void testDescribeTo_InitializedWithDynamicPropertyAndCallWithExistingPropertyAndInnerMatcher_ShouldContainHasPropertyDescription()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        final Description description = new StringDescription();

        // Execution
        classUnderTest.with("someProperty", equalTo("someValue")).describeTo(description);

        // Assertion
        assertThat("Matcher description Text",
                description.toString(),
                containsString(hasPropertyDescriptionText("someProperty", equalTo("someValue"))));
    }

    @Test
    void testDescribeTo_InitializedWithDynamicPropertyAndCallWithExistingProperty_ShouldContainIsInstanceDescription()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        final Description description = new StringDescription();

        // Execution
        classUnderTest.with("someProperty").describeTo(description);

        // Assertion
        assertThat("Matcher description Text",
                description.toString(),
                containsString(instanceOfDescriptionText(ClassWithSingleProperty.class)));
    }

    @Test
    void testDescribeTo_InitializedWithDynamicPropertyAndCallWithExistingProperty_ShouldContainHasPropertyDescription()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        final Description description = new StringDescription();

        // Execution
        classUnderTest.with("someProperty").describeTo(description);

        // Assertion
        assertThat("Matcher description Text",
                description.toString(),
                containsString(hasPropertyDescriptionText("someProperty")));
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

    @Test
    void testDescribeMissmatch_WrongTypeGiven_DescriptionShuoldContainTypeInformationOfGivenWrongType()
            throws Exception {
        // Preparation
        final Matcher<?> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        final Description description = new StringDescription();
        final AnotherClassWithSingleProperty modelClass =
                new AnotherClassWithSingleProperty("someProperty");

        // Execution
        classUnderTest.describeMismatch(modelClass, description);

        // Expectation

        assertThat(description.toString(),
                startsWith(instanceOfMissmatchTextFor(AnotherClassWithSingleProperty.class)));
    }

    private String instanceOfMissmatchTextFor(final Class<?> type) {
        return "Is an instance of " + type;
    }

    @Test
    void testDescribeMissmatch_CorrectInstanceWithNotMatchingPropertyGiven_ShouldStartWithNotMatchingPropertyText()
            throws Exception {
        // Preparation
        final String propertyName = "someProperty";
        final Matcher<String> propertyMatcher = equalTo("expectedPropertyValue");
        final Matcher<?> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class).with(
                        propertyName,
                        propertyMatcher);

        final Description description = new StringDescription();
        final ClassWithSingleProperty modelClass = new ClassWithSingleProperty("realPropertyValue");

        // Execution
        classUnderTest.describeMismatch(modelClass, description);

        // Expectation
        final String propertyMissmatchDescriptionText = getHasPropertyMissmatchDescriptionFor(
                modelClass,
                propertyName,
                propertyMatcher);

        assertThat(description.toString(), startsWith(propertyMissmatchDescriptionText));
    }

    @Test
    void testDescribeMissmatch_CorrectInstanceWithNotExistingPropertyGiven_ShouldStartWithNotMatchingPropertyText()
            throws Exception {
        // Preparation
        final String propertyName = "someNotExistingProperty";
        final Matcher<String> propertyMatcher = equalTo("expectedPropertyValue");
        final Matcher<?> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class).with(
                        propertyName,
                        propertyMatcher);

        final Description description = new StringDescription();
        final ClassWithSingleProperty modelClass = new ClassWithSingleProperty("realPropertyValue");

        // Execution
        classUnderTest.describeMismatch(modelClass, description);

        // Expectation
        final String propertyMissmatchDescriptionText = getHasPropertyMissmatchDescriptionFor(
                modelClass,
                propertyName,
                propertyMatcher);

        assertThat(description.toString(), startsWith(propertyMissmatchDescriptionText));
    }

    @Test
    void testDescribeMissmatch_IncorrectInstanceWithNotMatchingPropertyGiven_DescriptionShouldContainMissmatchInformationAboutWrongInstanceAndNotMatchingProperty()
            throws Exception {
        // Preparation
        final String propertyName = "someNotExistingProperty";
        final Matcher<String> propertyMatcher = equalTo("expectedPropertyValue");
        final Matcher<?> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class).with(
                        propertyName,
                        propertyMatcher);

        final Description description = new StringDescription();
        final AnotherClassWithSingleProperty modelClass =
                new AnotherClassWithSingleProperty("realPropertyValue");

        // Execution
        classUnderTest.describeMismatch(modelClass, description);

        // Expectation
        final String propertyMissmatchDescriptionText = getHasPropertyMissmatchDescriptionFor(
                modelClass,
                propertyName,
                propertyMatcher);

        assertThat(description.toString(),
                is(equalTo(
                        instanceOfMissmatchTextFor(modelClass.getClass()) + AND +
                                propertyMissmatchDescriptionText)));
    }

    @Test
    void testDescribeMissmatch_ObjectWithOneMatchingAndSomeNotMatchingPropertyGiven_NotMatchingMissmatchShouldNotBePartOfResultingDescription()
            throws Exception {
        final String propertyNameOfNotExistingProperty = "notExistingProperty";
        final Matcher<String> propertyMatcherOfNotExistingProperty = equalTo("anyValue");

        final String propertyNameOfMatching = "firstProperty";
        final Matcher<String> propertyMatcherOfMatching = equalTo("expectedPropertyValue1");

        final String propertyNameOfNonMatching = "secondProperty";
        final Matcher<String> propertyMatcherOfNonMatching = equalTo("expectedPropertyValue2");

        final Matcher<?> classUnderTest = new BeanPropertyMatcher<ClassTwoProperties>(
                ClassTwoProperties.class).with(propertyNameOfNotExistingProperty,
                propertyMatcherOfNotExistingProperty)
                .with(propertyNameOfMatching, propertyMatcherOfMatching)
                .with(propertyNameOfNonMatching,
                        propertyMatcherOfNonMatching);

        final Description description = new StringDescription();
        final ClassTwoProperties modelClass =
                new ClassTwoProperties("expectedPropertyValue1", "unexpectedPropertyValue2");

        // Execution
        classUnderTest.describeMismatch(modelClass, description);

        // Expectation
        final String propertyMissmatchDescriptionTextForMatching =
                getHasPropertyMissmatchDescriptionFor(modelClass,
                        propertyNameOfMatching,
                        propertyMatcherOfMatching);

        assertThat(description.toString(),
                is(not(containsString(propertyMissmatchDescriptionTextForMatching))));
    }

    @Test
    void testDescribeMissmatch_ObjectWithOneMatchingAndSomeNotMatchingPropertyGiven_NotMatchingResultsShouldBeShownInOrder()
            throws Exception {
        final String propertyNameOfNotExistingProperty = "notExistingProperty";
        final Matcher<String> propertyMatcherOfNotExistingProperty = equalTo("anyValue");

        final String propertyNameOfMatching = "firstProperty";
        final Matcher<String> propertyMatcherOfMatching = equalTo("expectedPropertyValue1");

        final String propertyNameOfNonMatching = "secondProperty";
        final Matcher<String> propertyMatcherOfNonMatching = equalTo("expectedPropertyValue2");

        final Matcher<?> classUnderTest = new BeanPropertyMatcher<ClassTwoProperties>(
                ClassTwoProperties.class).with(propertyNameOfNotExistingProperty,
                propertyMatcherOfNotExistingProperty)
                .with(propertyNameOfMatching, propertyMatcherOfMatching)
                .with(propertyNameOfNonMatching,
                        propertyMatcherOfNonMatching);

        final Description description = new StringDescription();
        final ClassTwoProperties modelClass =
                new ClassTwoProperties("expectedPropertyValue1", "unexpectedPropertyValue2");

        // Execution
        classUnderTest.describeMismatch(modelClass, description);

        // Expectation
        final String propertyMissmatchDescriptionTextForNonExisting =
                getHasPropertyMissmatchDescriptionFor(modelClass,
                        propertyNameOfNotExistingProperty,
                        propertyMatcherOfNotExistingProperty);

        final String propertyMissmatchDescriptionTextForNonMatching =
                getHasPropertyMissmatchDescriptionFor(modelClass,
                        propertyNameOfNonMatching,
                        propertyMatcherOfNonMatching);

        assertThat(description.toString(),
                is(equalTo(
                        propertyMissmatchDescriptionTextForNonExisting + AND +
                                propertyMissmatchDescriptionTextForNonMatching)));
    }

    @Test
    void testDescribeMissmatch_LotsOfNotMatchingObjetsGiven_MissmatchDescriptionShouldBeInOrder()
            throws Exception {
        final String propertyNameOfNotExistingProperty = "notExistingProperty";
        final Matcher<String> propertyMatcherOfNotExistingProperty = equalTo("anyValue");

        final String propertyNameOfNonMatching1 = "firstProperty";
        final Matcher<String> propertyMatcherOfNonMatching1 = equalTo("expectedPropertyValue1");

        final String propertyNameOfNonMatching2 = "secondProperty";
        final Matcher<String> propertyMatcherOfNonMatching2 = equalTo("expectedPropertyValue2");

        final Matcher<?> classUnderTest = new BeanPropertyMatcher<ClassTwoProperties>(
                ClassTwoProperties.class).with(propertyNameOfNotExistingProperty,
                propertyMatcherOfNotExistingProperty)
                .with(propertyNameOfNonMatching1,
                        propertyMatcherOfNonMatching1).with(
                        propertyNameOfNonMatching2,
                        propertyMatcherOfNonMatching2);

        final Description description = new StringDescription();
        final ClassTwoProperties modelClass =
                new ClassTwoProperties("unexpectedPropertyValue1", "unexpectedPropertyValue2");

        // Execution
        classUnderTest.describeMismatch(modelClass, description);

        // Expectation
        final String propertyMissmatchDescriptionTextForNonExisting =
                getHasPropertyMissmatchDescriptionFor(modelClass,
                        propertyNameOfNotExistingProperty,
                        propertyMatcherOfNotExistingProperty);

        final String propertyMissmatchDescriptionTextForNonMatching1 =
                getHasPropertyMissmatchDescriptionFor(modelClass,
                        propertyNameOfNonMatching1,
                        propertyMatcherOfNonMatching1);
        final String propertyMissmatchDescriptionTextForNonMatching2 =
                getHasPropertyMissmatchDescriptionFor(modelClass,
                        propertyNameOfNonMatching2,
                        propertyMatcherOfNonMatching2);

        assertThat(description.toString(),
                is(equalTo(
                        propertyMissmatchDescriptionTextForNonExisting + AND +
                                propertyMissmatchDescriptionTextForNonMatching1 + AND +
                                propertyMissmatchDescriptionTextForNonMatching2)));
    }

    private String getHasPropertyMissmatchDescriptionFor(final Object modelClass,
                                                         final String propertyName,
                                                         final Matcher<String> propertyMatcher) {
        final Description propertyMissmatchDescription = new StringDescription();
        hasProperty(propertyName, equalTo(propertyMatcher)).describeMismatch(modelClass,
                propertyMissmatchDescription);

        return propertyMissmatchDescription.toString();
    }

    private String hasPropertyDescriptionText(final String propertyName) {
        final Description instanceOfDescription = new StringDescription();
        Matchers.hasProperty(propertyName).describeTo(instanceOfDescription);

        return instanceOfDescription.toString();
    }

    private String hasPropertyDescriptionText(final String propertyName,
                                              final Matcher<String> innerMatcher) {
        final Description instanceOfDescription = new StringDescription();
        Matchers.hasProperty(propertyName, innerMatcher).describeTo(instanceOfDescription);

        return instanceOfDescription.toString();
    }

    private String instanceOfDescriptionText(final Class<ClassWithSingleProperty> type) {
        final Description instanceOfDescription = new StringDescription();
        Matchers.instanceOf(type).describeTo(instanceOfDescription);

        return instanceOfDescription.toString();
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
