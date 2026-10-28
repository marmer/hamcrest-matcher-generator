/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.model.Address;
import foo.bar.sample.assertj.model.Customer;
import foo.bar.sample.assertj.model.CustomerAssert;
import foo.bar.sample.assertj.model.Order;
import foo.bar.sample.assertj.model.SoftAssertionsProvider;
import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.BDDSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SoftAssertionTest {

    private static final Order ORDER =
            new Order(
                    42,
                    new Customer("Ada", null, new Address("1 Rue de Rivoli", "Paris")),
                    new BigDecimal("9.99"),
                    List.of(),
                    false);

    @Test
    @DisplayName("Soft assertions with the generated mixin collect every failure")
    void softAssertionsWithTheGeneratedMixinCollectEveryFailure() {
        final OrderSoftAssertions softly = new OrderSoftAssertions();
        softly.assertThat(ORDER).hasId(1);
        softly.assertThat(ORDER).isPaid();
        softly.assertThat(ORDER)
                .hasCustomerSatisfying(
                        customer -> CustomerAssert.assertThat(customer).hasName("Bob"));

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting Order.id to be:")
                .hasMessageContaining("Expecting Order.paid to be:")
                .hasMessageContaining("Expecting Order.customer.name to be:");
    }

    @Test
    @DisplayName("Soft assertions with the generated mixin pass when every check passes")
    void softAssertionsWithTheGeneratedMixinPassWhenEveryCheckPasses() {
        final OrderSoftAssertions softly = new OrderSoftAssertions();
        softly.assertThat(ORDER).hasId(42).isNotPaid();
        softly.assertThat(ORDER)
                .hasCustomerSatisfying(
                        customer -> CustomerAssert.assertThat(customer).hasName("Ada"));

        assertThatCode(softly::assertAll).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BDD soft assertions with the generated mixin collect every failure")
    void bddSoftAssertionsWithTheGeneratedMixinCollectEveryFailure() {
        final OrderBddSoftAssertions softly = new OrderBddSoftAssertions();
        softly.then(ORDER).hasId(1);
        softly.then(ORDER).isPaid();

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting Order.id to be:")
                .hasMessageContaining("Expecting Order.paid to be:");
    }

    /** The soft assertions of the orders, as an entry point of the test. */
    static class OrderSoftAssertions extends SoftAssertions implements SoftAssertionsProvider {}

    /** The BDD soft assertions of the orders, as an entry point of the test. */
    static class OrderBddSoftAssertions extends BDDSoftAssertions
            implements SoftAssertionsProvider {}
}
