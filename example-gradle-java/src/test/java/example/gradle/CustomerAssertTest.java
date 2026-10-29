/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.gradle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import example.gradle.model.Address;
import example.gradle.model.AddressAssert;
import example.gradle.model.Customer;
import example.gradle.model.CustomerAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerAssertTest {

    private static final Customer ADA =
            new Customer("Ada", new Address("1 Rue de Rivoli", "Paris"), true);

    @Test
    @DisplayName("The AssertJ backend checks one property by equality")
    void theAssertJBackendChecksOnePropertyByEquality() {
        // tag::assertj-assertion[]
        CustomerAssert.assertThat(ADA)
                .hasName("Ada")
                .hasAddressSatisfying(address -> AddressAssert.assertThat(address).hasCity("Paris"));
        // end::assertj-assertion[]
    }

    @Test
    @DisplayName("A value mismatch names the property with its expected and actual value")
    void aValueMismatchNamesThePropertyWithItsExpectedAndActualValue() {
        assertThatThrownBy(() -> CustomerAssert.assertThat(ADA).hasName("Bob"))
                .hasMessageContaining("Customer.name")
                .hasMessageContaining("Bob")
                .hasMessageContaining("Ada");
    }
}
