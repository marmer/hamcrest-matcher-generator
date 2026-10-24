package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.util.List;
import org.hamcrest.Matcher;
import proto.model.Customer;
import proto.model.Order;

/** PROTOTYPE. Hand-written stand-in for the generated matcher of {@link Order}. */
public final class OrderMatcher extends PropertyMatcher<Order, OrderMatcher> {

    private OrderMatcher() {
        super(Order.class);
        property("id", Order::getId);
        property("customer", Order::getCustomer);
        property("total", Order::getTotal);
        property("tags", Order::getTags);
    }

    public static OrderMatcher isOrder() { return new OrderMatcher(); }

    /** Reference object as decided in "Feature selection for 6.0.0": equalTo per property. */
    public static OrderMatcher isOrderLike(Order ref) {
        return isOrder().checkingAllProperties().withId(ref.getId()).withCustomer(ref.getCustomer())
                .withTotal(ref.getTotal()).withTags(ref.getTags());
    }

    /** Reference object, deep variant: nested types with a generated matcher recurse. */
    public static OrderMatcher isOrderLikeDeep(Order ref) {
        return isOrder().checkingAllProperties().withId(ref.getId())
                .withCustomer(ref.getCustomer() == null ? nullValue() : CustomerMatcher.isCustomerLikeDeep(ref.getCustomer()))
                .withTotal(ref.getTotal()).withTags(ref.getTags());
    }

    public OrderMatcher withId(long v) { return withId(equalTo(v)); }
    public OrderMatcher withId(Matcher<? super Long> m) { return check("id", m); }
    public OrderMatcher ignoringId() { return ignore("id"); }

    public OrderMatcher withCustomer(Customer v) { return withCustomer(equalTo(v)); }
    public OrderMatcher withCustomer(Matcher<? super Customer> m) { return check("customer", m); }
    public OrderMatcher ignoringCustomer() { return ignore("customer"); }

    public OrderMatcher withTotal(BigDecimal v) { return withTotal(equalTo(v)); }
    public OrderMatcher withTotal(Matcher<? super BigDecimal> m) { return check("total", m); }
    public OrderMatcher ignoringTotal() { return ignore("total"); }

    public OrderMatcher withTags(List<String> v) { return withTags(equalTo(v)); }
    public OrderMatcher withTags(Matcher<?> m) { return check("tags", m); }
    public OrderMatcher ignoringTags() { return ignore("tags"); }
}
