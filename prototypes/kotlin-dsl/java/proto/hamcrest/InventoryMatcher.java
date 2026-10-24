package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;
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

    /** K2 (Java-emitted lambda overload): {@code isInventory(m -> m.withFoo(..))}. */
    public static InventoryMatcher isInventory(Consumer<? super InventoryMatcher> spec) { InventoryMatcher m = isInventory(); spec.accept(m); return m; }

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

    public InventoryMatcher withOwner(@Nullable String v) { return withOwner(equalTo(v)); }
    public InventoryMatcher withOwner(Matcher<? super String> m) { return check("owner", m); }
    public InventoryMatcher ignoringOwner() { return ignore("owner"); }

    public InventoryMatcher withBySku(@Nullable Map<String, OrderLine> v) { return withBySku(equalTo(v)); }
    public InventoryMatcher withBySku(Matcher<?> m) { return check("bySku", m); }
    public InventoryMatcher ignoringBySku() { return ignore("bySku"); }
}
