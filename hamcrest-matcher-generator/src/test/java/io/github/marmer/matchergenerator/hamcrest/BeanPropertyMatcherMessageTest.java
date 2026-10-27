package io.github.marmer.matchergenerator.hamcrest;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Value;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Mismatch messages are compared with golden files under {@code BeanPropertyMatcherTest/}.
 */
class BeanPropertyMatcherMessageTest {
    private static final Address BERLIN = new Address("Berlin");
    private static final Address PARIS = new Address("Paris");
    private static final OrderLine LINE_1 = new OrderLine("A-1", 1);
    private static final OrderLine LINE_2 = new OrderLine("B-2", 2);

    private static String messageOf(final Object actual, final Matcher<?> matcher) {
        return Objects.requireNonNull(
            assertThrows(AssertionError.class, () -> assertThat(actual, (Matcher<Object>) matcher)).getMessage());
    }

    private static void assertMessage(final String scenario, final String actualMessage) throws IOException {
        try (InputStream in = BeanPropertyMatcherMessageTest.class
            .getResourceAsStream("BeanPropertyMatcherTest/" + scenario + ".txt")) {
            final String expected = in == null ? "<missing golden file>"
                : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(expected, actualMessage + "\n");
        }
    }

    @Test
    void flatMismatchListsOnlyTheFailingChecks() throws IOException {
        final Order order = new Order("o-1", "ada@example.org", new Customer("Ada", BERLIN), List.of(), Map.of());

        assertMessage("flat", messageOf(order, isOrder()
            .with("id", equalTo("o-1"))
            .with("email", startsWith("bob@"))
            .with("id", equalTo("o-2"))));
    }

    @Test
    void nestedGeneratedMatchersAreFlattenedIntoDotPaths() throws IOException {
        final Order order = new Order("o-1", "ada@example.org", new Customer("Ada", PARIS), List.of(), Map.of());

        assertMessage("nested", messageOf(order, isOrder()
            .with("email", startsWith("bob@"))
            .with("customer", isCustomer()
                .with("name", equalTo("Ada"))
                .with("address", isAddress().with("city", equalTo("Berlin"))))));
    }

    @Test
    void nullInsteadOfANestedObjectIsOneFailureWithTheWholeExpectation() throws IOException {
        final Order order = new Order("o-1", "ada@example.org", null, List.of(), Map.of());

        assertMessage("null-nested", messageOf(order, isOrder()
            .with("customer", isCustomer()
                .with("name", equalTo("Ada"))
                .with("address", isAddress().with("city", equalTo("Paris"))))));
    }

    @Test
    void nonEqualityMatchersAreDescribedByTheMatcher() throws IOException {
        final Order order = new Order("o-1", "ada@example.org", new Customer("Ada", PARIS), List.of(), Map.of());

        assertMessage("non-equality", messageOf(order, isOrder()
            .with("email", startsWith("bob@"))
            .with("customer", nullValue())
            .with("id", not(equalTo("o-1")))));
    }

    @Test
    void aPropertyTheTypeDoesNotHaveIsReported() throws IOException {
        assertMessage("missing-property", messageOf(new Address("Berlin"), isAddress()
            .with("zip")
            .with("city", equalTo("Berlin"))));
    }

    @Test
    void anObjectOfAnotherTypeIsReported() throws IOException {
        assertMessage("wrong-type", messageOf(new Customer("Ada", BERLIN), isAddress()
            .with("name", equalTo("Ada"))));
    }

    @Test
    void valuesOfGeneratedTypesPrintFromTheirPropertiesNotViaToString() throws IOException {
        isOrderLine(); // a generated type is known to print from its properties once its matcher exists
        final Order order = new Order("o-1", "ada@example.org", null, List.of(LINE_1), Map.of());

        assertMessage("generated-values", messageOf(order, isOrder()
            .with("lines", equalTo(List.of(LINE_2)))
            .with("customer", equalTo(new Customer("Ada", BERLIN)))));
    }

    @Test
    void generatedMatchersInsideContainsRenderOnOneLine() throws IOException {
        final Order order = new Order("o-1", "ada@example.org", null, List.of(LINE_1, LINE_2), Map.of());

        assertMessage("inside-contains", messageOf(order, isOrder()
            .with("lines", contains(
                isOrderLine().with("sku", equalTo("A-1")).with("quantity", equalTo(1)),
                isOrderLine().with("sku", equalTo("B-2")).with("quantity", equalTo(3))))));
    }

