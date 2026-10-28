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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class PropertyRepresentation extends StandardRepresentation {
  private static final ThreadLocal<Set<Object>> RENDERING = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

  @Override
  protected String fallbackToStringOf(final @Nullable Object object) {
    if (!RENDERING.get().add(object)) {
      return super.fallbackToStringOf(object);
    }
    try {
      if (object instanceof Box<?> actual) {
        return "Box[content=" + toStringOf(actual.getContent()) + ", contents=" + toStringOf(actual.getContents()) + "]";
      }
      return super.fallbackToStringOf(object);
    } finally {
      RENDERING.get().remove(object);
    }
  }
}
