package proto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static proto.hamcrest.AddressMatcher.isAddress;
import static proto.hamcrest.CustomerMatcher.isCustomer;
import static proto.hamcrest.OrderMatcher.isOrder;
import static proto.hamcrest.OrderMatcher.isOrderLike;
import static proto.hamcrest.OrderMatcher.isOrderLikeDeep;
import static proto.hamcrest.PersonMatcher.isPerson;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import proto.assertj.AddressAssert;
import proto.assertj.CustomerAssert;
import proto.assertj.OrderAssert;
import proto.assertj.PersonAssert;
import proto.assertj.PropertyAssert;
import proto.assertj.ProtoSoftAssertions;
import proto.hamcrest.PropertyMatcher;
import proto.hamcrest.PropertyMatcher.Format;
import proto.legacy.BeanPropertyMatcher;
import proto.model.Address;
import proto.model.Customer;
import proto.model.Order;
import proto.model.Person;

/**
 * PROTOTYPE. Prints what a test author sees for every scenario: the 5.x baseline (the real 5.x
 * runtime), the Hamcrest candidates A/B/C and the AssertJ variants J1/J2.
 */
public class Demo {

    static final Person ADA = new Person("Ada", 36, "ada@example.org", "+44 20 0000");
    static final Address PARIS = new Address("1 Rue de Rivoli", "Paris", "75001");
    static final Order ORDER = new Order(42, new Customer("Ada", "+44 20 0000", PARIS),
            new BigDecimal("9.99"), List.of("books", "sale"));
    static final Order ORDER_WITHOUT_CUSTOMER = new Order(43, null, new BigDecimal("9.99"), List.of());
    static final Order EXPECTED_ORDER = new Order(42,
            new Customer("Ada", "+44 20 0000", new Address("1 Rue de Rivoli", "Berlin", "75001")),
            new BigDecimal("10.00"), List.of("books", "sale"));

    public static void main(String[] args) {
        hamcrest("S1 flat POJO: two properties differ",
                () -> isPerson().withName("Bob").withAge(36).withEmail(startsWith("bob@")),
                () -> bean(Person.class).with("name", equalTo("Bob")).with("age", equalTo(36))
                        .with("email", startsWith("bob@")),
                ADA);

        hamcrest("S2 flat POJO + completeness check: one value differs, phone unaddressed",
                () -> isPerson().checkingAllProperties().withName("Bob").withAge(36).ignoringEmail(),
                null, ADA);

        hamcrest("S3 record: two components differ",
                () -> isAddress().withCity("Berlin").withZip("10115"),
                () -> bean(Address.class).with("city", equalTo("Berlin")).with("zip", equalTo("10115")),
                PARIS);

        hamcrest("S4 nested: Order -> Customer -> Address (record), two leaves differ",
                () -> isOrder().withId(42L).withTotal(new BigDecimal("10.00"))
                        .withCustomer(isCustomer().withName("Ada").withAddress(isAddress().withCity("Berlin"))),
                () -> bean(Order.class).with("id", equalTo(42L)).with("total", equalTo(new BigDecimal("10.00")))
                        .with("customer", bean(Customer.class).with("name", equalTo("Ada"))
                                .with("address", bean(Address.class).with("city", equalTo("Berlin")))),
                ORDER);

        hamcrest("S5 nested value is null",
                () -> isOrder().withCustomer(isCustomer().withName("Ada").withAddress(isAddress().withCity("Paris"))),
                () -> bean(Order.class).with("customer", bean(Customer.class).with("name", equalTo("Ada"))
                        .with("address", bean(Address.class).with("city", equalTo("Paris")))),
                ORDER_WITHOUT_CUSTOMER);

        hamcrest("S6 collection property with a plain Hamcrest matcher",
                () -> isOrder().withId(42L).withTags(hasItem("new")),
                () -> bean(Order.class).with("id", equalTo(42L)).with("tags", hasItem("new")),
                ORDER);

        hamcrest("S7 nested + completeness check on both levels",
                () -> isOrder().checkingAllProperties().withId(42L).ignoringTotal().ignoringTags()
                        .withCustomer(isCustomer().checkingAllProperties().withName("Ada")),
                null, ORDER);

        hamcrest("S8 reference object, as decided: isOrderLike(ref) = equalTo per property",
                () -> isOrderLike(EXPECTED_ORDER), null, ORDER);

        hamcrest("S9 reference object, deep variant: nested generated matchers recurse",
                () -> isOrderLikeDeep(EXPECTED_ORDER), null, ORDER);

        assertj("S1 flat POJO: two properties differ (AssertJ is eager: first failure only)",
                () -> PersonAssert.assertThat(ADA).hasName("Bob").hasAge(36).hasEmailSatisfying(
                        e -> Assertions.assertThat(e).startsWith("bob@")));

        assertj("S3 record: two components differ",
                () -> AddressAssert.assertThat(PARIS).hasCity("Berlin").hasZip("10115"));

        assertj("S4 nested: Order -> Customer -> Address (record)",
                () -> OrderAssert.assertThat(ORDER).hasId(42L).hasCustomerSatisfying(c -> CustomerAssert.assertThat(c)
                        .hasName("Ada").hasAddressSatisfying(a -> AddressAssert.assertThat(a).hasCity("Berlin"))));

        assertj("S5 nested value is null",
                () -> OrderAssert.assertThat(ORDER_WITHOUT_CUSTOMER).hasCustomerSatisfying(
                        c -> CustomerAssert.assertThat(c).hasName("Ada")));

        assertj("S6 collection property via AssertJ's own assertThat inside the lambda",
                () -> OrderAssert.assertThat(ORDER).hasTagsSatisfying(t -> Assertions.assertThat(t).contains("new")));

        assertj("S10 soft assertions: all failures collected",
                () -> {
                    ProtoSoftAssertions softly = new ProtoSoftAssertions();
                    softly.assertThat(ADA).hasName("Bob").hasAge(37).hasEmail("ada@example.org");
                    softly.assertAll();
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> BeanPropertyMatcher<T> bean(Class<T> type) { return new BeanPropertyMatcher(type); }

    static void hamcrest(String title, Supplier<Matcher<?>> candidate, Supplier<Matcher<?>> legacy, Object actual) {
        System.out.println("##### HAMCREST " + title);
        if (legacy != null) run("5.x today", () -> assertThat(actual, (Matcher<Object>) legacy.get()));
        else System.out.println("=== 5.x today\n(not expressible in 5.x)\n");
        for (Format f : Format.values()) {
            PropertyMatcher.format = f;
            run(f.name(), () -> assertThat(actual, (Matcher<Object>) candidate.get()));
        }
    }

    static void assertj(String title, Runnable test) {
        System.out.println("##### ASSERTJ " + title);
        PropertyAssert.withPath = false;
        run("J1 native, property name", test);
        PropertyAssert.withPath = true;
        run("J2 native, path from root", test);
    }

    static void run(String variant, Runnable test) {
        System.out.println("=== " + variant);
        try {
            test.run();
            System.out.println("PASSED\n");
        } catch (AssertionError e) {
            System.out.println(e.getMessage().strip() + "\n");
        }
    }
}
