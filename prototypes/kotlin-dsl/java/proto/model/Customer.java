package proto.model;

/** PROTOTYPE sample: nested POJO, no toString. */
public class Customer {
    private final String name;
    private final String phone;
    private final Address address;

    public Customer(String name, String phone, Address address) {
        this.name = name; this.phone = phone; this.address = address;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public Address getAddress() { return address; }
}
