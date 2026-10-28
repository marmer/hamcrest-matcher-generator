package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Class;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.processing.Generated;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.presentation.Representation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public abstract class AbstractLineAssert<SELF extends AbstractLineAssert<SELF, ACTUAL>, ACTUAL extends Line> extends AbstractObjectAssert<SELF, ACTUAL> {
  private final String path;

  protected AbstractLineAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Line");
  }

  public SELF hasSku(final @Nullable String value) {
    isNotNull();
    return checkProperty("sku", actual.getSku(), value);
  }

  public SELF hasSkuSatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    AssertionPath.within(path + ".sku", actual.getSku(), requirements);
    return myself;
  }

  @Override
  public SELF isNotNull() {
    if (actual == null) {
      failWithMessage("%nExpecting %s not to be null", path);
    }
    return myself;
  }

  private SELF checkProperty(final String property, final @Nullable Object actualValue,
      final @Nullable Object expectedValue) {
    if (!Objects.deepEquals(actualValue, expectedValue)) {
      final Representation representation = getWritableAssertionInfo().representation();
      throw failureWithActualExpected(actualValue, expectedValue, "%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s", path, property, representation.toStringOf(expectedValue), representation.toStringOf(actualValue));
    }
    return myself;
  }
}
