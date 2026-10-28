package some.other.pck;

import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class BoxAssert extends AbstractBoxAssert<BoxAssert, Box<?, ?>> {
  public BoxAssert(final @Nullable Box<?, ?> actual) {
    super(actual, BoxAssert.class);
  }

  public static BoxAssert assertThat(final @Nullable Box<?, ?> actual) {
    return new BoxAssert(actual);
  }
}
