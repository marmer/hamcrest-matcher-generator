package some.other.pck;

import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class LineAssert extends AbstractLineAssert<LineAssert, Line> {
  public LineAssert(final @Nullable Line actual) {
    super(actual, LineAssert.class);
  }

  public static LineAssert assertThat(final @Nullable Line actual) {
    return new LineAssert(actual);
  }
}
