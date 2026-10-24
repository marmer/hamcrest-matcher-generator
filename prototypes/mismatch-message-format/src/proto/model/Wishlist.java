package proto.model;

import java.util.Set;

/** PROTOTYPE sample: an unordered collection of POJOs, no toString. */
public class Wishlist {
    private final String owner;
    private final Set<OrderLine> items;

    public Wishlist(String owner, Set<OrderLine> items) { this.owner = owner; this.items = items; }

    public String getOwner() { return owner; }
    public Set<OrderLine> getItems() { return items; }
}
