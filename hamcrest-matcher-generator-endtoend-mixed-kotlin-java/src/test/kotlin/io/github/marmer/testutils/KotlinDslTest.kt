/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.testutils.model.KotlinProbe
import io.github.marmer.testutils.model.Parcel
import io.github.marmer.testutils.model.ParcelMatcher
import io.github.marmer.testutils.model.isKotlinProbe
import io.github.marmer.testutils.model.isParcel
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class KotlinDslTest {
    private val parcel = Parcel(12L, Parcel.Recipient("Ada", Parcel.Place("Berlin")))

    @Test
    @DisplayName("Nested scopes check the properties of nested Java objects")
    fun test_NestedScopes() {
        assertThat(parcel, isParcel {
            weight(12L)
            recipient {
                name("Ada")
                place { city("Berlin") }
            }
        })
    }

    @Test
    @DisplayName("The DSL of a Kotlin class checks its Kotlin properties")
    fun test_KotlinProperties() {
        assertThat(
            KotlinProbe(enabled = true, isX = null, isLabel = "label"),
            isKotlinProbe {
                enabled(true)
                isX(null)
                isLabel("label")
            }
        )
    }

    @Test
    @DisplayName("A mismatch reads the same as with the Java matcher")
    fun test_MismatchMessageIsTheOneOfTheJavaMatcher() {
        val dsl = isParcel { recipient { place { city("Hamburg") } } }
        val java = ParcelMatcher.isParcel().withRecipient(
            ParcelMatcher.RecipientMatcher.isRecipient()
                .withPlace(ParcelMatcher.PlaceMatcher.isPlace().withCity("Hamburg"))
        )

        val expected = assertThrows(AssertionError::class.java) { assertThat(parcel, java) }
        val actual = assertThrows(AssertionError::class.java) { assertThat(parcel, dsl) }

        assertEquals(expected.message, actual.message)
    }
}
