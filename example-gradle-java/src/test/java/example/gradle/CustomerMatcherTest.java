/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.gradle;

import static example.gradle.model.AddressMatcher.isAddress;
import static example.gradle.model.CustomerMatcher.isCustomer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import example.gradle.model.Address;
import example.gradle.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerMatcherTest {

    private static final Customer ADA =
            new Customer("Ada", new Address("1 Rue de Rivoli", "Paris"), true);

    @Test
    @DisplayName("The Hamcrest backend checks one property by equality")
    void theHamcrestBackendChecksOnePropertyByEquality() {
        // tag::hamcrest-assertion[]
        assertThat(ADA, isCustomer().withName("Ada"));
        // end::hamcrest-assertion[]
    }

    @Test
    @DisplayName("A nested property is checked with the matcher of its type")
    void aNestedPropertyIsCheckedWithTheMatcherOfItsType() {
        assertThat(ADA, isCustomer().withAddress(isAddress().withCity("Paris")));
    }

    @Test
    @DisplayName("A wrong value fails with the property named")
    void aWrongValueFailsWithThePropertyNamed() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () -> assertThat(ADA, isCustomer().withName("Bob")));

        assertThat(error.getMessage(), containsString("name"));
    }
}
