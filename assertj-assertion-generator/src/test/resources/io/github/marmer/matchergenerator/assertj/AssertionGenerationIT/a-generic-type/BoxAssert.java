package some.other.pck;

import java.lang.Number;
import java.lang.Object;
import java.lang.SuppressWarnings;
import java.lang.reflect.Type;
import javax.annotation.processing.Generated;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class BoxAssert extends AbstractBoxAssert<BoxAssert, Box<?, ?>> {
  @SuppressWarnings("rawtypes")
  public static final InstanceOfAssertFactory<Box, BoxAssert> BOX = new InstanceOfAssertFactory<>(Box.class, new Type[] {Object.class, Number.class}, BoxAssert::new);

  public BoxAssert(final @Nullable Box<?, ?> actual) {
    super(actual, BoxAssert.class);
  }

  public static BoxAssert assertThat(final @Nullable Box<?, ?> actual) {
    return new BoxAssert(actual);
  }
}
