package some.other.pck;

import java.lang.Class;
import javax.annotation.processing.Generated;
import org.assertj.core.api.AbstractObjectAssert;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated("io.github.marmer.matchergenerator.core.MatcherGenerationProcessor")
@NullMarked
public abstract class AbstractEmptyAssert<SELF extends AbstractEmptyAssert<SELF, ACTUAL>, ACTUAL extends Empty> extends AbstractObjectAssert<SELF, ACTUAL> {
  protected AbstractEmptyAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
  }
}
