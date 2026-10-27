/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isAddress;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isBooking;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isContact;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isCustomer;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isOrder;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isOrderLine;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.entries;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInAnyOrder;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Address;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Booking;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Contact;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Customer;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Order;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.OrderLine;
import java.util.List;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;

/** The completeness check of the runtime matcher: opt-in per matcher, off by default. */
class BeanPropertyMatcherCompletenessTest {
    private static final Address PARIS = new Address("Paris");
    private static final Contact ADA = new Contact("Ada", "0170-1", PARIS);

    private static String mismatchOf(final Matcher<?> matcher, final Object actual) {
        final Description description = new StringDescription();
        matcher.describeMismatch(actual, description);
        return description.toString();
    }

    private static String descriptionOf(final BeanPropertyMatcher<?> matcher) {
        final Description description = new StringDescription();
        matcher.describeTo(description);
        return description.toString();
    }

    @Test
    void anUnaddressedPropertyFailsTheCheckWhenItIsOptedIn() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class, "city").checkingAllProperties();

        assertFalse(matcher.matches(PARIS));
        assertEquals("neither checked nor ignored: city", mismatchOf(matcher, PARIS));
    }

    @Test
    void anUnaddressedPropertyDoesNotFailWithoutTheOptIn() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class, "city");

        assertTrue(matcher.matches(PARIS));
    }

    @Test
    void classIsNotAPropertyForTheCompletenessCheck() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class, "city", "class")
                        .with("city", equalTo("Paris"))
                        .checkingAllProperties();

        assertTrue(matcher.matches(PARIS));
    }

    @Test
    void anIgnoredPropertyIsAddressed() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class, "city")
                        .ignore("city")
                        .checkingAllProperties();

        assertTrue(matcher.matches(PARIS));
    }

    @Test
    void aCheckedPropertyIsAddressed() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class, "city")
                        .with("city", equalTo("Paris"))
                        .checkingAllProperties();

        assertTrue(matcher.matches(PARIS));
    }

    @Test
    void resetUnaddressesAPropertyAgain() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class, "city")
                        .with("city", equalTo("Paris"))
                        .checkingAllProperties();

        matcher.reset("city");

        assertFalse(matcher.matches(PARIS));
        assertEquals("neither checked nor ignored: city", mismatchOf(matcher, PARIS));
    }

    @Test
    void resetAlsoRemovesAnIgnoredProperty() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class, "city")
                        .ignore("city")
                        .checkingAllProperties();

        matcher.reset("city");

        assertFalse(matcher.matches(PARIS));
    }

    @Test
    void theDescriptionEndsWithTheSuffixOnlyWithTheOptIn() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class, "city")
                        .with("city", equalTo("Paris"))
                        .checkingAllProperties();

        assertEquals(
                "Address with 1 property check, every property checked or ignored",
                descriptionOf(matcher));
        assertEquals(
                "Address with 1 property check",
                descriptionOf(
                        new BeanPropertyMatcher<>(Address.class, "city")
                                .with("city", equalTo("Paris"))));
    }

    @Test
    void theCheckIsNotInheritedByNestedMatchers() {
        final Matcher<Customer> matcher =
                isCustomer()
                        .with("name", equalTo("Ada"))
                        .with("address", isAddress())
                        .checkingAllProperties();

        assertTrue(matcher.matches(new Customer("Ada", PARIS)));
    }

    @Test
    void theSpecExampleListsTheUnaddressedPathsOfANestedMatcher() {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .with(
                                "customer",
                                isContact().with("name", equalTo("Ada")).checkingAllProperties())
                        .ignoring("contacts")
                        .checkingAllProperties();

        final Booking booking = new Booking("b-1", ADA, List.of());
        assertEquals(
                "neither checked nor ignored: customer.phone, customer.address",
                mismatchOf(matcher, booking));
    }

    @Test
    void unaddressedPropertiesOfTheMatcherComeBeforeThoseOfNestedMatchers() {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .with(
                                "customer",
                                isContact().with("name", equalTo("Ada")).checkingAllProperties())
                        .checkingAllProperties();

        final Booking booking = new Booking("b-1", ADA, List.of());
        assertEquals(
                "neither checked nor ignored: contacts, customer.phone, customer.address",
                mismatchOf(matcher, booking));
    }

    @Test
    void aPropertyCheckedByItemsInOrderCountsAsChecked() {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .with(
                                "customer",
                                isContact()
                                        .with("name", equalTo("Ada"))
                                        .checkingAllProperties()
                                        .ignoring("phone")
                                        .ignoring("address"))
                        .with(
                                "contacts",
                                itemsInOrder(
                                        isContact()
                                                .with("name", equalTo("Ada"))
                                                .ignoring("phone")
                                                .ignoring("address")))
                        .checkingAllProperties();

        assertTrue(matcher.matches(new Booking("b-1", ADA, List.of(ADA))));
    }

    @Test
    void aPropertyCheckedByItemsInAnyOrderCountsAsChecked() {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .ignoring("customer")
                        .with(
                                "contacts",
                                itemsInAnyOrder(
                                        isContact()
                                                .with("name", equalTo("Ada"))
                                                .ignoring("phone")
                                                .ignoring("address")))
                        .checkingAllProperties();

        assertTrue(matcher.matches(new Booking("b-1", ADA, List.of(ADA))));
    }

    @Test
    void aPropertyCheckedByEntriesCountsAsChecked() {
        final Matcher<Order> matcher =
                isOrder()
                        .with("id", equalTo("o-1"))
                        .with("email", equalTo("ada@example.org"))
                        .ignoring("customer")
                        .ignoring("lines")
                        .ignoring("lineArray")
                        .ignoring("tags")
                        .ignoring("byColor")
                        .ignoring("byId")
                        .ignoring("byLine")
                        .ignoring("batches")
                        .with("bySku", entries("B-2", isOrderLine().with("quantity", equalTo(2))))
                        .checkingAllProperties();

        assertTrue(
                matcher.matches(
                        new Order(
                                "o-1",
                                "ada@example.org",
                                null,
                                List.of(),
                                Map.of("B-2", new OrderLine("B-2", 2)))));
    }

    @Test
    void aNestedMatcherInsideItemsInOrderReportsItsIndexedPaths() {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .ignoring("customer")
                        .with(
                                "contacts",
                                itemsInOrder(
                                        isContact().with("name", equalTo("Ada")),
                                        isContact()
                                                .with("name", equalTo("Ada"))
                                                .checkingAllProperties()))
                        .checkingAllProperties();

        final Booking booking = new Booking("b-1", ADA, List.of(ADA, ADA));
        assertEquals(
                "neither checked nor ignored: contacts[1].phone, contacts[1].address",
                mismatchOf(matcher, booking));
    }
}
