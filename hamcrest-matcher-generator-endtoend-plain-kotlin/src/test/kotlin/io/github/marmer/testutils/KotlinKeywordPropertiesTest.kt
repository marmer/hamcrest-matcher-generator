/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.testutils.model.Keywords
import io.github.marmer.testutils.model.isKeywords
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class KotlinKeywordPropertiesTest {
    private val keywords = Keywords("inside", "box")

    @Test
    @DisplayName("Properties named like Kotlin keywords are checked through escaped DSL functions")
    fun test_KeywordPropertiesAreEscaped() {
        assertThat(keywords, isKeywords {
            `in`("inside")
            `object`(equalTo("box"))
        })
        assertThrows(AssertionError::class.java) {
            assertThat(keywords, isKeywords { `in`("outside") })
        }
    }
}
