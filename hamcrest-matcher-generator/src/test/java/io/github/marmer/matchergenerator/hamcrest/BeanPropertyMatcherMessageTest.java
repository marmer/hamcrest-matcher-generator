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
import static io.github.marmer.matchergenerator.hamcrest.GoldenMessages.messageOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Address;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Booking;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Contact;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Customer;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Marked;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Order;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.OrderLine;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.TestMatcher;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

/** Mismatch messages are compared with golden files under {@code BeanPropertyMatcherTest/}. */
class BeanPropertyMatcherMessageTest {
    private static final Address BERLIN = new Address("Berlin");
    private static final Address PARIS = new Address("Paris");
    private static final OrderLine LINE_1 = new OrderLine("A-1", 1);
    private static final OrderLine LINE_2 = new OrderLine("B-2", 2);

    private final GoldenMessages golden = new GoldenMessages("BeanPropertyMatcherTest");

    @Test
    void flatMismatchListsOnlyTheFailingChecks() throws IOException {
        final Order order =
                Order.of(
                        "o-1", "ada@example.org", new Customer("Ada", BERLIN), List.of(), Map.of());

        golden.assertMessage(
                "flat",
                messageOf(
                        order,
                        isOrder()
                                .with("id", equalTo("o-1"))
                                .with("email", startsWith("bob@"))
                                .with("id", equalTo("o-2"))));
    }

    @Test
    void nestedGeneratedMatchersAreFlattenedIntoDotPaths() throws IOException {
        final Order order =
                Order.of("o-1", "ada@example.org", new Customer("Ada", PARIS), List.of(), Map.of());

        golden.assertMessage(
                "nested",
                messageOf(
                        order,
                        isOrder()
                                .with("email", startsWith("bob@"))
                                .with(
                                        "customer",
                                        isCustomer()
                                                .with("name", equalTo("Ada"))
                                                .with(
                                                        "address",
                                                        isAddress()
                                                                .with(
                                                                        "city",
                                                                        equalTo("Berlin"))))));
    }

    @Test
    void nullInsteadOfANestedObjectIsOneFailureWithTheWholeExpectation() throws IOException {
        final Order order = Order.of("o-1", "ada@example.org", null, List.of(), Map.of());

        golden.assertMessage(
                "null-nested",
                messageOf(
                        order,
                        isOrder()
                                .with(
                                        "customer",
                                        isCustomer()
                                                .with("name", equalTo("Ada"))
                                                .with(
                                                        "address",
                                                        isAddress()
                                                                .with("city", equalTo("Paris"))))));
    }

    @Test
    void nonEqualityMatchersAreDescribedByTheMatcher() throws IOException {
        final Order order =
                Order.of("o-1", "ada@example.org", new Customer("Ada", PARIS), List.of(), Map.of());

        golden.assertMessage(
                "non-equality",
                messageOf(
                        order,
                        isOrder()
                                .with("email", startsWith("bob@"))
                                .with("customer", nullValue())
                                .with("id", not(equalTo("o-1")))));
    }

    @Test
    void aPropertyTheTypeDoesNotHaveIsReported() throws IOException {
        golden.assertMessage(
                "missing-property",
                messageOf(
                        new Address("Berlin"),
                        isAddress().with("zip").with("city", equalTo("Berlin"))));
    }

    @Test
    void anObjectOfAnotherTypeIsReported() throws IOException {
        golden.assertMessage(
                "wrong-type",
                messageOf(new Customer("Ada", BERLIN), isAddress().with("name", equalTo("Ada"))));
    }

    @Test
    void valuesOfGeneratedTypesPrintFromTheirPropertiesNotViaToString() throws IOException {
        isOrderLine(); // a generated type is known to print from its properties once its matcher
        // exists
        final Order order = Order.of("o-1", "ada@example.org", null, List.of(LINE_1), Map.of());

        golden.assertMessage(
                "generated-values",
                messageOf(
                        order,
                        isOrder()
                                .with("lines", equalTo(List.of(LINE_2)))
                                .with("customer", equalTo(new Customer("Ada", BERLIN)))));
    }

    @Test
    void generatedMatchersInsideContainsRenderOnOneLine() throws IOException {
        final Order order =
                Order.of("o-1", "ada@example.org", null, List.of(LINE_1, LINE_2), Map.of());

        golden.assertMessage(
                "inside-contains",
                messageOf(
                        order,
                        isOrder()
                                .with(
                                        "lines",
                                        contains(
                                                isOrderLine()
                                                        .with("sku", equalTo("A-1"))
                                                        .with("quantity", equalTo(1)),
                                                isOrderLine()
                                                        .with("sku", equalTo("B-2"))
                                                        .with("quantity", equalTo(3))))));
    }

    @Test
    void generatedMatchersInsideHasItemRenderOnOneLine() throws IOException {
        final Order order =
                Order.of("o-1", "ada@example.org", null, List.of(LINE_1, LINE_2), Map.of());

        golden.assertMessage(
                "inside-has-item",
                messageOf(
                        order,
                        isOrder()
                                .with(
                                        "lines",
                                        hasItem(
                                                isOrderLine()
                                                        .with("sku", equalTo("X-9"))
                                                        .with("quantity", equalTo(3))))));
    }

    @Test
    void generatedMatchersInsideHasEntryRenderOnOneLine() throws IOException {
        final Order order =
                Order.of("o-1", "ada@example.org", null, List.of(), Map.of("bySku", LINE_2));

        golden.assertMessage(
                "inside-has-entry",
                messageOf(
                        order,
                        isOrder()
                                .with(
                                        "bySku",
                                        hasEntry(
                                                equalTo("bySku"),
                                                isOrderLine()
                                                        .with("sku", equalTo("X-9"))
                                                        .with("quantity", equalTo(3))))));
    }

