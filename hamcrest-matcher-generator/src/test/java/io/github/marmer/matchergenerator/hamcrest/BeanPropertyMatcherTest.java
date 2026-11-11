/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher.property;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import lombok.Value;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class BeanPropertyMatcherTest {

    @Test
    void testMatches_OnlyWithMatchingTypeInitialized_ShouldNotMatchInstanceOfDifferentType()
            throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<ClassWithSingleProperty>(ClassWithSingleProperty.class);

        // Execution
        final boolean matches =
                classUnderTest.matches(new AnotherClassWithSingleProperty("someValue"));

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
        final boolean matches =
                classUnderTest
                        .with("someProperty", is(equalTo("someValue")))
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
        final boolean matches =
                classUnderTest
                        .with("someProperty", is(equalTo("someValue")))
                        .matches(new ClassWithSingleProperty("someDifferentValue"));

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
        final boolean matches =
                classUnderTest
                        .with("differentProperty")
                        .matches(new ClassWithSingleProperty("someValue"));

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
        final boolean matches =
                classUnderTest
                        .with("someProperty")
                        .matches(new ClassWithSingleProperty("someValue"));

        // Assertion
        assertThat("matches", matches, is(true));
    }

    @Test
    void theRuntimeLibraryIsNullMarked() {
        assertThat(BeanPropertyMatcher.class.isAnnotationPresent(NullMarked.class), is(true));
    }

    @Test
    void aRecordComponentWithoutValueMatchesNull() {
        record Nameless(@Nullable String name) {}

        assertThat(
                new Nameless(null),
                new BeanPropertyMatcher<>(Nameless.class).with("name", nullValue()));
    }

    @Test
    void
            testMatches_InitializedWithDynamicPropertyWithInnerMatcherAndCallWithMotExistingPropertiesAtSecondPlace_ShouldMatch()
                    throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(ClassTwoProperties.class);

        // Execution
        final boolean matches =
                classUnderTest
                        .with("firstProperty", equalTo("firstPropertyValue"))
                        .with("notExistingProperty")
                        .matches(
                                new ClassTwoProperties(
                                        "firstPropertyValue", "secondPropertyValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Test
    void
            testMatches_InitializedWithDynamicPropertyWithoutInnerMatcherAndCallWithTwoExistingProperties_ShouldMatch()
                    throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(ClassTwoProperties.class);

        // Execution
        final boolean matches =
                classUnderTest
                        .with("secondProperty")
                        .with("firstProperty", equalTo("firstPropertyValue"))
                        .matches(
                                new ClassTwoProperties(
                                        "firstPropertyValue", "secondPropertyValue"));

        // Assertion
        assertThat("matches", matches, is(true));
    }

    @Test
    void
            testMatches_InitializedWithDynamicPropertyWithoutInnerMatcherAndCallWithNotExistingPropertiesAtSecondPlace_ShouldNotMatch()
                    throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(ClassTwoProperties.class);

        // Execution
        final boolean matches =
                classUnderTest
                        .with("secondProperty")
                        .with("notExistingProperty", equalTo("firstPropertyValue"))
                        .matches(
                                new ClassTwoProperties(
                                        "firstPropertyValue", "secondPropertyValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Test
    void
            testMatches_InitializedWithDynamicPropertyWithoutInnerMatcherAndCallWithExistingPropertiesAtSecondPlaceButNotMatching_ShouldNotMatch()
                    throws Exception {
        // Preparation
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(ClassTwoProperties.class);

        // Execution
        final boolean matches =
                classUnderTest
                        .with("secondProperty")
                        .with("firstProperty", equalTo("notMatchingValue"))
                        .matches(
                                new ClassTwoProperties(
                                        "firstPropertyValue", "secondPropertyValue"));

        // Assertion
        assertThat("matches", matches, is(false));
    }

    @Test
    void testDescribeTo_WithoutChecks_ShouldNameTheTypeAndCountNoChecks() {
        assertThat(
                describe(new BeanPropertyMatcher<>(ClassWithSingleProperty.class)),
                is("ClassWithSingleProperty with 0 property checks"));
    }

    @Test
    void testDescribeTo_WithChecks_ShouldCountTheChecks() {
        assertThat(
                describe(
                        new BeanPropertyMatcher<>(ClassTwoProperties.class)
                                .with("firstProperty", equalTo("a"))
                                .with("secondProperty")),
                is("ClassTwoProperties with 2 property checks"));
    }

    @Test
    void testDescribeMismatch_WrongTypeGiven_ShouldContainTypeInformationOfGivenWrongType() {
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<>(ClassWithSingleProperty.class);

        assertThat(
                mismatchOf(classUnderTest, new AnotherClassWithSingleProperty("x")),
                startsWith(
                        "expected an instance of "
                                + ClassWithSingleProperty.class.getName()
                                + " but was a "
                                + AnotherClassWithSingleProperty.class.getName()));
    }

    @Test
    void testDescribeMismatch_NotMatchingProperty_ShouldNameThePathExpectedAndActualValue() {
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<>(ClassWithSingleProperty.class)
                        .with("someProperty", equalTo("expected"));

        assertThat(
                mismatchOf(classUnderTest, new ClassWithSingleProperty("real")),
                is("someProperty: expected \"expected\" but was \"real\""));
    }

    @Test
    void testDescribeMismatch_NotExistingProperty_ShouldSayThatThePropertyDoesNotExist() {
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<>(ClassWithSingleProperty.class)
                        .with("notExisting", equalTo("anyValue"));

        assertThat(
                mismatchOf(classUnderTest, new ClassWithSingleProperty("real")),
                is(
                        "notExisting: expected \"anyValue\" but ClassWithSingleProperty has no property \"notExisting\""));
    }

    @Test
    void
            testDescribeMismatch_NotExistingPropertyWithoutMatcher_ShouldSayThatThePropertyDoesNotExist() {
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<>(ClassWithSingleProperty.class).with("notExisting");

        assertThat(
                mismatchOf(classUnderTest, new ClassWithSingleProperty("real")),
                is(
                        "notExisting: expected the property to exist but ClassWithSingleProperty has no property \"notExisting\""));
    }

    @Test
    void testDescribeMismatch_WrongTypeAndNotMatchingProperty_ShouldContainBothFailures() {
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<>(ClassWithSingleProperty.class)
                        .with("someProperty", equalTo("expected"));

        final String mismatch =
                mismatchOf(classUnderTest, new AnotherClassWithSingleProperty("real"));

        assertThat(
                mismatch,
                allOf(
                        containsString(
                                "expected an instance of "
                                        + ClassWithSingleProperty.class.getName()),
                        containsString("someProperty: expected \"expected\" but was \"real\"")));
    }

    @Test
    void
            testDescribeMismatch_OneMatchingAndSomeNotMatchingProperties_ShouldListOnlyTheFailuresInOrder() {
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<>(ClassTwoProperties.class)
                        .with("notExisting", equalTo("anyValue"))
                        .with("firstProperty", equalTo("matching"))
                        .with("secondProperty", equalTo("expected2"));

        assertThat(
                mismatchOf(classUnderTest, new ClassTwoProperties("matching", "real2")),
                is(
                        "notExisting: expected \"anyValue\" but ClassTwoProperties has no property \"notExisting\"\n"
                                + "          secondProperty: expected \"expected2\" but was \"real2\""));
    }

    @Test
    void testMatches_ResetProperty_ShouldNotCheckThePropertyAnymore() {
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<>(ClassWithSingleProperty.class)
                        .with("someProperty", equalTo("expected"));
        classUnderTest.reset("someProperty");

        assertThat(classUnderTest.matches(new ClassWithSingleProperty("other")), is(true));
    }

    @Test
    void testMatches_RecordComponentWithoutGetter_ShouldMatchTheComponentValue() {
        final BeanPropertyMatcher<Point> classUnderTest =
                new BeanPropertyMatcher<>(Point.class).with("x", equalTo(1)).with("y");

        assertThat(classUnderTest.matches(new Point(1, 2)), is(true));
        assertThat(classUnderTest.matches(new Point(5, 2)), is(false));
    }

    @Test
    void testDescribeMismatch_RecordComponentNotMatching_ShouldNameTheComponent() {
        final BeanPropertyMatcher<Point> classUnderTest =
                new BeanPropertyMatcher<>(Point.class).with("x", equalTo(1));

        assertThat(mismatchOf(classUnderTest, new Point(5, 2)), is("x: expected <1> but was <5>"));
    }

    @Test
    void testDescribeMismatch_RecordWithoutTheComponent_ShouldSayThatThePropertyDoesNotExist() {
        final BeanPropertyMatcher<Point> classUnderTest =
                new BeanPropertyMatcher<>(Point.class).with("z");

        assertThat(
                mismatchOf(classUnderTest, new Point(1, 2)),
                is("z: expected the property to exist but Point has no property \"z\""));
    }

    private static String describe(final org.hamcrest.SelfDescribing describable) {
        return StringDescription.toString(describable);
    }

    private static String mismatchOf(final Matcher<?> matcher, final Object item) {
        final Description description = new StringDescription();
        matcher.describeMismatch(item, description);
        return description.toString();
    }

    public record Point(int x, int y) {}

    @Test
    void testMatches_InitializedWithBooleanPropertyReadByIsGetter_ShouldMatchByItsValue() {
        final BeanPropertyMatcher<ClassWithBooleanIsGetter> classUnderTest =
                new BeanPropertyMatcher<>(ClassWithBooleanIsGetter.class);

        final boolean matches =
                classUnderTest.with("active", is(true)).matches(new ClassWithBooleanIsGetter(true));

        assertThat("matches", matches, is(true));
    }

    @Test
    void testMatches_InitializedWithRecordComponent_ShouldMatchByTheValueOfItsAccessor() {
        final BeanPropertyMatcher<ComponentRecord> classUnderTest =
                new BeanPropertyMatcher<>(ComponentRecord.class);

        final boolean matches =
                classUnderTest
                        .with("name", is("someValue"))
                        .matches(new ComponentRecord("someValue"));

        assertThat("matches", matches, is(true));
    }

    @Test
    void testMatches_InitializedWithRecordComponentAndDifferentValue_ShouldNotMatch() {
        final BeanPropertyMatcher<ComponentRecord> classUnderTest =
                new BeanPropertyMatcher<>(ComponentRecord.class);

        final boolean matches =
                classUnderTest
                        .with("name", is("someValue"))
                        .matches(new ComponentRecord("someDifferentValue"));

        assertThat("matches", matches, is(false));
    }

    @Test
    void testMatches_RecordComponentWhoseAccessorThrows_ShouldFailNamingTheProperty() {
        final BeanPropertyMatcher<ThrowingRecord> classUnderTest =
                new BeanPropertyMatcher<>(ThrowingRecord.class);
        final Matcher<ThrowingRecord> matcher = classUnderTest.with("broken", is("someValue"));

        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () -> matcher.matches(new ThrowingRecord("someValue")));

        assertThat(
                "message", error.getMessage(), containsString("Could not read property 'broken'"));
    }

    @Test
    void testMatches_AfterResetOfTheProperty_ShouldNoLongerCheckIt() {
        final BeanPropertyMatcher<ClassWithSingleProperty> classUnderTest =
                new BeanPropertyMatcher<>(ClassWithSingleProperty.class);
        classUnderTest.with("someProperty", is(equalTo("someValue")));

        classUnderTest.reset("someProperty");

        final boolean matches =
                classUnderTest.matches(new ClassWithSingleProperty("someDifferentValue"));
        assertThat("matches", matches, is(true));
    }

    @Test
    void testMatches_PropertyWithAccessor_ShouldReadTheValueThroughTheAccessor() {
        final BeanPropertyMatcher<TextOnly> classUnderTest =
                new BeanPropertyMatcher<TextOnly>(
                        TextOnly.class, property("label", TextOnly::text));
        classUnderTest.with("label", is(equalTo("someValue")));

        final boolean matches = classUnderTest.matches(new TextOnly("someValue"));

        assertThat("matches", matches, is(true));
    }

    @Test
    void testMatches_PropertyWithAccessorAndOtherValue_ShouldNotMatch() {
        final BeanPropertyMatcher<TextOnly> classUnderTest =
                new BeanPropertyMatcher<TextOnly>(
                        TextOnly.class, property("label", TextOnly::text));
        classUnderTest.with("label", is(equalTo("someValue")));

        final boolean matches = classUnderTest.matches(new TextOnly("someDifferentValue"));

        assertThat("matches", matches, is(false));
    }

    @Test
    void testMatches_AccessorThatThrows_ShouldFailNamingTheProperty() {
        final BeanPropertyMatcher<ThrowingRecord> classUnderTest =
                new BeanPropertyMatcher<ThrowingRecord>(
                        ThrowingRecord.class, property("broken", ThrowingRecord::broken));
        final Matcher<ThrowingRecord> matcher = classUnderTest.with("broken", is("someValue"));

        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () -> matcher.matches(new ThrowingRecord("someValue")));

        assertThat(
                "message", error.getMessage(), containsString("Could not read property 'broken'"));
        assertThat("cause", error.getCause(), instanceOf(IllegalStateException.class));
    }

    @Test
    void testMatches_AccessorThatThrowsACheckedException_ShouldFailNamingTheProperty() {
        final BeanPropertyMatcher<CheckedGetter> classUnderTest =
                new BeanPropertyMatcher<CheckedGetter>(
                        CheckedGetter.class, property("checked", CheckedGetter::getChecked));
        final Matcher<CheckedGetter> matcher = classUnderTest.with("checked", is("someValue"));

        final AssertionError error =
                assertThrows(AssertionError.class, () -> matcher.matches(new CheckedGetter()));

        assertThat(
                "message", error.getMessage(), containsString("Could not read property 'checked'"));
        assertThat("cause", error.getCause(), instanceOf(IOException.class));
    }

    @Test
    void testDescribeMismatch_WithAccessors_ShouldReportTheSameAsWithoutAccessors() {
        final BeanPropertyMatcher<ClassTwoProperties> reflective =
                new BeanPropertyMatcher<ClassTwoProperties>(
                        ClassTwoProperties.class, "firstProperty", "secondProperty");
        reflective.with("firstProperty", is(equalTo("a"))).with("secondProperty", is(equalTo("b")));
        final BeanPropertyMatcher<ClassTwoProperties> accessors =
                new BeanPropertyMatcher<ClassTwoProperties>(
                        ClassTwoProperties.class,
                        property("firstProperty", ClassTwoProperties::getFirstProperty),
                        property("secondProperty", ClassTwoProperties::getSecondProperty));
        accessors.with("firstProperty", is(equalTo("a"))).with("secondProperty", is(equalTo("b")));
        final ClassTwoProperties item = new ClassTwoProperties("x", "y");

        assertThat("mismatch", mismatchOf(accessors, item), is(mismatchOf(reflective, item)));
    }

    @Test
    void testDescribeMismatch_WithAccessorsAndCompleteness_ShouldReportTheSameAsWithoutAccessors() {
        final BeanPropertyMatcher<ClassTwoProperties> reflective =
                new BeanPropertyMatcher<ClassTwoProperties>(
                                ClassTwoProperties.class, "firstProperty", "secondProperty")
                        .checkingAllProperties();
        reflective.with("firstProperty", is(equalTo("a")));
        final BeanPropertyMatcher<ClassTwoProperties> accessors =
                new BeanPropertyMatcher<ClassTwoProperties>(
                                ClassTwoProperties.class,
                                property("firstProperty", ClassTwoProperties::getFirstProperty),
                                property("secondProperty", ClassTwoProperties::getSecondProperty))
                        .checkingAllProperties();
        accessors.with("firstProperty", is(equalTo("a")));
        final ClassTwoProperties item = new ClassTwoProperties("x", "y");

        assertThat("mismatch", mismatchOf(accessors, item), is(mismatchOf(reflective, item)));
    }

    @Test
    void testDescribeMismatch_ItemOfAnotherType_ShouldReportTheSameAsWithoutAccessors() {
        final BeanPropertyMatcher<ClassWithSingleProperty> reflective =
                new BeanPropertyMatcher<ClassWithSingleProperty>(
                        ClassWithSingleProperty.class, "someProperty");
        reflective.with("someProperty", is(equalTo("a")));
        final BeanPropertyMatcher<ClassWithSingleProperty> accessors =
                new BeanPropertyMatcher<ClassWithSingleProperty>(
                        ClassWithSingleProperty.class,
                        property("someProperty", ClassWithSingleProperty::getSomeProperty));
        accessors.with("someProperty", is(equalTo("a")));
        final AnotherClassWithSingleProperty item = new AnotherClassWithSingleProperty("x");

        assertThat("mismatch", mismatchOf(accessors, item), is(mismatchOf(reflective, item)));
    }

    @Test
    void testMatches_PropertyWithoutAccessor_ShouldStillBeReadByName() {
        final BeanPropertyMatcher<ClassTwoProperties> classUnderTest =
                new BeanPropertyMatcher<ClassTwoProperties>(
                        ClassTwoProperties.class,
                        property("firstProperty", ClassTwoProperties::getFirstProperty));
        classUnderTest.with("secondProperty", is(equalTo("y")));

        final boolean matches = classUnderTest.matches(new ClassTwoProperties("x", "y"));

        assertThat("matches", matches, is(true));
    }

    @Test
    void testConstruction_WithAccessors_ShouldRegisterTheNamesForPrinting() {
        new BeanPropertyMatcher<RegisteredWithAccessors>(
                RegisteredWithAccessors.class,
                property("name", RegisteredWithAccessors::getName),
                property("class", RegisteredWithAccessors::getClass));

        assertThat(
                "printed properties",
                PropertyAccess.propertiesOfGeneratedType(RegisteredWithAccessors.class),
                is(List.of("name")));
    }

    public static class CheckedGetter {
        public String getChecked() throws IOException {
            throw new IOException("checked on purpose");
        }
    }

    public static class TextOnly {
        private final String text;

        public TextOnly(final String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    public static class RegisteredWithAccessors {
        public String getName() {
            return "name";
        }
    }

    @Value
    public static class ClassWithBooleanIsGetter {
        private boolean active;
    }

    public record ComponentRecord(String name) {}

    public record ThrowingRecord(String broken) {
        @Override
        public String broken() {
            throw new IllegalStateException("broken on purpose");
        }
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
