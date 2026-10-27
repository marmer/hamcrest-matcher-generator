/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    static TestMatcher<Node> isNode() {
        return new TestMatcher<>(
                Node.class, "name", "parent", "children", "byName", "peers", "tags", "siblings");
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

    /** A bean that can be part of a cyclic graph: its children point back to it. */
    public static class Node {
        private final String name;
        private @Nullable Node parent;
        private final List<Node> children = new ArrayList<>();
        private final Map<String, Node> byName = new LinkedHashMap<>();
        private final Set<Node> peers = new LinkedHashSet<>();
        private final List<String> tags = new ArrayList<>();
        private Node[] siblings = new Node[0];

        public Node(final String name) {
            this.name = name;
        }

        /** Adds the child and points the child back to this node. */
        public Node withChild(final Node child) {
            child.parent = this;
            children.add(child);
            return this;
        }

        public Node withNamed(final String key, final Node node) {
            byName.put(key, node);
            return this;
        }

        public Node withPeer(final Node peer) {
            peers.add(peer);
            return this;
        }

        public Node withSiblings(final Node... newSiblings) {
            siblings = newSiblings;
            return this;
        }

        public Node withTag(final String tag) {
            tags.add(tag);
            return this;
        }

        public Node withParent(final @Nullable Node newParent) {
            parent = newParent;
            return this;
        }

        public String getName() {
            return name;
        }

        public @Nullable Node getParent() {
            return parent;
        }

        public List<Node> getChildren() {
            return children;
        }

        public Map<String, Node> getByName() {
            return byName;
        }

        public Set<Node> getPeers() {
            return peers;
        }

        public List<String> getTags() {
            return tags;
        }

        public Node[] getSiblings() {
            return siblings;
        }
    }

    /** Small bean for the completeness tests of the collection matchers. */
    @Value
    public static class Registry {
        Map<String, OrderLine> bySku;
        Contact[] contacts;
    }
}
