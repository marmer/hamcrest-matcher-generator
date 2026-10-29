/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static foo.bar.sample.assertj.model.AddressAssert.assertThat;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.model.Address;
import foo.bar.sample.assertj.model.Badge;
import foo.bar.sample.assertj.model.Customer;
import foo.bar.sample.assertj.model.CustomerAssert;
import foo.bar.sample.assertj.model.Membership;
import foo.bar.sample.assertj.model.MembershipAssert;
import foo.bar.sample.assertj.model.Order;
import foo.bar.sample.assertj.model.OrderAssert;
import foo.bar.sample.assertj.model.SoftAssertionsProvider;
import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompletenessCheckTest {

    private static final Order ORDER =
            new Order(
                    42,
                    new Customer("Ada", null, new Address("1 Rue de Rivoli", "Paris")),
                    new BigDecimal("9.99"),
                    List.of("books", "sale"),
                    false);

    private static final Customer MEMBER =
            new Customer("Ada", null, new Address("1 Rue de Rivoli", "Paris"));
    private static final Membership MEMBERSHIP =
            new Membership("gold", MEMBER, MEMBER, new Badge("g"));

    @Test
    @DisplayName("The short form passes when every property is checked, whatever the check kind")
    void theShortFormPassesWhenEveryPropertyIsChecked() {
        assertThatCode(
                        () ->
                                OrderAssert.assertThat(ORDER)
                                        .hasId(42)
                                        .hasCustomerSatisfying(
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Ada"))
                                        .hasTotal(new BigDecimal("9.99"))
                                        .hasTags(List.of("books", "sale"))
                                        .isNotPaid()
                                        .hasAllPropertiesChecked())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ignoringFoo counts a property as addressed")
    void ignoringCountsAPropertyAsAddressed() {
        assertThatCode(
                        () ->
                                OrderAssert.assertThat(ORDER)
                                        .ignoringId()
                                        .ignoringCustomer()
                                        .ignoringTotal()
                                        .ignoringTags()
                                        .ignoringPaid()
                                        .hasAllPropertiesChecked())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The scope form passes when every property is checked or ignored")
    void theScopeFormPassesWhenEveryPropertyIsCheckedOrIgnored() {
        assertThatCode(
                        () ->
                                OrderAssert.assertThat(ORDER)
                                        .satisfiesCheckingAllProperties(
                                                order ->
                                                        order.hasId(42)
                                                                .hasTotal(new BigDecimal("9.99"))
                                                                // Only pins that satisfying counts
                                                                // as addressed.
                                                                .hasTagsSatisfying(tags -> {})
                                                                .isNotPaid()
                                                                .ignoringCustomer()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A property that is neither checked nor ignored fails the scope form")
    void aPropertyThatIsNeitherCheckedNorIgnoredFailsTheScopeForm() {
        assertThatThrownBy(
                        () ->
                                OrderAssert.assertThat(ORDER)
                                        .satisfiesCheckingAllProperties(
                                                order ->
                                                        order.hasId(42)
                                                                .hasTotal(new BigDecimal("9.99"))
                                                                .isNotPaid()
                                                                .ignoringCustomer()))
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        String.format(
                                "%nExpecting every property of Order to be checked or ignored,"
                                        + " but these were neither: tags"));
    }

    @Test
    @DisplayName("The scope form counts only what its lambda addresses, not the chain before it")
    void theScopeFormCountsOnlyWhatItsLambdaAddresses() {
        final Customer customer =
                new Customer("Ada", null, new Address("1 Rue de Rivoli", "Paris"));

        assertThatThrownBy(
                        () ->
                                CustomerAssert.assertThat(customer)
                                        .hasName("Ada")
                                        .satisfiesCheckingAllProperties(
                                                c -> c.hasPhone(null).ignoringAddress()))
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        String.format(
                                "%nExpecting every property of Customer to be checked or ignored,"
                                        + " but these were neither: name"));
    }

    @Test
    @DisplayName(
            "A scope that addresses every property passes, and the chain after it still counts")
    void aScopeThatAddressesEveryPropertyPassesAndTheChainAfterItStillCounts() {
        final Customer customer =
                new Customer("Ada", null, new Address("1 Rue de Rivoli", "Paris"));

        assertThatCode(
                        () ->
                                CustomerAssert.assertThat(customer)
                                        .hasName("Ada")
                                        .satisfiesCheckingAllProperties(
                                                c ->
                                                        c.hasName("Ada")
                                                                .hasPhone(null)
                                                                .ignoringAddress())
                                        .hasAllPropertiesChecked())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A property that is not checked fails the short form")
    void aPropertyThatIsNotCheckedFailsTheShortForm() {
        // QS-C2: the check is what makes a newly added property fail a test that uses it. The
        // city below stands for such a property, which the test does not check yet.
        final Address address = new Address("1 Rue de Rivoli", "Paris");

        assertThatCode(
                        () ->
                                assertThat(address)
                                        .hasStreet("1 Rue de Rivoli")
                                        .hasCity("Paris")
                                        .hasAllPropertiesChecked())
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () ->
                                assertThat(address)
                                        .hasStreet("1 Rue de Rivoli")
                                        .hasAllPropertiesChecked())
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                        String.format(
                                "%nExpecting every property of Address to be checked or ignored,"
                                        + " but these were neither: city"));
    }

    @Test
    @DisplayName("A wrong value fails before the completeness check runs")
    void aWrongValueFailsBeforeTheCompletenessCheckRuns() {
        assertThatThrownBy(
                        () ->
                                OrderAssert.assertThat(ORDER)
                                        .satisfiesCheckingAllProperties(
                                                order -> order.hasId(1).isNotPaid()))
                .hasMessage(String.format("%nExpecting Order.id to be:%n  1L%nbut was:%n  42L"));
    }

    @Test
    @DisplayName("The neither line of a nested check names the path of the nested type")
    void theNeitherLineOfANestedCheckNamesThePathOfTheNestedType() {
        assertThatThrownBy(
                        () ->
                                OrderAssert.assertThat(ORDER)
                                        .hasCustomerSatisfying(
                                                customer ->
                                                        CustomerAssert.assertThat(customer)
                                                                .hasName("Ada")
                                                                .hasAllPropertiesChecked()))
                .hasMessage(
                        String.format(
                                "%nExpecting every property of Order.customer to be checked or"
                                        + " ignored, but these were neither: phone, address"));
    }

    @Test
    @DisplayName("A soft assertion collects the neither line with the other failures")
    void aSoftAssertionCollectsTheNeitherLineWithTheOtherFailures() {
        final OrderSoftAssertions softly = new OrderSoftAssertions();
        softly.assertThat(ORDER)
                .satisfiesCheckingAllProperties(order -> order.hasId(1).isNotPaid());
        softly.assertThat(ORDER).hasId(42).isNotPaid().hasAllPropertiesChecked();

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting Order.id to be:")
                .hasMessageContaining(
                        "Expecting every property of Order to be checked or ignored,"
                                + " but these were neither: customer, total, tags");
    }

    @Test
    @DisplayName("After extracting with as a property of the same name is not addressed yet")
    void afterExtractingWithAsAPropertyOfTheSameNameIsNotAddressedYet() {
        assertThatThrownBy(
                        () ->
                                MembershipAssert.assertThat(MEMBERSHIP)
                                        .hasName("gold")
                                        .extracting(
                                                Membership::getMember, as(CustomerAssert.CUSTOMER))
                                        .hasPhone(null)
                                        .ignoringAddress()
                                        .hasAllPropertiesChecked())
                .hasMessage(
                        String.format(
                                "%nExpecting every property of Customer to be checked or"
                                        + " ignored, but these were neither: name"));
    }

    @Test
    @DisplayName("After first with as a property of the same name is not addressed yet")
    void afterFirstWithAsAPropertyOfTheSameNameIsNotAddressedYet() {
        MembershipAssert.assertThat(MEMBERSHIP).hasName("gold");

        assertThatThrownBy(
                        () ->
                                org.assertj.core.api.Assertions.assertThat(List.of(MEMBERSHIP))
                                        .first(as(MembershipAssert.MEMBERSHIP))
                                        .ignoringMember()
                                        .ignoringBillingMember()
                                        .ignoringBadge()
                                        .hasAllPropertiesChecked())
                .hasMessageEndingWith(
                        String.format(
                                "%nExpecting every property of Membership to be checked or"
                                        + " ignored, but these were neither: name"));
    }

    @Test
    @DisplayName("The navigated assertion passes when it addresses all its own properties")
    void theNavigatedAssertionPassesWhenItAddressesAllItsOwnProperties() {
        assertThatCode(
                        () ->
                                MembershipAssert.assertThat(MEMBERSHIP)
                                        .extracting(
                                                Membership::getMember, as(CustomerAssert.CUSTOMER))
                                        .hasName("Ada")
                                        .ignoringPhone()
                                        .ignoringAddress()
                                        .hasAllPropertiesChecked())
                .doesNotThrowAnyException();
    }

    /** The soft assertions of the orders, as an entry point of the test. */
    static class OrderSoftAssertions extends SoftAssertions implements SoftAssertionsProvider {}
}
