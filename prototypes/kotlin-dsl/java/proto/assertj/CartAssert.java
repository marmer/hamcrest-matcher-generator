package proto.assertj;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import proto.model.*;

/** PROTOTYPE. Hand-written stand-in for the generated AssertJ assert of {@link Cart}. */
public class CartAssert extends PropertyAssert<CartAssert, Cart> {
    public CartAssert(Cart actual) { super(actual, CartAssert.class, "Cart"); }
    public static CartAssert assertThat(Cart actual) { return new CartAssert(actual); }
    public CartAssert hasOwner(String v) { return hasProperty("owner", actual == null ? null : actual.getOwner(), v); }
    public CartAssert hasOwnerSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("owner", actual == null ? null : actual.getOwner(), r); }
    public CartAssert hasLines(List<OrderLine> v) { return hasProperty("lines", actual == null ? null : actual.getLines(), v); }
    public CartAssert hasLinesSatisfying(Consumer<? super List<OrderLine>> r) { return hasPropertySatisfying("lines", actual == null ? null : actual.getLines(), r); }
}
