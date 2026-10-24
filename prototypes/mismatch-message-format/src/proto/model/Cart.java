package proto.model;

import java.util.List;

/** PROTOTYPE sample: a nested list of POJOs, no toString. */
public class Cart {
    private final String owner;
    private final List<OrderLine> lines;

    public Cart(String owner, List<OrderLine> lines) { this.owner = owner; this.lines = lines; }

    public String getOwner() { return owner; }
    public List<OrderLine> getLines() { return lines; }
}
