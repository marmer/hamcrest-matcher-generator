package some.other.pck;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.ThreadLocal;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.assertj.core.presentation.StandardRepresentation;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class PropertyRepresentation extends StandardRepresentation {
  private static final ThreadLocal<Set<Object>> RENDERING = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

  @Override
  protected String fallbackToStringOf(final Object object) {
    if (!RENDERING.get().add(object)) {
      return super.fallbackToStringOf(object);
    }
    try {
      if (object instanceof Order actual) {
        return "Order[id=" + toStringOf(actual.getId()) + "]";
      }
      return super.fallbackToStringOf(object);
    } finally {
      RENDERING.get().remove(object);
    }
  }
}
