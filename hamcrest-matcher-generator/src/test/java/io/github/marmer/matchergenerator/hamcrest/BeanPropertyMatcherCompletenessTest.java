/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isAddress;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isBooking;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isContact;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isCustomer;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isOrderLine;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isRegistry;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.entries;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInAnyOrder;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInOrder;
import static io.github.marmer.matchergenerator.hamcrest.GoldenMessages.messageOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Address;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Booking;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Contact;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Customer;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.OrderLine;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Registry;
import java.io.IOException;
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
    private static final Contact BOB = new Contact("Bob", "0170-2", PARIS);

    private final GoldenMessages golden = new GoldenMessages("BeanPropertyMatcherCompletenessTest");

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
    void theSpecExampleListsTheUnaddressedPathsOfANestedMatcher() throws IOException {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .with(
                                "customer",
                                isContact().with("name", equalTo("Ada")).checkingAllProperties())
                        .ignoring("contacts")
                        .checkingAllProperties();

        golden.assertMessage(
                "nested-spec-example", messageOf(new Booking("b-1", ADA, List.of()), matcher));
    }

    @Test
    void unaddressedPropertiesOfTheMatcherComeBeforeThoseOfNestedMatchers() throws IOException {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .with(
                                "customer",
                                isContact().with("name", equalTo("Ada")).checkingAllProperties())
                        .checkingAllProperties();

        golden.assertMessage(
                "own-before-nested", messageOf(new Booking("b-1", ADA, List.of()), matcher));
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
        final Matcher<Registry> matcher =
                isRegistry()
                        .ignoring("contacts")
                        .with("bySku", entries("B-2", isOrderLine().with("quantity", equalTo(2))))
                        .checkingAllProperties();

        assertTrue(
                matcher.matches(
                        new Registry(Map.of("B-2", new OrderLine("B-2", 2)), new Contact[0])));
    }

    @Test
    void aNestedMatcherInsideItemsInOrderReportsItsIndexedPaths() throws IOException {
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

        golden.assertMessage(
                "items-in-order-indexed-paths",
                messageOf(new Booking("b-1", ADA, List.of(ADA, ADA)), matcher));
    }

    @Test
    void itemsInAnyOrderAssignsByValuesAndReportsTheUnaddressedPropertiesOfTheAssignedItem()
            throws IOException {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .ignoring("customer")
                        .with(
                                "contacts",
                                itemsInAnyOrder(
                                        isContact()
                                                .with("name", equalTo("Bob"))
                                                .checkingAllProperties(),
                                        isContact()
                                                .with("name", equalTo("Ada"))
                                                .ignoring("phone")
                                                .ignoring("address")
                                                .checkingAllProperties()))
                        .checkingAllProperties();

        golden.assertMessage(
                "items-in-any-order-assigned-item",
                messageOf(new Booking("b-1", ADA, List.of(ADA, BOB)), matcher));
    }

    @Test
    void anIncompleteItemMatcherIsNoMissingItemOfItsOwnInItemsInAnyOrder() throws IOException {
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .ignoring("customer")
                        .with(
                                "contacts",
                                itemsInAnyOrder(
                                        isContact()
                                                .with("name", equalTo("Ada"))
                                                .checkingAllProperties()))
                        .checkingAllProperties();

        golden.assertMessage(
                "items-in-any-order-incomplete-item",
                messageOf(new Booking("b-1", ADA, List.of(ADA)), matcher));
    }

    @Test
    void theClosestCandidateOfItemsInAnyOrderIsChosenByValuesAlone() throws IOException {
        // Both candidates are unused. Candidate 1 differs in two values, but has no address to
        // leave a property unaddressed; candidate 2 differs in one value and the address matcher
        // leaves its city unaddressed. Counting that line would make them tie and pick the first.
        final Matcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .ignoring("customer")
                        .with(
                                "contacts",
                                itemsInAnyOrder(
                                        isContact()
                                                .with("name", equalTo("Zed"))
                                                .ignoring("phone")
                                                .with(
                                                        "address",
                                                        isAddress().checkingAllProperties())))
                        .checkingAllProperties();

        golden.assertMessage(
                "items-in-any-order-closest",
                messageOf(
                        new Booking(
                                "b-1",
                                ADA,
                                List.of(
                                        new Contact("Ada", "1", null),
                                        new Contact("Ada", "2", PARIS))),
                        matcher));
    }

    @Test
    void aNestedMatcherInsideEntriesReportsItsKeyedPaths() throws IOException {
        final Matcher<Registry> matcher =
                isRegistry()
                        .ignoring("contacts")
                        .with(
                                "bySku",
                                entries(
                                        "A-1",
                                        isOrderLine().with("sku", equalTo("A-1")),
                                        "B-2",
                                        isOrderLine()
                                                .with("sku", equalTo("B-2"))
                                                .checkingAllProperties()))
                        .checkingAllProperties();

        golden.assertMessage(
                "entries-keyed-paths",
                messageOf(
                        new Registry(
                                Map.of(
                                        "A-1",
                                        new OrderLine("A-1", 1),
                                        "B-2",
                                        new OrderLine("B-2", 2)),
                                new Contact[0]),
                        matcher));
    }

    @Test
    void itemsInAnyOrderTreatsArraysLikeCollectionsForTheCompletenessCheck() throws IOException {
        final Matcher<Registry> matcher =
                isRegistry()
                        .ignoring("bySku")
                        .with(
                                "contacts",
                                itemsInAnyOrder(
                                        isContact()
                                                .with("name", equalTo("Bob"))
                                                .checkingAllProperties(),
                                        isContact()
                                                .with("name", equalTo("Ada"))
                                                .ignoring("phone")
                                                .ignoring("address")
                                                .checkingAllProperties()))
                        .checkingAllProperties();

        golden.assertMessage(
                "items-in-any-order-array",
                messageOf(new Registry(Map.of(), new Contact[] {ADA, BOB}), matcher));
    }

    @Test
    void theDescriptionOfCollectionMatchersCountsTheirChecksAndEndsWithTheSuffix() {
        final BeanPropertyMatcher<Booking> matcher =
                isBooking()
                        .with("id", equalTo("b-1"))
                        .with(
                                "contacts",
                                itemsInOrder(
                                        isContact().with("name", equalTo("Ada")),
                                        isContact().with("name", equalTo("Bob"))))
                        .checkingAllProperties()
                        .beanPropertyMatcher();

        assertEquals(
                "Booking with 3 property checks, every property checked or ignored",
                descriptionOf(matcher));
    }

    @Test
    void theDescriptionOfAnItemsInAnyOrderCountsOneCheckPerItemMatcher() {
        final BeanPropertyMatcher<Booking> matcher =
                isBooking()
                        .with(
                                "contacts",
                                itemsInAnyOrder(
                                        isContact().with("name", equalTo("Ada")).with("phone"),
                                        isContact().with("name", equalTo("Bob"))))
                        .checkingAllProperties()
                        .beanPropertyMatcher();

        assertEquals(
                "Booking with 2 property checks, every property checked or ignored",
                descriptionOf(matcher));
    }
}
