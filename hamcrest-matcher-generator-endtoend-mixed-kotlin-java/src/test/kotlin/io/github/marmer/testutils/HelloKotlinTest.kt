package io.github.marmer.testutils

import io.github.marmer.testutils.model.MultiPojo1
import io.github.marmer.testutils.model.MultiPojo1Matcher
import io.github.marmer.testutils.model.NullabilityProbe
import io.github.marmer.testutils.model.NullabilityProbeMatcher
import io.github.marmer.testutils.model.SomePojo
import io.github.marmer.testutils.model.SomePojoMatcher
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class HelloKotlinTest {
    @Test
    @DisplayName("Usage of generated matchers")
    fun test_UsageOfGeneratedMatchers() {
        MatcherAssert.assertThat(MultiPojo1("blub"), MultiPojo1Matcher.isMultiPojo1().withSomeProp("blub"))
        MatcherAssert.assertThat(SomePojo("blub"), SomePojoMatcher.isSomePojo().withSomeProp("blub"))
    }

    @Test
    @DisplayName("Nullable properties accept null without nullness errors")
    fun test_NullablePropertiesAcceptNull() {
        MatcherAssert.assertThat(
            NullabilityProbe("name", null, 1),
            NullabilityProbeMatcher.isNullabilityProbe().withNickname(null).withName("name").withCount(1)
        )
        MatcherAssert.assertThat(
            NullabilityProbe("name", null, 1),
            NullabilityProbeMatcher.isNullabilityProbe().withNickname(Matchers.nullValue())
        )
    }

    @Test
    @DisplayName("Equality values accept null for non null properties as well")
    fun test_EqualityValuesAcceptNull() {
        MatcherAssert.assertThat(
            NullabilityProbe("name", "nick", 1),
            Matchers.not(NullabilityProbeMatcher.isNullabilityProbe().withName(null))
        )
    }

    @Test
    @DisplayName("Generated matchers are typed non null")
    fun test_GeneratedMatchersAreTypedNonNull() {
        val matcher: NullabilityProbeMatcher = NullabilityProbeMatcher.isNullabilityProbe()
        val configured: NullabilityProbeMatcher = matcher.withName("name").withNickname("nick").withCount(1)

        MatcherAssert.assertThat(NullabilityProbe("name", "nick", 1), configured)
    }
}
