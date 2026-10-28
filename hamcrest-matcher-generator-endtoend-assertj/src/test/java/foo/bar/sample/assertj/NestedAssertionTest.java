/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.model.Address;
import foo.bar.sample.assertj.model.AddressAssert;
import foo.bar.sample.assertj.model.Customer;
import foo.bar.sample.assertj.model.CustomerAssert;
import foo.bar.sample.assertj.model.Order;
import foo.bar.sample.assertj.model.OrderAssert;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NestedAssertionTest {

    private static final Order ORDER =
            new Order(
                    42,
                    new Customer("Ada", null, new Address("1 Rue de Rivoli", "Paris")),
                    new BigDecimal("9.99"),
                    List.of(),
                    false);
    private static final Order ORDER_WITHOUT_CUSTOMER =
            new Order(43, null, new BigDecimal("9.99"), List.of(), false);

    @Test
    @DisplayName("A nested failure names the whole path from the root type")
    void aNestedFailureNamesTheWholePathFromTheRootType() {
        assertThatThrownBy(
                        () ->
                                OrderAssert.assertThat(ORDER)
                                        .hasCustomerSatisfying(
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Ada")
                                                                .hasAddressSatisfying(
                                                                        address ->
                                                                                AddressAssert
                                                                                        .assertThat(
                                                                                                address)
                                                                                        .hasCity(
                                                                                                "Berlin"))))
                .hasMessage(
                        String.format(
                                "%nExpecting Order.customer.address.city to be:%n  \"Berlin\"%nbut was:%n  \"Paris\""));
    }

    @Test
    @DisplayName("A null nested value fails with the path of the nested value")
    void aNullNestedValueFailsWithThePathOfTheNestedValue() {
        assertThatThrownBy(
                        () ->
                                OrderAssert.assertThat(ORDER_WITHOUT_CUSTOMER)
                                        .hasCustomerSatisfying(
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Ada")))
                .hasMessage(String.format("%nExpecting Order.customer not to be null"));
    }

    @Test
    @DisplayName("A nested value that matches passes")
    void aNestedValueThatMatchesPasses() {
        OrderAssert.assertThat(ORDER)
                .hasCustomerSatisfying(
                        customer ->
                                CustomerAssert.assertThat(customer)
                                        .hasName("Ada")
                                        .hasAddress(new Address("1 Rue de Rivoli", "Paris")));
    }

    @Test
    @DisplayName("After a requirement the next assertion starts at its own type again")
    void afterARequirementTheNextAssertionStartsAtItsOwnTypeAgain() {
        OrderAssert.assertThat(ORDER).hasCustomerSatisfying(customer -> {});

        assertThatThrownBy(() -> CustomerAssert.assertThat(ORDER.getCustomer()).hasName("Bob"))
                .hasMessageContaining("Expecting Customer.name to be:");
    }

    @Test
    @DisplayName("The path is restored when a requirement fails")
    void thePathIsRestoredWhenARequirementFails() {
        assertThatThrownBy(
                        () ->
                                OrderAssert.assertThat(ORDER)
                                        .hasCustomerSatisfying(
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Bob")))
                .hasMessageContaining("Expecting Order.customer.name to be:");

        assertThatThrownBy(() -> CustomerAssert.assertThat(ORDER.getCustomer()).hasName("Bob"))
                .hasMessageContaining("Expecting Customer.name to be:");
    }
}
