package proto.assertj;

import java.util.Map;
import java.util.function.Consumer;
import proto.model.*;

/** PROTOTYPE. Hand-written stand-in for the generated AssertJ assert of {@link Inventory}. */
public class InventoryAssert extends PropertyAssert<InventoryAssert, Inventory> {
    public InventoryAssert(Inventory actual) { super(actual, InventoryAssert.class, "Inventory"); }
    public static InventoryAssert assertThat(Inventory actual) { return new InventoryAssert(actual); }
    public InventoryAssert hasOwner(String v) { return hasProperty("owner", actual == null ? null : actual.getOwner(), v); }
    public InventoryAssert hasBySku(Map<String, OrderLine> v) { return hasProperty("bySku", actual == null ? null : actual.getBySku(), v); }
    public InventoryAssert hasBySkuSatisfying(Consumer<? super Map<String, OrderLine>> r) { return hasPropertySatisfying("bySku", actual == null ? null : actual.getBySku(), r); }
}
