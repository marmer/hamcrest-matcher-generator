/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.kotlin

import example.kotlin.model.Address
import example.kotlin.model.Customer
import example.kotlin.model.isCustomer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CustomerDslTest {

    private val ada = Customer("Ada", Address("Paris"), true)

    @Test
    @DisplayName("A basic assertion checks one property by equality")
    fun aBasicAssertionChecksOnePropertyByEquality() {
        // tag::basic-assertion[]
        assertThat(ada, isCustomer { name("Ada") })
        // end::basic-assertion[]
    }

    @Test
    @DisplayName("A nested scope checks the properties of the nested object")
    fun aNestedScopeChecksThePropertiesOfTheNestedObject() {
        // tag::nested-property[]
        assertThat(ada, isCustomer {
            address { city("Paris") }
        })
        // end::nested-property[]
    }

    @Test
    @DisplayName("The completeness check passes when every property is checked or ignored")
    fun theCompletenessCheckPassesWhenEveryPropertyIsCheckedOrIgnored() {
        // tag::completeness-check[]
        assertThat(ada, isCustomer {
            checkingAllProperties()
            name("Ada")
            address { city("Paris") }
            ignoringActive()
        })
        // end::completeness-check[]
    }

    @Test
    @DisplayName("A wrong value in a nested scope fails")
    fun aWrongValueInANestedScopeFails() {
        assertThrows(AssertionError::class.java) {
            assertThat(ada, isCustomer { address { city("Berlin") } })
        }
    }

    @Test
    @DisplayName("The completeness check names a property that was neither checked nor ignored")
    fun theCompletenessCheckNamesAPropertyThatWasNeitherCheckedNorIgnored() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(ada, isCustomer {
                checkingAllProperties()
                name("Ada")
                address { city("Paris") }
            })
        }

        assertThat(error.message, containsString("active"))
    }
}
