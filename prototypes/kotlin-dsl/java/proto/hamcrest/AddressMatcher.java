package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;

import java.util.function.Consumer;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;
import proto.model.Address;

/** PROTOTYPE. Hand-written stand-in for the generated matcher of the record {@link Address}. */
public final class AddressMatcher extends PropertyMatcher<Address, AddressMatcher> {

    private AddressMatcher() {
        super(Address.class);
        property("street", Address::street);
        property("city", Address::city);
        property("zip", Address::zip);
    }

    public static AddressMatcher isAddress() { return new AddressMatcher(); }

    /** K2 (Java-emitted lambda overload): {@code isAddress(m -> m.withFoo(..))}. */
    public static AddressMatcher isAddress(Consumer<? super AddressMatcher> spec) { AddressMatcher m = isAddress(); spec.accept(m); return m; }

    public static AddressMatcher isAddressLike(Address ref) {
        return isAddress().checkingAllProperties().withStreet(ref.street()).withCity(ref.city()).withZip(ref.zip());
    }

    public AddressMatcher withStreet(@Nullable String v) { return withStreet(equalTo(v)); }
    public AddressMatcher withStreet(Matcher<? super String> m) { return check("street", m); }
    public AddressMatcher ignoringStreet() { return ignore("street"); }

    public AddressMatcher withCity(@Nullable String v) { return withCity(equalTo(v)); }
    public AddressMatcher withCity(Matcher<? super String> m) { return check("city", m); }
    public AddressMatcher ignoringCity() { return ignore("city"); }

    public AddressMatcher withZip(@Nullable String v) { return withZip(equalTo(v)); }
    public AddressMatcher withZip(Matcher<? super String> m) { return check("zip", m); }
    public AddressMatcher ignoringZip() { return ignore("zip"); }
}
