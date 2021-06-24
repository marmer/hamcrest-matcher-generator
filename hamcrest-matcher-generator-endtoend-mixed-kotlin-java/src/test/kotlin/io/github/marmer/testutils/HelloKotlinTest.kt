package io.github.marmer.testutils

import io.github.marmer.testutils.model.MultiPojo1
import io.github.marmer.testutils.model.MultiPojo1Matcher
import io.github.marmer.testutils.model.SomePojo
import io.github.marmer.testutils.model.SomePojoMatcher
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class HelloKotlinTest {
    @Test
    @DisplayName("Usage of generated matchers")
    fun test_UsageOfGeneratedMatchers() {
        MatcherAssert.assertThat(MultiPojo1("blub"), MultiPojo1Matcher.isMultiPojo1().withSomeProp("blub"))
        MatcherAssert.assertThat(SomePojo("blub"), SomePojoMatcher.isSomePojo().withSomeProp("blub"))
    }
}

