package some.other.pck;

import javax.annotation.processing.Generated;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class AddressAssert extends AbstractAddressAssert<AddressAssert, Address> {
  public static final InstanceOfAssertFactory<Address, AddressAssert> ADDRESS = new InstanceOfAssertFactory<>(Address.class, AddressAssert::new);

  public AddressAssert(final @Nullable Address actual) {
    super(actual, AddressAssert.class);
  }

  public static AddressAssert assertThat(final @Nullable Address actual) {
    return new AddressAssert(actual);
  }
}
