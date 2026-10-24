package proto.model;

import java.util.Map;

/** PROTOTYPE sample: a map from key to POJO, no toString. */
public class Inventory {
    private final String owner;
    private final Map<String, OrderLine> bySku;

    public Inventory(String owner, Map<String, OrderLine> bySku) { this.owner = owner; this.bySku = bySku; }

    public String getOwner() { return owner; }
    public Map<String, OrderLine> getBySku() { return bySku; }
}
