package some.other.pck;

import javax.annotation.processing.Generated;

@Generated("io.github.marmer.matchergenerator.core.MatcherGenerationProcessor")
public interface SoftAssertionsProvider extends org.assertj.core.api.SoftAssertionsProvider {
  default OrderAssert assertThat(final Order actual) {
    return proxy(OrderAssert.class, Order.class, actual);
  }

  default OrderAssert then(final Order actual) {
    return proxy(OrderAssert.class, Order.class, actual);
  }
}
