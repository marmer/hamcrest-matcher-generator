package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;

import java.util.LinkedHashMap;
import java.util.Map;
import org.hamcrest.Matcher;
import proto.model.Inventory;
import proto.model.OrderLine;

/** PROTOTYPE. Hand-written stand-in for the generated matcher of {@link Inventory}. */
public final class InventoryMatcher extends PropertyMatcher<Inventory, InventoryMatcher> {

    private InventoryMatcher() {
        super(Inventory.class);
        property("owner", Inventory::getOwner);
        property("bySku", Inventory::getBySku);
    }

    public static InventoryMatcher isInventory() { return new InventoryMatcher(); }

    /** Reference object, shallow: equalTo per property (the map compared with equals). */
    public static InventoryMatcher isInventoryLike(Inventory ref) {
        return isInventory().checkingAllProperties().withOwner(ref.getOwner()).withBySku(ref.getBySku());
    }

    /** Reference object, deep: a map with values of a generated type is matched key by key. */
    public static InventoryMatcher isInventoryLikeDeep(Inventory ref) {
        Map<String, Matcher<? super OrderLine>> values = new LinkedHashMap<>();
        ref.getBySku().forEach((k, v) -> values.put(k, OrderLineMatcher.isOrderLineLike(v)));
        return isInventory().checkingAllProperties().withOwner(ref.getOwner()).withBySku(PropertyMatcher.entries(values));
    }

    public InventoryMatcher withOwner(String v) { return withOwner(equalTo(v)); }
    public InventoryMatcher withOwner(Matcher<? super String> m) { return check("owner", m); }
    public InventoryMatcher ignoringOwner() { return ignore("owner"); }

    public InventoryMatcher withBySku(Map<String, OrderLine> v) { return withBySku(equalTo(v)); }
    public InventoryMatcher withBySku(Matcher<?> m) { return check("bySku", m); }
    public InventoryMatcher ignoringBySku() { return ignore("bySku"); }
}
