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
public class OrderAssert extends AbstractOrderAssert<OrderAssert, Order> {
  public static final InstanceOfAssertFactory<Order, OrderAssert> ORDER = new InstanceOfAssertFactory<>(Order.class, OrderAssert::new);

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
    public static final InstanceOfAssertFactory<Order.Customer, CustomerAssert> CUSTOMER = new InstanceOfAssertFactory<>(Order.Customer.class, CustomerAssert::new);

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
      public static final InstanceOfAssertFactory<Order.Customer.Address, AddressAssert> ADDRESS = new InstanceOfAssertFactory<>(Order.Customer.Address.class, AddressAssert::new);

      public AddressAssert(final Order.Customer.@Nullable Address actual) {
        super(actual, AddressAssert.class);
      }

      public static AddressAssert assertThat(final Order.Customer.@Nullable Address actual) {
        return new AddressAssert(actual);
      }
    }
  }
}
