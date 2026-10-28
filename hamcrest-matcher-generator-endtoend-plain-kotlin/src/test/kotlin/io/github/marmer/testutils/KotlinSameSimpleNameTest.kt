/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.testutils.model.Address
import io.github.marmer.testutils.model.Customer
import io.github.marmer.testutils.model.Shipment
import io.github.marmer.testutils.model.isShipment
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class KotlinSameSimpleNameTest {
    private val shipment = Shipment(
        Customer("Ada", Address("Berlin")),
        io.github.marmer.testutils.model.other.Customer("Bob"),
    )

    @Test
    @DisplayName("Two properties of types with the same simple name get their own scopes")
    fun test_TypesWithTheSameSimpleNameKeepTheirScopes() {
        assertThat(shipment, isShipment {
            customer { name("Ada"); address { city("Berlin") } }
            receiver { label("Bob") }
        })
    }

    @Test
    @DisplayName("The scope of the second type checks its own properties")
    fun test_SecondScopeChecksItsOwnProperties() {
        assertThrows(AssertionError::class.java) {
            assertThat(shipment, isShipment { receiver { label("Eve") } })
        }
    }
}
