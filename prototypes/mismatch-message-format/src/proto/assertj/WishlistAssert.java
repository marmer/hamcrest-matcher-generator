package proto.assertj;

import java.util.Set;
import java.util.function.Consumer;
import proto.model.*;

/** PROTOTYPE. Hand-written stand-in for the generated AssertJ assert of {@link Wishlist}. */
public class WishlistAssert extends PropertyAssert<WishlistAssert, Wishlist> {
    public WishlistAssert(Wishlist actual) { super(actual, WishlistAssert.class, "Wishlist"); }
    public static WishlistAssert assertThat(Wishlist actual) { return new WishlistAssert(actual); }
    public WishlistAssert hasOwner(String v) { return hasProperty("owner", actual == null ? null : actual.getOwner(), v); }
    public WishlistAssert hasOwnerSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("owner", actual == null ? null : actual.getOwner(), r); }
    public WishlistAssert hasItems(Set<OrderLine> v) { return hasProperty("items", actual == null ? null : actual.getItems(), v); }
    public WishlistAssert hasItemsSatisfying(Consumer<? super Set<OrderLine>> r) { return hasPropertySatisfying("items", actual == null ? null : actual.getItems(), r); }
}
