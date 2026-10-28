/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.lines.Invoice;
import foo.bar.sample.assertj.lines.Line;
import foo.bar.sample.assertj.lines.LineAssert;
import foo.bar.sample.assertj.model.Address;
import foo.bar.sample.assertj.model.Customer;
import foo.bar.sample.assertj.model.CustomerAssert;
import foo.bar.sample.assertj.model.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NavigationAssertionTest {

    private static final Order ORDER =
            new Order(
                    42,
                    new Customer("Ada", null, new Address("1 Rue de Rivoli", "Paris")),
                    new BigDecimal("9.99"),
                    List.of(),
                    false);
    private static final Line LINE = new Line("A-1", 2);
    private static final Invoice INVOICE =
            new Invoice(List.of(LINE, new Line("B-2", 5)), Map.of("A-1", LINE));

    @Test
    @DisplayName("extracting with as navigates into the generated assertions of the property")
    void extractingWithAsNavigatesIntoTheGeneratedAssertions() {
        assertThatThrownBy(
                        () ->
                                assertThat(ORDER)
                                        .extracting(Order::getCustomer, as(CustomerAssert.CUSTOMER))
                                        .hasName("Bob"))
                .hasMessage(
                        String.format(
                                "%nExpecting Customer.name to be:%n  \"Bob\"%nbut was:%n  \"Ada\""));
    }

    @Test
    @DisplayName("extracting with as passes when the navigated assertions pass")
    void extractingWithAsPassesWhenTheNavigatedAssertionsPass() {
        assertThatCode(
                        () ->
                                assertThat(ORDER)
                                        .extracting(Order::getCustomer, as(CustomerAssert.CUSTOMER))
                                        .hasName("Ada")
                                        .hasPhone(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("first with as navigates into the generated assertions of the first element")
    void firstWithAsNavigatesIntoTheGeneratedAssertions() {
        assertThatThrownBy(
                        () ->
                                assertThat(INVOICE.getLines())
                                        .first(as(LineAssert.LINE))
                                        .hasSku("B-2"))
                .hasMessageEndingWith(
                        String.format(
                                "%nExpecting Line.sku to be:%n  \"B-2\"%nbut was:%n  \"A-1\""));
    }

    @Test
    @DisplayName("first with as passes when the first element meets the assertions")
    void firstWithAsPassesWhenTheFirstElementMeetsTheAssertions() {
        assertThatCode(
                        () ->
                                assertThat(INVOICE.getLines())
                                        .first(as(LineAssert.LINE))
                                        .hasSku("A-1")
                                        .hasQuantity(2))
                .doesNotThrowAnyException();
    }
}
