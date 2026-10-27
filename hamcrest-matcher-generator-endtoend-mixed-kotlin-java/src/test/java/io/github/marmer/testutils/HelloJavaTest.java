package io.github.marmer.testutils;

import io.github.marmer.testutils.model.KotlinProbe;
import io.github.marmer.testutils.model.KotlinProbeMatcher;
import io.github.marmer.testutils.model.MultiPojo1;
import io.github.marmer.testutils.model.MultiPojo1Matcher;
import io.github.marmer.testutils.model.NullabilityProbe;
import io.github.marmer.testutils.model.NullabilityProbeMatcher;
import io.github.marmer.testutils.model.SomePojo;
import io.github.marmer.testutils.model.SomePojoMatcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HelloJavaTest {

    @Test
    @DisplayName("Usage of generated matchers")
    void test_UsageOfGeneratedMatchers() {
        MatcherAssert.assertThat(new MultiPojo1("blub"), MultiPojo1Matcher.isMultiPojo1().withSomeProp("blub"));
        MatcherAssert.assertThat(new SomePojo("blub"), SomePojoMatcher.isSomePojo().withSomeProp("blub"));
    }

    @Test
    @DisplayName("Kotlin getters are asserted through generated matchers")
    void test_KotlinGettersAreAsserted() {
        MatcherAssert.assertThat(
            new KotlinProbe(true, true, "label"),
            KotlinProbeMatcher.isKotlinProbe().withEnabled(true).withIsX(true).withIsLabel("label"));
    }

    @Test
    @DisplayName("Nullable Kotlin properties accept null through generated matchers")
    void test_NullableKotlinPropertiesAcceptNull() {
        MatcherAssert.assertThat(
            new NullabilityProbe("name", null, 1),
            NullabilityProbeMatcher.isNullabilityProbe().withName("name").withNickname((String) null).withCount(1));
    }

}
