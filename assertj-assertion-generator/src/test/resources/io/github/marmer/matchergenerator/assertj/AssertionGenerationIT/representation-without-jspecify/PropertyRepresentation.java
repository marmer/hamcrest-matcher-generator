package some.other.pck;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.assertj.core.presentation.StandardRepresentation;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class PropertyRepresentation extends StandardRepresentation {
  @Override
  protected String fallbackToStringOf(final Object object) {
    if (object instanceof Order actual) {
      return "Order[id=" + toStringOf(actual.getId()) + "]";
    }
    return super.fallbackToStringOf(object);
  }
}
