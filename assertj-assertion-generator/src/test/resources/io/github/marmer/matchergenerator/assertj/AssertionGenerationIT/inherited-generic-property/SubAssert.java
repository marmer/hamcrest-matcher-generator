package some.other.pck;

import javax.annotation.processing.Generated;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated("io.github.marmer.matchergenerator.core.MatcherGenerationProcessor")
@NullMarked
public class SubAssert extends AbstractSubAssert<SubAssert, Sub> {
  public static final InstanceOfAssertFactory<Sub, SubAssert> SUB = new InstanceOfAssertFactory<>(Sub.class, SubAssert::new);

  public SubAssert(final @Nullable Sub actual) {
    super(actual, SubAssert.class);
  }

  public static SubAssert assertThat(final @Nullable Sub actual) {
    return new SubAssert(actual);
  }
}
