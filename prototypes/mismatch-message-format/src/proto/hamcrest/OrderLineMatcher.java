package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.Matcher;
import proto.model.OrderLine;

/** PROTOTYPE. Hand-written stand-in for the generated matcher of {@link OrderLine}. */
public final class OrderLineMatcher extends PropertyMatcher<OrderLine, OrderLineMatcher> {

    private OrderLineMatcher() {
        super(OrderLine.class);
        property("sku", OrderLine::getSku);
        property("quantity", OrderLine::getQuantity);
    }

    public static OrderLineMatcher isOrderLine() { return new OrderLineMatcher(); }

    public static OrderLineMatcher isOrderLineLike(OrderLine ref) {
        return isOrderLine().checkingAllProperties().withSku(ref.getSku()).withQuantity(ref.getQuantity());
    }

    public OrderLineMatcher withSku(String v) { return withSku(equalTo(v)); }
    public OrderLineMatcher withSku(Matcher<? super String> m) { return check("sku", m); }
    public OrderLineMatcher ignoringSku() { return ignore("sku"); }

    public OrderLineMatcher withQuantity(int v) { return withQuantity(equalTo(v)); }
    public OrderLineMatcher withQuantity(Matcher<? super Integer> m) { return check("quantity", m); }
    public OrderLineMatcher ignoringQuantity() { return ignore("quantity"); }
}
