package some.other.pck;

import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public interface SoftAssertionsProvider extends org.assertj.core.api.SoftAssertionsProvider {
  default LineAssert assertThat(final @Nullable Line actual) {
    return proxy(LineAssert.class, Line.class, actual);
  }

  default LineAssert then(final @Nullable Line actual) {
    return proxy(LineAssert.class, Line.class, actual);
  }

  default OrderAssert assertThat(final @Nullable Order actual) {
    return proxy(OrderAssert.class, Order.class, actual);
  }

  default OrderAssert then(final @Nullable Order actual) {
    return proxy(OrderAssert.class, Order.class, actual);
  }

  default OrderAssert.CustomerAssert assertThat(final Order.@Nullable Customer actual) {
    return proxy(OrderAssert.CustomerAssert.class, Order.Customer.class, actual);
  }

  default OrderAssert.CustomerAssert then(final Order.@Nullable Customer actual) {
    return proxy(OrderAssert.CustomerAssert.class, Order.Customer.class, actual);
  }
}
