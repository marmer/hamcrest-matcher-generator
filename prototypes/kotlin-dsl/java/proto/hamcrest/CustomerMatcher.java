package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.function.Consumer;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;
import proto.model.Address;
import proto.model.Customer;

/** PROTOTYPE. Hand-written stand-in for the generated matcher of {@link Customer}. */
public final class CustomerMatcher extends PropertyMatcher<Customer, CustomerMatcher> {

    private CustomerMatcher() {
        super(Customer.class);
        property("name", Customer::getName);
        property("phone", Customer::getPhone);
        property("address", Customer::getAddress);
    }

    public static CustomerMatcher isCustomer() { return new CustomerMatcher(); }

    /** K2 (Java-emitted lambda overload): {@code isCustomer(m -> m.withFoo(..))}. */
    public static CustomerMatcher isCustomer(Consumer<? super CustomerMatcher> spec) { CustomerMatcher m = isCustomer(); spec.accept(m); return m; }

    /** Reference object, deep variant: nested types with a generated matcher recurse. */
    public static CustomerMatcher isCustomerLikeDeep(Customer ref) {
        return isCustomer().checkingAllProperties().withName(ref.getName()).withPhone(ref.getPhone())
                .withAddress(ref.getAddress() == null ? nullValue() : AddressMatcher.isAddressLike(ref.getAddress()));
    }

    public CustomerMatcher withName(@Nullable String v) { return withName(equalTo(v)); }
    public CustomerMatcher withName(Matcher<? super String> m) { return check("name", m); }
    public CustomerMatcher ignoringName() { return ignore("name"); }

    public CustomerMatcher withPhone(@Nullable String v) { return withPhone(equalTo(v)); }
    public CustomerMatcher withPhone(Matcher<? super String> m) { return check("phone", m); }
    public CustomerMatcher ignoringPhone() { return ignore("phone"); }

    public CustomerMatcher withAddress(@Nullable Address v) { return withAddress(equalTo(v)); }
    public CustomerMatcher withAddress(Matcher<? super Address> m) { return check("address", m); }
    /** K2: nested generated type, lambda overload. */
    public CustomerMatcher withAddress(Consumer<? super AddressMatcher> spec) { return withAddress(AddressMatcher.isAddress(spec)); }
    public CustomerMatcher ignoringAddress() { return ignore("address"); }
}