    private static TestMatcher<Order> failingOrderMatcher() {
        return isOrder().with("id", equalTo("o-2")).with("email", startsWith("bob@"));
    }

    private static Order someOrder() {
        return Order.of("o-1", "ada@example.org", null, List.of(), Map.of());
    }

    @Test
    void aTopLevelGeneratedMatcherKeepsTheBlockFormatInsideIs() throws IOException {
        golden.assertMessage("wrapped-in-is", messageOf(someOrder(), is(failingOrderMatcher())));
    }

    @Test
    void aTopLevelGeneratedMatcherKeepsTheBlockFormatInsideAllOf() throws IOException {
        golden.assertMessage(
                "wrapped-in-all-of",
                messageOf(someOrder(), allOf(failingOrderMatcher(), notNullValue())));
    }

    @Test
    void aTopLevelGeneratedMatcherKeepsTheBlockFormatInsideAUserMatcher() throws IOException {
        golden.assertMessage(
                "wrapped-in-user-matcher",
                messageOf(someOrder(), new DelegatingMatcher(failingOrderMatcher())));
    }

    /**
     * A foreign matcher that holds the generated matcher is called by Hamcrest, not by a generated
     * matcher, so the runtime cannot know it is inside a foreign matcher and keeps the block
     * format.
     */
    @Test
    void aGeneratedMatcherInsideAForeignMatcherAtTheTopLevelKeepsTheBlockFormat()
            throws IOException {
        golden.assertMessage(
                "inside-contains-top-level",
                messageOf(
                        List.of(LINE_1, LINE_2),
                        contains(
                                isOrderLine()
                                        .with("sku", equalTo("A-1"))
                                        .with("quantity", equalTo(1)),
                                isOrderLine()
                                        .with("sku", equalTo("B-2"))
                                        .with("quantity", equalTo(3)))));
    }

    @Test
    void propertiesEveryObjectHasAreNotPrinted() {
        final Marked value = new Marked("x");

        new BeanPropertyMatcher<>(Marked.class, "id", "class");

        assertEquals("Marked {id \"x\"}", PropertyDescription.describeValue(value));
    }

    @Test
    void severalMatchersOfOneTypeDoNotChangeHowItsValuesPrint() {
        final Marked value = new Marked("x");

        new BeanPropertyMatcher<>(Marked.class, "id");
        new BeanPropertyMatcher<>(Marked.class, "id", "other");

        assertEquals("Marked {id \"x\"}", PropertyDescription.describeValue(value));
    }

    @Test
    void checksOfAResetPropertyAreNotReported() {
        final BeanPropertyMatcher<Address> matcher =
                new BeanPropertyMatcher<>(Address.class).with("city", equalTo("Paris"));
        matcher.reset("city");

        assertThat(BERLIN, matcher);
    }

    @Test
    void aNestedGeneratedMatcherWithoutChecksIsDescribedByItsTypeAlone() {
        final String message =
                messageOf(new Customer("Ada", null), isCustomer().with("address", isAddress()));

        assertThat(message, containsString("address: expected an Address but was null"));
    }

    @Test
    void aNestedGeneratedMatcherWithChecksIsDescribedWithThem() {
        final String message =
                messageOf(
                        new Customer("Ada", null),
                        isCustomer().with("address", isAddress().with("city", equalTo("Berlin"))));

        assertThat(
                message,
                containsString("address: expected an Address with city \"Berlin\" but was null"));
    }

    @Test
    void aPropertyWithAnEmptyNameIsReportedWithoutPath() {
        final String message = messageOf(BERLIN, isAddress().with(""));

        assertThat(
                message,
                containsString(
                        "but: expected the property to exist but Address has no property \"\""));
    }

    @Test
    void theCompletenessLineIsTheLastLineOfTheMismatch() throws IOException {
        final Booking booking = new Booking("b-1", new Contact("Ada", "0170-1", BERLIN), List.of());

        golden.assertMessage(
                "completeness-check",
                messageOf(
                        booking,
                        isBooking()
                                .with("id", equalTo("b-2"))
                                .with(
                                        "customer",
                                        isContact()
                                                .with("name", equalTo("Ada"))
                                                .checkingAllProperties())
                                .ignoring("contacts")
                                .checkingAllProperties()));
    }

    @Test
    void theCompletenessLineInsideAHamcrestMatcherRendersOnOneLine() throws IOException {
        final Contact ada = new Contact("Ada", "0170-1", BERLIN);
        final Booking booking = new Booking("b-1", ada, List.of(ada));

        golden.assertMessage(
                "completeness-inside-has-item",
                messageOf(
                        booking,
                        isBooking()
                                .with(
                                        "contacts",
                                        hasItem(
                                                isContact()
                                                        .with("name", equalTo("Ada"))
                                                        .checkingAllProperties()))));
    }

    private static final class DelegatingMatcher extends BaseMatcher<Object> {
        private final Matcher<?> delegate;

        private DelegatingMatcher(final Matcher<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean matches(final Object actual) {
            return delegate.matches(actual);
        }

        @Override
        public void describeTo(final Description description) {
            delegate.describeTo(description);
        }

        @Override
        public void describeMismatch(final Object actual, final Description description) {
            delegate.describeMismatch(actual, description);
        }
    }
}
