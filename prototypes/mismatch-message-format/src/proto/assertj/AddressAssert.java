package proto.assertj;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import proto.model.*;

/** PROTOTYPE. Hand-written stand-in for the generated AssertJ assert of {@link Address}. */
public class AddressAssert extends PropertyAssert<AddressAssert, Address> {
    public AddressAssert(Address actual) { super(actual, AddressAssert.class, "Address"); }
    public static AddressAssert assertThat(Address actual) { return new AddressAssert(actual); }
    public AddressAssert hasStreet(String v) { return hasProperty("street", actual == null ? null : actual.street(), v); }
    public AddressAssert hasStreetSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("street", actual == null ? null : actual.street(), r); }
    public AddressAssert hasCity(String v) { return hasProperty("city", actual == null ? null : actual.city(), v); }
    public AddressAssert hasCitySatisfying(Consumer<? super String> r) { return hasPropertySatisfying("city", actual == null ? null : actual.city(), r); }
    public AddressAssert hasZip(String v) { return hasProperty("zip", actual == null ? null : actual.zip(), v); }
    public AddressAssert hasZipSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("zip", actual == null ? null : actual.zip(), r); }
}
