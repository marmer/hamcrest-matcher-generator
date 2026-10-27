/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.lombok;

import static io.github.marmer.testutils.lombok.model.LombokPojoMatcher.isLombokPojo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.github.marmer.testutils.lombok.model.LombokPojo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LombokGettersTest {

    @Test
    @DisplayName("Matchers assert every Lombok generated getter")
    void test_MatchersAssertEveryLombokGetter() {
        final LombokPojo pojo =
                new LombokPojo("name", 2, true, Boolean.FALSE, true, List.of("tag"));

        assertThat(
                pojo,
                isLombokPojo()
                        .withName("name")
                        .withCount(2)
                        .withActive(true)
                        .withFlagged(false)
                        .withEnabled(true)
                        .withTags(List.of("tag")));
    }

    @Test
    @DisplayName("Matchers accept Hamcrest matchers for Lombok generated getters")
    void test_MatchersAcceptHamcrestMatchers() {
        final LombokPojo pojo = new LombokPojo("name", 2, true, null, false, List.of("tag"));

        assertThat(
                pojo,
                isLombokPojo()
                        .withCount(is(greaterThan(1)))
                        .withActive(is(true))
                        .withFlagged(is((Boolean) null))
                        .withEnabled(is(false)));
    }

    @Test
    @DisplayName("Matchers fail for mismatching Lombok generated getters")
    void test_MatchersFailForMismatchingGetters() {
        final LombokPojo pojo =
                new LombokPojo("name", 2, true, Boolean.FALSE, true, List.of("tag"));

        assertThat(pojo, not(isLombokPojo().withActive(false)));
        assertThat(pojo, not(isLombokPojo().withEnabled(false)));
        assertThat(pojo, not(isLombokPojo().withFlagged(true)));
    }
}