    @Test
    void generatedMatchersInsideHasItemRenderOnOneLine() throws IOException {
        final Order order = new Order("o-1", "ada@example.org", null, List.of(LINE_1, LINE_2), Map.of());

        assertMessage("inside-has-item", messageOf(order, isOrder()
            .with("lines", hasItem(isOrderLine().with("sku", equalTo("X-9")).with("quantity", equalTo(3))))));
    }

    @Test
    void generatedMatchersInsideHasEntryRenderOnOneLine() throws IOException {
        final Order order = new Order("o-1", "ada@example.org", null, List.of(), Map.of("bySku", LINE_2));

        assertMessage("inside-has-entry", messageOf(order, isOrder()
            .with("linesBySku", hasEntry(equalTo("bySku"),
                isOrderLine().with("sku", equalTo("X-9")).with("quantity", equalTo(3))))));
    }

    private static TestMatcher<Order> failingOrderMatcher() {
        return isOrder()
            .with("id", equalTo("o-2"))
            .with("email", startsWith("bob@"));
    }

    private static Order someOrder() {
        return new Order("o-1", "ada@example.org", null, List.of(), Map.of());
    }

    @Test
    void aTopLevelGeneratedMatcherKeepsTheBlockFormatInsideIs() throws IOException {
        assertMessage("wrapped-in-is", messageOf(someOrder(), is(failingOrderMatcher())));
    }

    @Test
    void aTopLevelGeneratedMatcherKeepsTheBlockFormatInsideAllOf() throws IOException {
        assertMessage("wrapped-in-all-of", messageOf(someOrder(), allOf(failingOrderMatcher(), notNullValue())));
    }

    @Test
    void aTopLevelGeneratedMatcherKeepsTheBlockFormatInsideAUserMatcher() throws IOException {
        assertMessage("wrapped-in-user-matcher", messageOf(someOrder(), new DelegatingMatcher(failingOrderMatcher())));
    }

    /**
     * A foreign matcher that holds the generated matcher is called by Hamcrest, not by a generated matcher, so the
     * runtime cannot know it is inside a foreign matcher and keeps the block format.
     */
    @Test
    void aGeneratedMatcherInsideAForeignMatcherAtTheTopLevelKeepsTheBlockFormat() throws IOException {
        assertMessage("inside-contains-top-level", messageOf(List.of(LINE_1, LINE_2), contains(
            isOrderLine().with("sku", equalTo("A-1")).with("quantity", equalTo(1)),
            isOrderLine().with("sku", equalTo("B-2")).with("quantity", equalTo(3)))));
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
        final BeanPropertyMatcher<Address> matcher = new BeanPropertyMatcher<>(Address.class)
            .with("city", equalTo("Paris"));
        matcher.reset("city");

        assertThat(BERLIN, matcher);
    }

    @Test
    void aNestedGeneratedMatcherWithoutChecksIsDescribedByItsTypeAlone() {
        final String message = messageOf(new Customer("Ada", null), isCustomer().with("address", isAddress()));

        assertThat(message, containsString("address: expected an Address but was null"));
    }

    @Test
    void aNestedGeneratedMatcherWithChecksIsDescribedWithThem() {
        final String message = messageOf(new Customer("Ada", null),
            isCustomer().with("address", isAddress().with("city", equalTo("Berlin"))));

        assertThat(message, containsString("address: expected an Address with city \"Berlin\" but was null"));
    }

    @Test
    void aPropertyWithAnEmptyNameIsReportedWithoutPath() {
        final String message = messageOf(BERLIN, isAddress().with(""));

        assertThat(message, containsString("but: expected the property to exist but Address has no property \"\""));
    }

    private static TestMatcher<Order> isOrder() {
        return new TestMatcher<>(Order.class, "id", "email", "customer", "lines", "linesBySku");
    }

    private static TestMatcher<Customer> isCustomer() {
        return new TestMatcher<>(Customer.class, "name", "address");
    }

    private static TestMatcher<Address> isAddress() {
        return new TestMatcher<>(Address.class, "city");
    }

    private static TestMatcher<OrderLine> isOrderLine() {
        return new TestMatcher<>(OrderLine.class, "sku", "quantity");
    }

    /**
     * Hand written stand-in for a generated matcher.
     */
    private static final class TestMatcher<T> extends TypeSafeMatcher<T> implements GeneratedBeanMatcher {
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

    @Value
    public static class Marked {
        String id;
        String other = "o";
    }

    @Value
    public static class Order {
        String id;
        String email;
        @Nullable Customer customer;
        List<OrderLine> lines;
        Map<String, OrderLine> linesBySku;
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
}
