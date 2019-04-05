package io.github.marmer.annotationprocessing.extraction;

import lombok.var;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class ListUtilTest {
    @Test
    @DisplayName("Two collections should be merged into one")
    void testJoinToList_TwoCollectionsShouldBeMergedIntoOne()
            throws Exception {
        // Preparation

        // Execution
        final var result = ListUtil.joinToList(asList(1, 3), asList(2, 4));

        // Assertion
        assertThat(result, contains(1, 3, 2, 4));
    }
}