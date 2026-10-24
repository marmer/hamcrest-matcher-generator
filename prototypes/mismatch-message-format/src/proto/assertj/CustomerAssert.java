package proto.assertj;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import proto.model.*;

/** PROTOTYPE. Hand-written stand-in for the generated AssertJ assert of {@link Customer}. */
public class CustomerAssert extends PropertyAssert<CustomerAssert, Customer> {
    public CustomerAssert(Customer actual) { super(actual, CustomerAssert.class, "Customer"); }
    public static CustomerAssert assertThat(Customer actual) { return new CustomerAssert(actual); }
    public CustomerAssert hasName(String v) { return hasProperty("name", actual == null ? null : actual.getName(), v); }
    public CustomerAssert hasNameSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("name", actual == null ? null : actual.getName(), r); }
    public CustomerAssert hasPhone(String v) { return hasProperty("phone", actual == null ? null : actual.getPhone(), v); }
    public CustomerAssert hasPhoneSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("phone", actual == null ? null : actual.getPhone(), r); }
    public CustomerAssert hasAddress(Address v) { return hasProperty("address", actual == null ? null : actual.getAddress(), v); }
    public CustomerAssert hasAddressSatisfying(Consumer<? super Address> r) { return hasPropertySatisfying("address", actual == null ? null : actual.getAddress(), r); }
}
