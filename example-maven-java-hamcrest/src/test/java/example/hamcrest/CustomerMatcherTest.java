/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.hamcrest;

import static example.hamcrest.model.AddressMatcher.isAddress;
import static example.hamcrest.model.CustomerMatcher.isCustomer;
import static example.hamcrest.model.CustomerMatcher.isCustomerLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import example.hamcrest.model.Address;
import example.hamcrest.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerMatcherTest {

    private static final Customer ADA =
            new Customer("Ada", new Address("1 Rue de Rivoli", "Paris"), true);

    @Test
    @DisplayName("A basic assertion checks one property by equality")
    void aBasicAssertionChecksOnePropertyByEquality() {
        // tag::basic-assertion[]
        assertThat(ADA, isCustomer().withName("Ada"));
        // end::basic-assertion[]
    }

    @Test
    @DisplayName("A nested property is checked with the matcher of its type")
    void aNestedPropertyIsCheckedWithTheMatcherOfItsType() {
        // tag::nested-property[]
        assertThat(ADA, isCustomer().withAddress(isAddress().withCity("Paris")));
        // end::nested-property[]
    }

    @Test
    @DisplayName("The completeness check passes when every property is checked or ignored")
    void theCompletenessCheckPassesWhenEveryPropertyIsCheckedOrIgnored() {
        // tag::completeness-check[]
        assertThat(
                ADA,
                isCustomer()
                        .withName("Ada")
                        .withAddress(isAddress().withCity("Paris"))
                        .ignoringActive()
                        .checkingAllProperties());
        // end::completeness-check[]
    }

    @Test
    @DisplayName("The completeness check names a property that was neither checked nor ignored")
    void theCompletenessCheckNamesAPropertyThatWasNeitherCheckedNorIgnored() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        ADA,
                                        isCustomer()
                                                .withName("Ada")
                                                .withAddress(isAddress().withCity("Paris"))
                                                .checkingAllProperties()));

        assertThat(error.getMessage(), containsString("active"));
    }

    @Test
    @DisplayName("isXLike compares every property of a reference object, nested ones included")
    void isXLikeComparesEveryPropertyOfAReferenceObject() {
        // tag::reference-object[]
        assertThat(
                ADA,
                isCustomerLike(new Customer("Ada", new Address("1 Rue de Rivoli", "Paris"), true)));
        // end::reference-object[]
    }

    @Test
    @DisplayName("isXLike names the nested property that differs from the reference object")
    void isXLikeNamesTheNestedPropertyThatDiffersFromTheReferenceObject() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        ADA,
                                        isCustomerLike(
                                                new Customer(
                                                        "Ada",
                                                        new Address("1 Rue de Rivoli", "Berlin"),
                                                        true))));

        assertThat(error.getMessage(), containsString("address.city"));
    }
}
