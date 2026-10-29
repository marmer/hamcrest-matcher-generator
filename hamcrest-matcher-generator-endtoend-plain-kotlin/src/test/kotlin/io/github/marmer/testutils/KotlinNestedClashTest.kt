/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.testutils.model.Address
import io.github.marmer.testutils.model.AddressMatcher
import io.github.marmer.testutils.model.Customer
import io.github.marmer.testutils.model.CustomerMatcher
import io.github.marmer.testutils.model.NestedClash
import io.github.marmer.testutils.model.NestedClashMatcher
import io.github.marmer.testutils.model.isCustomer
import io.github.marmer.testutils.model.isNestedClash
import io.github.marmer.testutils.model.isNestedClashCustomer
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class KotlinNestedClashTest {
    private val clash = NestedClash(Customer("Ada", Address("Berlin")), NestedClash.Customer("inner"))

    @Test
    @DisplayName("A nested type named like a top level type of the package gets a scope named after its enclosing type")
    fun test_BothScopesAreUsable() {
        assertThat(clash, isNestedClash {
            customer { name("Ada"); address { city("Berlin") } }
            inner { label("inner") }
        })
        assertThat(NestedClash.Customer("inner"), isNestedClashCustomer { label("inner") })
        assertThat(Customer("Ada", Address("Berlin")), isCustomer { name("Ada") })
    }

    @Test
    @DisplayName("Mismatches of both scopes read the same as with the Java matchers")
    fun test_MismatchMessagesAreTheOnesOfTheJavaMatchers() {
        val dsl = isNestedClash {
            customer { address { city("Hamburg") } }
            inner { label("other") }
        }
        val java = NestedClashMatcher.isNestedClash()
            .withCustomer(CustomerMatcher.isCustomer().withAddress(AddressMatcher.isAddress().withCity("Hamburg")))
            .withInner(NestedClashMatcher.CustomerMatcher.isCustomer().withLabel("other"))

        val dslError = assertThrows(AssertionError::class.java) { assertThat(clash, dsl) }
        val javaError = assertThrows(AssertionError::class.java) { assertThat(clash, java) }

        assertEquals(javaError.message, dslError.message)
    }
}
