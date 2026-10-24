package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Consumer;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;
import proto.model.Cart;
import proto.model.OrderLine;

/** PROTOTYPE. Hand-written stand-in for the generated matcher of {@link Cart}. */
public final class CartMatcher extends PropertyMatcher<Cart, CartMatcher> {

    private CartMatcher() {
        super(Cart.class);
        property("owner", Cart::getOwner);
        property("lines", Cart::getLines);
    }

    public static CartMatcher isCart() { return new CartMatcher(); }

    /** K2 (Java-emitted lambda overload): {@code isCart(m -> m.withFoo(..))}. */
    public static CartMatcher isCart(Consumer<? super CartMatcher> spec) { CartMatcher m = isCart(); spec.accept(m); return m; }

    /** Reference object, shallow: equalTo per property (the list compared with equals). */
    public static CartMatcher isCartLike(Cart ref) {
        return isCart().checkingAllProperties().withOwner(ref.getOwner()).withLines(ref.getLines());
    }

    /** Reference object, deep: list elements of a generated type matched item by item, indexed paths. */
    public static CartMatcher isCartLikeDeep(Cart ref) {
        return isCart().checkingAllProperties().withOwner(ref.getOwner())
                .withLines(PropertyMatcher.itemsInOrder(ref.getLines().stream()
                        .map(OrderLineMatcher::isOrderLineLike).collect(Collectors.toList())));
    }

    public CartMatcher withOwner(@Nullable String v) { return withOwner(equalTo(v)); }
    public CartMatcher withOwner(Matcher<? super String> m) { return check("owner", m); }
    public CartMatcher ignoringOwner() { return ignore("owner"); }

    public CartMatcher withLines(@Nullable List<OrderLine> v) { return withLines(equalTo(v)); }
    public CartMatcher withLines(Matcher<?> m) { return check("lines", m); }
    public CartMatcher ignoringLines() { return ignore("lines"); }
}
