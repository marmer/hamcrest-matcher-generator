/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.testutils.model.CheckedReading
import io.github.marmer.testutils.model.isCheckedReading
import java.io.IOException
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class CheckedGetterTest {
    @Test
    @DisplayName("A check of a property whose getter throws a checked exception fails with the read failure")
    fun test_CheckOfThrowingGetterFailsWithTheReadFailure() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(CheckedReading(), isCheckedReading { name("sensor") })
        }

        assertEquals("Could not read property 'name'", error.message)
        assertTrue(error.cause is IOException)
        assertEquals("sensor offline", error.cause?.message)
    }

    @Test
    @DisplayName("The other property checks of the scope still pass on their own")
    fun test_OtherPropertyChecksStillPass() {
        assertThat(CheckedReading(), isCheckedReading { count(1) })
    }
}
