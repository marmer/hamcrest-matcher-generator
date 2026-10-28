/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static foo.bar.sample.assertj.model.OrderAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.model.Address;
import foo.bar.sample.assertj.model.Customer;
import foo.bar.sample.assertj.model.CustomerAssert;
import foo.bar.sample.assertj.model.Order;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BeanAssertionTest {

    private static final Order ORDER =
            new Order(
                    42,
                    new Customer("Ada", null, new Address("1 Rue de Rivoli", "Paris")),
                    new BigDecimal("9.99"),
                    List.of("books", "sale"),
                    true);

    @Test
    @DisplayName("Properties of a bean are asserted with hasFoo")
    void propertiesOfABeanAreAssertedWithHasFoo() {
        assertThatCode(
                        () ->
                                assertThat(ORDER)
                                        .hasId(42)
                                        .hasTotal(new BigDecimal("9.99"))
                                        .hasTags(List.of("books", "sale"))
                                        .hasPaid(true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A boolean property is asserted with isFoo and isNotFoo")
    void aBooleanPropertyIsAssertedWithIsFooAndIsNotFoo() {
        assertThatCode(() -> assertThat(ORDER).isPaid().isNotNull()).doesNotThrowAnyException();
        assertThatThrownBy(() -> assertThat(ORDER).isNotPaid())
                .hasMessage(
                        String.format("%nExpecting Order.paid to be:%n  false%nbut was:%n  true"));
    }

    @Test
    @DisplayName("hasFoo accepts null for an absent value")
    void hasFooAcceptsNullForAnAbsentValue() {
        assertThatCode(() -> CustomerAssert.assertThat(ORDER.getCustomer()).hasPhone(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A wrong value fails in AssertJ's style with the path from the root type")
    void aWrongValueFailsInAssertJsStyleWithThePathFromTheRootType() {
        assertThatThrownBy(() -> assertThat(ORDER).hasTotal(new BigDecimal("10.00")))
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        String.format("%nExpecting Order.total to be:%n  10.00%nbut was:%n  9.99"));
    }

    @Test
    @DisplayName("Strings are rendered as AssertJ renders them")
    void stringsAreRenderedAsAssertJRendersThem() {
        assertThatThrownBy(() -> CustomerAssert.assertThat(ORDER.getCustomer()).hasName("Bob"))
                .hasMessage(
                        String.format(
                                "%nExpecting Customer.name to be:%n  \"Bob\"%nbut was:%n  \"Ada\""));
    }

    @Test
    @DisplayName("Collections are compared deeply")
    void collectionsAreComparedDeeply() {
        assertThatThrownBy(() -> assertThat(ORDER).hasTags(List.of("books")))
                .hasMessage(
                        String.format(
                                "%nExpecting Order.tags to be:%n  [\"books\"]%nbut was:%n  [\"books\", \"sale\"]"));
    }

    @Test
    @DisplayName("A missing asserted object fails with its type")
    void aMissingAssertedObjectFailsWithItsType() {
        assertThatThrownBy(() -> assertThat(null).hasId(42))
                .hasMessage(String.format("%nExpecting Order not to be null"));
    }

    @Test
    @DisplayName("The built-in assertions stay available")
    void theBuiltInAssertionsStayAvailable() {
        assertThat(ORDER).isNotNull().isSameAs(ORDER).isInstanceOf(Order.class).hasId(42);
        assertThatThrownBy(() -> assertThat(ORDER).isNull()).isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("A description is part of the failure message")
    void aDescriptionIsPartOfTheFailureMessage() {
        assertThatThrownBy(() -> assertThat(ORDER).as("the order").hasId(1))
                .hasMessageStartingWith("[the order] ");
    }

    @Test
    @DisplayName("A custom failure message replaces the generated one")
    void aCustomFailureMessageReplacesTheGeneratedOne() {
        assertThatThrownBy(() -> assertThat(ORDER).withFailMessage("custom").hasId(1))
                .hasMessage("custom");
    }

    @Test
    @DisplayName("A description applies to the failure of a missing asserted object")
    void aDescriptionAppliesToTheFailureOfAMissingAssertedObject() {
        assertThatThrownBy(() -> assertThat(null).as("the order").hasId(1))
                .hasMessage(String.format("[the order] %nExpecting Order not to be null"));
    }

    @Test
    @DisplayName("A custom failure message applies to the failure of a missing asserted object")
    void aCustomFailureMessageAppliesToTheFailureOfAMissingAssertedObject() {
        assertThatThrownBy(() -> assertThat(null).withFailMessage("custom").hasId(1))
                .hasMessage("custom");
    }

    @Test
    @DisplayName("An overriding error message applies to both failures")
    void anOverridingErrorMessageAppliesToBothFailures() {
        assertThatThrownBy(() -> assertThat(null).overridingErrorMessage("overridden").hasId(1))
                .hasMessage("overridden");
        assertThatThrownBy(() -> assertThat(ORDER).overridingErrorMessage("overridden").hasId(1))
                .hasMessage("overridden");
    }

    @Test
    @DisplayName("isNotNull names the path of the asserted object")
    void isNotNullNamesThePathOfTheAssertedObject() {
        assertThatThrownBy(() -> assertThat(null).isNotNull())
                .hasMessage(String.format("%nExpecting Order not to be null"));
    }
}
