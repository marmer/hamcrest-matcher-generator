package some.other.pck;

import javax.annotation.processing.Generated;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public interface SoftAssertionsProvider extends org.assertj.core.api.SoftAssertionsProvider {
  default OrderAssert assertThat(final Order actual) {
    return proxy(OrderAssert.class, Order.class, actual);
  }

  default OrderAssert then(final Order actual) {
    return proxy(OrderAssert.class, Order.class, actual);
  }
}
