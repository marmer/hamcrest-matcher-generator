package some.other.pck;

import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class OrderAssert extends AbstractOrderAssert<OrderAssert, Order> {
  public OrderAssert(final @Nullable Order actual) {
    super(actual, OrderAssert.class);
  }

  public static OrderAssert assertThat(final @Nullable Order actual) {
    return new OrderAssert(actual);
  }

  @Generated(
      value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
      date = "2020-01-01T00:00"
  )
  public static class CustomerAssert extends AbstractOrderAssert.AbstractCustomerAssert<CustomerAssert, Order.Customer> {
    public CustomerAssert(final Order.@Nullable Customer actual) {
      super(actual, CustomerAssert.class);
    }

    public static CustomerAssert assertThat(final Order.@Nullable Customer actual) {
      return new CustomerAssert(actual);
    }

    @Generated(
        value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
        date = "2020-01-01T00:00"
    )
    public static class AddressAssert extends AbstractOrderAssert.AbstractCustomerAssert.AbstractAddressAssert<AddressAssert, Order.Customer.Address> {
      public AddressAssert(final Order.Customer.@Nullable Address actual) {
        super(actual, AddressAssert.class);
      }

      public static AddressAssert assertThat(final Order.Customer.@Nullable Address actual) {
        return new AddressAssert(actual);
      }
    }
  }
}
