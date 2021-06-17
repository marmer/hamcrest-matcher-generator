package io.github.marmer.annotationprocessing.extraction

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

internal class ListUtilTest {
    @Test
    @DisplayName("Two collections should be merged into one")
    fun testJoinToList_TwoCollectionsShouldBeMergedIntoOne() {
        // Preparation

        // Execution
        val result = ListUtil.joinToList(Arrays.asList(1, 3), Arrays.asList(2, 4))

        // Assertion
        MatcherAssert.assertThat(result, Matchers.contains(1, 3, 2, 4))
    }
}
