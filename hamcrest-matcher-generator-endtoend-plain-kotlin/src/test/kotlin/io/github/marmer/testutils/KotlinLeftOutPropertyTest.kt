/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.testutils.model.Lookalike
import io.github.marmer.testutils.model.LookalikeMatcher
import io.github.marmer.testutils.model.isLookalike
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class KotlinLeftOutPropertyTest {
    private val lookalike = Lookalike(1L, "code")

    @Test
    @DisplayName("The completeness check of a scope ignores a property the DSL leaves out")
    fun test_CompletenessCheckIgnoresTheLeftOutProperty() {
        assertThat(lookalike, isLookalike {
            checkingAllProperties()
            id(1L)
        })
    }

    @Test
    @DisplayName("The completeness check of a scope still names another property it does not check")
    fun test_CompletenessCheckNamesAnotherUncheckedProperty() {
        val dslError = assertThrows(AssertionError::class.java) {
            assertThat(lookalike, isLookalike { checkingAllProperties() })
        }
        val javaError = assertThrows(AssertionError::class.java) {
            assertThat(
                lookalike,
                LookalikeMatcher.isLookalike().checkingAllProperties().ignoringHashCode()
            )
        }

        assertTrue(dslError.message!!.contains("neither checked nor ignored: id"), dslError.message)
        assertEquals(javaError.message, dslError.message)
    }
}
