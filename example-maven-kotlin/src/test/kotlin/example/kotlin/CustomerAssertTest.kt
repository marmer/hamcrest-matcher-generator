/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.kotlin

import example.kotlin.model.Address
import example.kotlin.model.AddressAssert
import example.kotlin.model.Customer
import example.kotlin.model.CustomerAssert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CustomerAssertTest {

    private val ada = Customer("Ada", Address("Paris"), true)

    @Test
    @DisplayName("The AssertJ backend checks one property by equality, next to the Hamcrest one")
    fun theAssertJBackendChecksOnePropertyByEquality() {
        // tag::assertj-assertion[]
        CustomerAssert.assertThat(ada)
            .hasName("Ada")
            .hasAddressSatisfying { AddressAssert.assertThat(it).hasCity("Paris") }
        // end::assertj-assertion[]
    }

    @Test
    @DisplayName("A value mismatch names the property with its expected and actual value")
    fun aValueMismatchNamesThePropertyWithItsExpectedAndActualValue() {
        assertThatThrownBy { CustomerAssert.assertThat(ada).hasName("Bob") }
            .hasMessageContaining("Customer.name")
            .hasMessageContaining("Bob")
            .hasMessageContaining("Ada")
    }
}
