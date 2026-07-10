package foo.bar.sample.model;

import org.hamcrest.StringDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static foo.bar.sample.model.SomePojoMatcher.isSomePojo;
import static foo.bar.sample.model.SomePojoMatcher.isSomePojoEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NewFeaturesTest {

    private SomePojo newPojo(final String pojoField, final String parentField, final int intProperty) {
        final SomePojo pojo = new SomePojo();
        pojo.setPojoField(pojoField);
        pojo.setParentField(parentField);
        pojo.setIntProperty(intProperty);
        return pojo;
    }

    @Test
    @DisplayName("Reference object matcher matches objects with equal properties")
    void isSomePojoEqualTo_matchesEqualObjects() {
        final SomePojo reference = newPojo("value", "parentValue", 42);
        final SomePojo actual = newPojo("value", "parentValue", 42);

        assertThat(actual, isSomePojoEqualTo(reference));
    }

    @Test
    @DisplayName("Reference object matcher produces a property diff for differing objects")
    void isSomePojoEqualTo_producesPropertyDiff() {
        final SomePojo reference = newPojo("value", "parentValue", 42);
        final SomePojo actual = newPojo("otherValue", "parentValue", 7);

        final SomePojoMatcher matcher = isSomePojoEqualTo(reference);
        assertThat(matcher.matches(actual), is(false));

        final StringDescription mismatchDescription = new StringDescription();
        matcher.describeMismatch(actual, mismatchDescription);
        assertThat(mismatchDescription.toString(), is(equalTo(
                "pojoField: expected \"value\" but was \"otherValue\"\n"
                        + "intProperty: expected <42> but was <7>")));
    }

    @Test
    @DisplayName("Individual expectations of a reference object matcher can be overridden")
    void isSomePojoEqualTo_expectationsCanBeOverridden() {
        final SomePojo reference = newPojo("value", "parentValue", 42);
        final SomePojo actual = newPojo("otherValue", "parentValue", 42);

        assertThat(actual, isSomePojoEqualTo(reference)
                .resetPojoField()
                .withPojoField("otherValue"));
    }

    @Test
    @DisplayName("Strict mode passes when all generated properties have expectations")
    void strict_passesWhenAllPropertiesAreChecked() {
        final SomePojo actual = newPojo("value", "parentValue", 42);

        assertThat(actual, isSomePojo()
                .strict()
                .withPojoField("value")
                .withParentField("parentValue")
                .withIntProperty(42));
    }

    @Test
    @DisplayName("Strict mode fails and lists unchecked properties")
    void strict_failsForUncheckedProperties() {
        final SomePojo actual = newPojo("value", "parentValue", 42);

        final SomePojoMatcher matcher = isSomePojo()
                .strict()
                .withPojoField("value");
        assertThat(matcher.matches(actual), is(false));

        final StringDescription mismatchDescription = new StringDescription();
        matcher.describeMismatch(actual, mismatchDescription);
        assertThat(mismatchDescription.toString(), is(equalTo(
                "intProperty: unchecked property (strict mode)\n"
                        + "parentField: unchecked property (strict mode)")));
    }

    @Test
    @DisplayName("No matcher is generated for excluded types")
    void exclude_noMatcherIsGeneratedForExcludedTypes() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("foo.bar.sample.model.ExcludedPojoMatcher"));
    }
}
