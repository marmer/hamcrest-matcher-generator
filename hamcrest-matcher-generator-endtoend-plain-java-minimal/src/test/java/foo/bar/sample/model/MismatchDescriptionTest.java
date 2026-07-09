package foo.bar.sample.model;

import org.hamcrest.StringDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static foo.bar.sample.model.SomePojoMatcher.isSomePojo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class MismatchDescriptionTest {

    @Test
    @DisplayName("Mismatch lists only the failing properties, one per line, with expected vs. actual")
    void describeMismatch_listsOnlyFailingPropertiesOnePerLine() {
        // Preparation
        final SomePojo somePojo = new SomePojo();
        somePojo.setPojoField("actualPojoValue");
        somePojo.setParentField("okParentValue");
        somePojo.setIntProperty(7);

        final SomePojoMatcher matcher = isSomePojo()
                .withPojoField("expectedPojoValue")
                .withParentField("okParentValue")
                .withIntProperty(42);

        // Execution
        final StringDescription mismatchDescription = new StringDescription();
        matcher.describeMismatch(somePojo, mismatchDescription);

        // Assertion
        assertThat(mismatchDescription.toString(), is(equalTo(
                "pojoField: expected \"expectedPojoValue\" but was \"actualPojoValue\"\n"
                        + "intProperty: expected <42> but was <7>")));
    }

    @Test
    @DisplayName("Mismatch of matcher based expectations shows the matcher description as expectation")
    void describeMismatch_worksForMatcherBasedExpectations() {
        // Preparation
        final SomePojo somePojo = new SomePojo();
        somePojo.setPojoField("actualPojoValue");

        final SomePojoMatcher matcher = isSomePojo()
                .withPojoField(is(equalTo("expectedPojoValue")));

        // Execution
        final StringDescription mismatchDescription = new StringDescription();
        matcher.describeMismatch(somePojo, mismatchDescription);

        // Assertion
        assertThat(mismatchDescription.toString(), is(equalTo(
                "pojoField: expected is \"expectedPojoValue\" but was \"actualPojoValue\"")));
    }

    @Test
    @DisplayName("The expected side stays compact: class plus configured expectations")
    void describeTo_staysCompact() {
        // Preparation
        final SomePojoMatcher matcher = isSomePojo()
                .withPojoField("expectedPojoValue");

        // Execution
        final StringDescription description = new StringDescription();
        matcher.describeTo(description);

        // Assertion
        assertThat(description.toString(), is(equalTo(
                "(an instance of foo.bar.sample.model.SomePojo and hasProperty(\"pojoField\", \"expectedPojoValue\"))")));
    }
}
