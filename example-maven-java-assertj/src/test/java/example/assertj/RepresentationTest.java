/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.assertj;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import example.assertj.model.Address;
import example.assertj.model.Customer;
import example.assertj.model.CustomerAssert;
import example.assertj.model.PropertyRepresentation;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RepresentationTest {

    private static final Customer ADA =
            new Customer("Ada", new Address("1 Rue de Rivoli", "Paris"), true);
    private static final Customer BOB =
            new Customer("Bob", new Address("2 Quai Saint-Antoine", "Lyon"), false);

    @BeforeEach
    @AfterEach
    void useTheDefaultRepresentation() {
        Assertions.useDefaultRepresentation();
    }

    @Test
    @DisplayName("Once registered, a collection failure prints its elements by their properties")
    void onceRegisteredACollectionFailurePrintsItsElementsByTheirProperties() {
        // tag::representation[]
        Assertions.useRepresentation(new PropertyRepresentation());

        assertThatThrownBy(
                        () ->
                                Assertions.assertThat(List.of(ADA, BOB))
                                        .satisfiesExactlyInAnyOrder(
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Ada"),
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Carl")))
                .hasMessageContaining("Customer[name=\"Ada\"")
                .hasMessageContaining("Customer[name=\"Bob\"");
        // end::representation[]
    }

    @Test
    @DisplayName("Without the registration a collection failure prints its elements by toString")
    void withoutTheRegistrationACollectionFailurePrintsItsElementsByToString() {
        assertThatThrownBy(
                        () ->
                                Assertions.assertThat(List.of(ADA, BOB))
                                        .satisfiesExactlyInAnyOrder(
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Ada"),
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Carl")))
                .hasMessageContaining("Customer@")
                .hasMessageNotContaining("Customer[name=");
    }
}
