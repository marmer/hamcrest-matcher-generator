/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.assertj;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import example.assertj.model.Address;
import example.assertj.model.AddressAssert;
import example.assertj.model.Customer;
import example.assertj.model.CustomerAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerAssertTest {

    private static final Customer ADA =
            new Customer("Ada", new Address("1 Rue de Rivoli", "Paris"), true);

    @Test
    @DisplayName("A basic assertion checks one property by equality")
    void aBasicAssertionChecksOnePropertyByEquality() {
        // tag::basic-assertion[]
        CustomerAssert.assertThat(ADA).hasName("Ada");
        // end::basic-assertion[]
    }

    @Test
    @DisplayName("A nested property is checked with the assertion of its type")
    void aNestedPropertyIsCheckedWithTheAssertionOfItsType() {
        // tag::nested-property[]
        CustomerAssert.assertThat(ADA)
                .hasAddressSatisfying(
                        address -> AddressAssert.assertThat(address).hasCity("Paris"));
        // end::nested-property[]
    }

    @Test
    @DisplayName("usingRecursiveComparison compares the whole object in one step")
    void usingRecursiveComparisonComparesTheWholeObjectInOneStep() {
        // tag::whole-object[]
        CustomerAssert.assertThat(ADA)
                .usingRecursiveComparison()
                .isEqualTo(new Customer("Ada", new Address("1 Rue de Rivoli", "Paris"), true));
        // end::whole-object[]
    }

    @Test
    @DisplayName("The completeness check passes when every property is checked or ignored")
    void theCompletenessCheckPassesWhenEveryPropertyIsCheckedOrIgnored() {
        // tag::completeness-check[]
        CustomerAssert.assertThat(ADA)
                .hasName("Ada")
                .hasAddressSatisfying(address -> AddressAssert.assertThat(address).hasCity("Paris"))
                .ignoringActive()
                .hasAllPropertiesChecked();
        // end::completeness-check[]
    }

    @Test
    @DisplayName("The scope form runs the completeness check only inside its lambda")
    void theScopeFormRunsTheCompletenessCheckOnlyInsideItsLambda() {
        // tag::completeness-check-scope[]
        CustomerAssert.assertThat(ADA)
                .satisfiesCheckingAllProperties(
                        customer ->
                                customer.hasName("Ada")
                                        .hasAddressSatisfying(
                                                address ->
                                                        AddressAssert.assertThat(address)
                                                                .hasCity("Paris"))
                                        .isActive());
        // end::completeness-check-scope[]
    }

    @Test
    @DisplayName("A value mismatch names the property with its expected and actual value")
    void aValueMismatchNamesThePropertyWithItsExpectedAndActualValue() {
        assertThatThrownBy(() -> CustomerAssert.assertThat(ADA).hasName("Bob"))
                .hasMessageContaining("Customer.name")
                .hasMessageContaining("Bob")
                .hasMessageContaining("Ada");
    }

    @Test
    @DisplayName("The completeness check names a property that was neither checked nor ignored")
    void theCompletenessCheckNamesAPropertyThatWasNeitherCheckedNorIgnored() {
        assertThatThrownBy(
                        () ->
                                CustomerAssert.assertThat(ADA)
                                        .hasName("Ada")
                                        .hasAddressSatisfying(
                                                address ->
                                                        AddressAssert.assertThat(address)
                                                                .hasCity("Paris"))
                                        .hasAllPropertiesChecked())
                .hasMessageContaining("active");
    }
}
