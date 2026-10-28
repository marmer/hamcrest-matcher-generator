package some.other.pck;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.assertj.core.presentation.StandardRepresentation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class PropertyRepresentation extends StandardRepresentation {
  @Override
  protected String fallbackToStringOf(final @Nullable Object object) {
    if (object instanceof Line actual) {
      return "Line[sku=" + toStringOf(actual.getSku()) + ", quantity=" + toStringOf(actual.getQuantity()) + "]";
    }
    if (object instanceof Order actual) {
      return "Order[customer=" + toStringOf(actual.getCustomer()) + "]";
    }
    if (object instanceof Order.Customer actual) {
      return "Customer[name=" + toStringOf(actual.getName()) + "]";
    }
    return super.fallbackToStringOf(object);
  }
}
