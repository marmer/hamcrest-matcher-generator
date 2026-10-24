package proto.assertj;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import proto.model.*;

/** PROTOTYPE. Hand-written stand-in for the generated AssertJ assert of {@link Order}. */
public class OrderAssert extends PropertyAssert<OrderAssert, Order> {
    public OrderAssert(Order actual) { super(actual, OrderAssert.class, "Order"); }
    public static OrderAssert assertThat(Order actual) { return new OrderAssert(actual); }
    public OrderAssert hasId(Long v) { return hasProperty("id", actual == null ? null : actual.getId(), v); }
    public OrderAssert hasIdSatisfying(Consumer<? super Long> r) { return hasPropertySatisfying("id", actual == null ? null : actual.getId(), r); }
    public OrderAssert hasCustomer(Customer v) { return hasProperty("customer", actual == null ? null : actual.getCustomer(), v); }
    public OrderAssert hasCustomerSatisfying(Consumer<? super Customer> r) { return hasPropertySatisfying("customer", actual == null ? null : actual.getCustomer(), r); }
    public OrderAssert hasTotal(BigDecimal v) { return hasProperty("total", actual == null ? null : actual.getTotal(), v); }
    public OrderAssert hasTotalSatisfying(Consumer<? super BigDecimal> r) { return hasPropertySatisfying("total", actual == null ? null : actual.getTotal(), r); }
    public OrderAssert hasTags(List<String> v) { return hasProperty("tags", actual == null ? null : actual.getTags(), v); }
    public OrderAssert hasTagsSatisfying(Consumer<? super List<String>> r) { return hasPropertySatisfying("tags", actual == null ? null : actual.getTags(), r); }
}
