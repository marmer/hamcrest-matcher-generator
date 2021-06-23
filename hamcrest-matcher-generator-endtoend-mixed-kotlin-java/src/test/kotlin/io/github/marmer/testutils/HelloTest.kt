package io.github.marmer.testutils

import io.github.marmer.testutils.model.MultiPojo1
import io.github.marmer.testutils.model.MultiPojo1Matcher
import io.github.marmer.testutils.model.SomePojo
import io.github.marmer.testutils.model.SomePojoMatcher
import org.hamcrest.MatcherAssert

class HelloTest {
    @org.junit.jupiter.api.Test
    fun justSomeKotlinTest() {
        // TODO: marmer 23.06.2021 Check when and how to trigger the annotation processor to be able to use the matcher this way
        MatcherAssert.assertThat(MultiPojo1("blub"), MultiPojo1Matcher.isMultiPojo1().withSomeProp("blub"))
        MatcherAssert.assertThat(SomePojo("blub"), SomePojoMatcher.isSomePojo().withSomeProp("blub"))
    }
}
