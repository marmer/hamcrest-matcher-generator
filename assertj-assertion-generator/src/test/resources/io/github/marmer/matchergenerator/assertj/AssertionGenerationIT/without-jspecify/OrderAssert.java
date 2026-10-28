package some.other.pck;

import javax.annotation.processing.Generated;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class OrderAssert extends AbstractOrderAssert<OrderAssert, Order> {
  public OrderAssert(final Order actual) {
    super(actual, OrderAssert.class);
  }

  public static OrderAssert assertThat(final Order actual) {
    return new OrderAssert(actual);
  }
}
