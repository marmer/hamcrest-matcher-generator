package proto.model;

/** PROTOTYPE sample: list element POJO, no toString. */
public class OrderLine {
    private final String sku;
    private final int quantity;

    public OrderLine(String sku, int quantity) { this.sku = sku; this.quantity = quantity; }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
}
