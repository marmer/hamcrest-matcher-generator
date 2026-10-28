/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.testutils.model.Address
import io.github.marmer.testutils.model.AddressMatcher
import io.github.marmer.testutils.model.Customer
import io.github.marmer.testutils.model.CustomerMatcher
import io.github.marmer.testutils.model.Order
import io.github.marmer.testutils.model.OrderMatcher
import io.github.marmer.testutils.model.isOrder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class KotlinDslTest {
    private val order = Order(42L, Customer("Ada", Address("Berlin")), null)

    @Test
    @DisplayName("Nested scopes check the properties of nested objects")
    fun test_NestedScopes() {
        assertThat(order, isOrder {
            id(42L)
            customer {
                name("Ada")
                address { city("Berlin") }
            }
        })
    }

    @Test
    @DisplayName("A matcher takes the place of a value")
    fun test_MatchersInScopes() {
        assertThat(order, isOrder {
            id(org.hamcrest.Matchers.greaterThan(1L))
            customer { name(startsWith("A")) }
        })
    }

    @Test
    @DisplayName("Null is a legal value for a nullable property")
    fun test_NullValues() {
        assertThat(order, isOrder { note(null) })
    }

    @Test
    @DisplayName("The completeness check is switched on in the scope, and a property can be ignored")
    fun test_CompletenessCheck() {
        assertThat(order, isOrder {
            checkingAllProperties()
            id(42L)
            customer { ignoringName(); address { city("Berlin") } }
            ignoringNote()
            ignoringClass()
        })

        val error = assertThrows(AssertionError::class.java) {
            assertThat(order, isOrder {
                checkingAllProperties()
                id(42L)
            })
        }
        assertEquals(
            describeMismatch(OrderMatcher.isOrder().checkingAllProperties().withId(42L)),
            error.message
        )
    }

    @Test
    @DisplayName("A mismatch reads the same as with the Java matcher")
    fun test_MismatchMessageIsTheOneOfTheJavaMatcher() {
        val dsl = isOrder {
            customer { address { city("Hamburg") } }
        }
        val java = OrderMatcher.isOrder().withCustomer(
            CustomerMatcher.isCustomer().withAddress(AddressMatcher.isAddress().withCity("Hamburg"))
        )

        val error = assertThrows(AssertionError::class.java) { assertThat(order, dsl) }

        assertEquals(describeMismatch(java), error.message)
    }

    private fun describeMismatch(matcher: OrderMatcher): String? =
        assertThrows(AssertionError::class.java) { assertThat(order, matcher) }.message
}
