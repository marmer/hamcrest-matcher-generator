package proto.model;

import java.math.BigDecimal;
import java.util.List;

/** PROTOTYPE sample: root of a nested POJO graph, no toString. */
public class Order {
    private final long id;
    private final Customer customer;
    private final BigDecimal total;
    private final List<String> tags;

    public Order(long id, Customer customer, BigDecimal total, List<String> tags) {
        this.id = id; this.customer = customer; this.total = total; this.tags = tags;
    }

    public long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public BigDecimal getTotal() { return total; }
    public List<String> getTags() { return tags; }
}
