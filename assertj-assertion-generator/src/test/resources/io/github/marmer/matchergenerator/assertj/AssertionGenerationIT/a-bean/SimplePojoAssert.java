package some.other.pck;

import javax.annotation.processing.Generated;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class SimplePojoAssert extends AbstractSimplePojoAssert<SimplePojoAssert, SimplePojo> {
  public static final InstanceOfAssertFactory<SimplePojo, SimplePojoAssert> SIMPLE_POJO = new InstanceOfAssertFactory<>(SimplePojo.class, SimplePojoAssert::new);

  public SimplePojoAssert(final @Nullable SimplePojo actual) {
    super(actual, SimplePojoAssert.class);
  }

  public static SimplePojoAssert assertThat(final @Nullable SimplePojo actual) {
    return new SimplePojoAssert(actual);
  }
}
