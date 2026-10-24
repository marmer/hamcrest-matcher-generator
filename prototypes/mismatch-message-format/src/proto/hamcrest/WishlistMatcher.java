package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;

import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;
import proto.model.OrderLine;
import proto.model.Wishlist;

/** PROTOTYPE. Hand-written stand-in for the generated matcher of {@link Wishlist}. */
public final class WishlistMatcher extends PropertyMatcher<Wishlist, WishlistMatcher> {

    private WishlistMatcher() {
        super(Wishlist.class);
        property("owner", Wishlist::getOwner);
        property("items", Wishlist::getItems);
    }

    public static WishlistMatcher isWishlist() { return new WishlistMatcher(); }

    /** Reference object, shallow: equalTo per property (the set compared with equals). */
    public static WishlistMatcher isWishlistLike(Wishlist ref) {
        return isWishlist().checkingAllProperties().withOwner(ref.getOwner()).withItems(ref.getItems());
    }

    /** Reference object, deep: a non-List collection of a generated type is matched in any order. */
    public static WishlistMatcher isWishlistLikeDeep(Wishlist ref) {
        return isWishlist().checkingAllProperties().withOwner(ref.getOwner())
                .withItems(PropertyMatcher.itemsInAnyOrder(ref.getItems().stream()
                        .map(OrderLineMatcher::isOrderLineLike).collect(Collectors.toList())));
    }

    public WishlistMatcher withOwner(String v) { return withOwner(equalTo(v)); }
    public WishlistMatcher withOwner(Matcher<? super String> m) { return check("owner", m); }
    public WishlistMatcher ignoringOwner() { return ignore("owner"); }

    public WishlistMatcher withItems(Set<OrderLine> v) { return withItems(equalTo(v)); }
    public WishlistMatcher withItems(Matcher<?> m) { return check("items", m); }
    public WishlistMatcher ignoringItems() { return ignore("items"); }
}
