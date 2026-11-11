package some.other.pck;

import javax.annotation.processing.Generated;
import org.assertj.core.api.InstanceOfAssertFactory;

@Generated("io.github.marmer.matchergenerator.core.MatcherGenerationProcessor")
public class OrderAssert extends AbstractOrderAssert<OrderAssert, Order> {
  public static final InstanceOfAssertFactory<Order, OrderAssert> ORDER = new InstanceOfAssertFactory<>(Order.class, OrderAssert::new);

  public OrderAssert(final Order actual) {
    super(actual, OrderAssert.class);
  }

  public static OrderAssert assertThat(final Order actual) {
    return new OrderAssert(actual);
  }
}
