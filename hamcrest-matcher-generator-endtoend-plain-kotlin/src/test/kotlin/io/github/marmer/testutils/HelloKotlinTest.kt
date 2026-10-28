/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.testutils.model.MultiPojo1
import io.github.marmer.testutils.model.KotlinProbe
import io.github.marmer.testutils.model.KotlinProbeMatcher
import io.github.marmer.testutils.model.NullabilityProbe
import io.github.marmer.testutils.model.NullabilityProbeMatcher
import io.github.marmer.testutils.model.MultiPojo1Matcher
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

// This module compiles with -language-version 1.9 (see pom.xml): Kotlin 1.9 does not read the JSpecify
// annotations, so a bare null is ambiguous between the value and the matcher overload and needs the cast.
// The mixed module compiles with the default language version and uses the bare null.
internal class HelloKotlinTest {
    @Test
    @DisplayName("Usage of generated matchers")
    fun test_UsageOfGeneratedMatchers() {
        MatcherAssert.assertThat(MultiPojo1("blub"), MultiPojo1Matcher.isMultiPojo1().withSomeProp("blub"))
    }

    @Test
    @DisplayName("Kotlin getters are asserted through generated matchers")
    fun test_KotlinGettersAreAsserted() {
        MatcherAssert.assertThat(
            KotlinProbe(enabled = true, isX = true, isLabel = "label"),
            KotlinProbeMatcher.isKotlinProbe().withEnabled(true).withIsX(true).withIsLabel("label")
        )
    }

    @Test
    @DisplayName("Nullable properties accept null without nullness errors")
    fun test_NullablePropertiesAcceptNull() {
        MatcherAssert.assertThat(
            NullabilityProbe("name", null, 1),
            NullabilityProbeMatcher.isNullabilityProbe().withNickname(null as String?).withName("name").withCount(1)
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
            Matchers.not(NullabilityProbeMatcher.isNullabilityProbe().withName(null as String?))
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
