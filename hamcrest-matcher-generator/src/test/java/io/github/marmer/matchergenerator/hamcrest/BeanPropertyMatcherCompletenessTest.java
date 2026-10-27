/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcherMessageTest.Address;
import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcherMessageTest.Customer;
import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcherMessageTest.Order;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;

/** The completeness check of the runtime matcher: opt-in per matcher, off by default. */
class BeanPropertyMatcherCompletenessTest {
    private static final Address PARIS = new Address("Paris");

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
        final BeanPropertyMatcher<Customer> matcher =
                new BeanPropertyMatcher<>(Customer.class, "name", "address")
                        .with("name", equalTo("Ada"))
                        .with("address", nested(new BeanPropertyMatcher<>(Address.class, "city")))
                        .checkingAllProperties();

        assertTrue(matcher.matches(new Customer("Ada", PARIS)));
    }

    @Test
    void aNestedMatcherWithTheOptInReportsItsPathOnTheLastLine() {
        final BeanPropertyMatcher<Order> matcher =
                new BeanPropertyMatcher<>(Order.class, "id", "customer")
                        .with("id", equalTo("1"))
                        .with(
                                "customer",
                                nested(
                                        new BeanPropertyMatcher<>(Customer.class, "name", "address")
                                                .with("name", equalTo("Ada"))
                                                .with(
                                                        "address",
                                                        nested(
                                                                new BeanPropertyMatcher<>(
                                                                                Address.class,
                                                                                "city")
                                                                        .checkingAllProperties()))));

        final Order order = new Order("1", "a@b", new Customer("Ada", PARIS), null, null);
        assertFalse(matcher.matches(order));
        assertEquals("neither checked nor ignored: customer.address.city", mismatchOf(matcher, order));
    }

    /** A generated matcher as seen by the runtime: a Hamcrest matcher that delegates. */
    private static Matcher<Object> nested(final BeanPropertyMatcher<?> inner) {
        return new DelegatingGeneratedMatcher(inner);
    }

    private static final class DelegatingGeneratedMatcher extends TypeSafeMatcher<Object>
            implements GeneratedBeanMatcher {
        private final BeanPropertyMatcher<?> delegate;

        private DelegatingGeneratedMatcher(final BeanPropertyMatcher<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public BeanPropertyMatcher<?> beanPropertyMatcher() {
            return delegate;
        }

        @Override
        protected boolean matchesSafely(final Object item) {
            return delegate.matches(item);
        }

        @Override
        public void describeTo(final Description description) {
            delegate.describeTo(description);
        }

        @Override
        protected void describeMismatchSafely(final Object item, final Description description) {
            delegate.describeMismatch(item, description);
        }
    }
}
