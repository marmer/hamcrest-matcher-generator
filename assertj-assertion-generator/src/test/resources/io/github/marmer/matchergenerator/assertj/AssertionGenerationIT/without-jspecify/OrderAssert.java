package some.other.pck;

import javax.annotation.processing.Generated;
import org.assertj.core.api.InstanceOfAssertFactory;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class OrderAssert extends AbstractOrderAssert<OrderAssert, Order> {
  public static final InstanceOfAssertFactory<Order, OrderAssert> ORDER = new InstanceOfAssertFactory<>(Order.class, OrderAssert::new);

  public OrderAssert(final Order actual) {
    super(actual, OrderAssert.class);
  }

  public static OrderAssert assertThat(final Order actual) {
    return new OrderAssert(actual);
  }
}
