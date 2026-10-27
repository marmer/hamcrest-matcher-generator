/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jspecify.annotations.Nullable;

/**
 * Beans and hand-written stand-ins for generated matchers, shared by the runtime tests. The
 * stand-ins are wired exactly like generated code: they implement {@link GeneratedBeanMatcher}, so
 * nested ones are flattened into property paths.
 */
final class BeanFixtures {
    private BeanFixtures() {}

    static TestMatcher<Order> isOrder() {
        return new TestMatcher<>(
                Order.class,
                "id",
                "email",
                "customer",
                "lines",
                "bySku",
                "lineArray",
                "tags",
                "byColor",
                "byId",
                "byLine",
                "batches");
    }

    static TestMatcher<Customer> isCustomer() {
        return new TestMatcher<>(Customer.class, "name", "address");
    }

    static TestMatcher<Address> isAddress() {
        return new TestMatcher<>(Address.class, "city");
    }

    static TestMatcher<OrderLine> isOrderLine() {
        return new TestMatcher<>(OrderLine.class, "sku", "quantity");
    }

    static TestMatcher<Booking> isBooking() {
        return new TestMatcher<>(Booking.class, "id", "customer", "contacts");
    }

    static TestMatcher<Registry> isRegistry() {
        return new TestMatcher<>(Registry.class, "bySku", "contacts");
    }

    static TestMatcher<Contact> isContact() {
        return new TestMatcher<>(Contact.class, "name", "phone", "address");
    }

    /** Hand written stand-in for a generated matcher. */
    static final class TestMatcher<T> extends TypeSafeMatcher<T> implements GeneratedBeanMatcher {
        private final BeanPropertyMatcher<T> beanPropertyMatcher;

        private TestMatcher(final Class<T> type, final String... properties) {
            beanPropertyMatcher = new BeanPropertyMatcher<>(type, properties);
        }

        TestMatcher<T> with(final String property, final Matcher<?> matcher) {
            beanPropertyMatcher.with(property, matcher);
            return this;
        }

        TestMatcher<T> with(final String property) {
            beanPropertyMatcher.with(property);
            return this;
        }

        TestMatcher<T> ignoring(final String property) {
            beanPropertyMatcher.ignore(property);
            return this;
        }

        TestMatcher<T> checkingAllProperties() {
            beanPropertyMatcher.checkingAllProperties();
            return this;
        }

        @Override
        public BeanPropertyMatcher<T> beanPropertyMatcher() {
            return beanPropertyMatcher;
        }

        @Override
        public void describeTo(final Description description) {
            beanPropertyMatcher.describeTo(description);
        }

        @Override
        protected boolean matchesSafely(final T item) {
            return beanPropertyMatcher.matches(item);
        }

        @Override
        protected void describeMismatchSafely(final T item, final Description mismatchDescription) {
            beanPropertyMatcher.describeMismatch(item, mismatchDescription);
        }
    }

    @Value
    public static class Marked {
        String id;
        String other = "o";
    }

    public enum Color {
        RED,
        GREEN,
        BLUE
    }

    @Value
    @Builder
    public static class Order {
        @Builder.Default String id = "o-1";
        @Builder.Default String email = "ada@example.org";
        @Builder.Default @Nullable Customer customer = null;
        @Builder.Default List<OrderLine> lines = List.of();
        @Builder.Default Map<String, OrderLine> bySku = Map.of();
        @Builder.Default OrderLine[] lineArray = new OrderLine[0];
        @Builder.Default @Nullable List<String> tags = List.of();
        @Builder.Default Map<Color, OrderLine> byColor = Map.of();
        @Builder.Default Map<Integer, OrderLine> byId = Map.of();
        @Builder.Default Map<OrderLine, String> byLine = Map.of();
        @Builder.Default List<List<OrderLine>> batches = List.of();

        static Order of(
                final String id,
                final String email,
                final @Nullable Customer customer,
                final List<OrderLine> lines,
                final Map<String, OrderLine> bySku) {
            return builder()
                    .id(id)
                    .email(email)
                    .customer(customer)
                    .lines(lines)
                    .bySku(bySku)
                    .build();
        }
    }

    @Value
    public static class Customer {
        String name;
        @Nullable Address address;
    }

    @Value
    public static class Address {
        String city;
    }

    @Value
    public static class OrderLine {
        String sku;
        int quantity;
    }

    @Value
    public static class Booking {
        String id;
        @Nullable Contact customer;
        List<Contact> contacts;
    }

    @Value
    public static class Contact {
        String name;
        String phone;
        @Nullable Address address;
    }

    /** Small bean for the completeness tests of the collection matchers. */
    @Value
    public static class Registry {
        Map<String, OrderLine> bySku;
        Contact[] contacts;
    }
}
