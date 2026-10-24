package proto.assertj;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import proto.model.*;

/** PROTOTYPE. Hand-written stand-in for the generated AssertJ assert of {@link OrderLine}. */
public class OrderLineAssert extends PropertyAssert<OrderLineAssert, OrderLine> {
    public OrderLineAssert(OrderLine actual) { super(actual, OrderLineAssert.class, "OrderLine"); }
    public static OrderLineAssert assertThat(OrderLine actual) { return new OrderLineAssert(actual); }
    public OrderLineAssert hasSku(String v) { return hasProperty("sku", actual == null ? null : actual.getSku(), v); }
    public OrderLineAssert hasSkuSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("sku", actual == null ? null : actual.getSku(), r); }
    public OrderLineAssert hasQuantity(Integer v) { return hasProperty("quantity", actual == null ? null : actual.getQuantity(), v); }
    public OrderLineAssert hasQuantitySatisfying(Consumer<? super Integer> r) { return hasPropertySatisfying("quantity", actual == null ? null : actual.getQuantity(), r); }
}
