package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Class;
import java.lang.Object;
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
public abstract class AbstractAddressAssert<SELF extends AbstractAddressAssert<SELF, ACTUAL>, ACTUAL extends Address> extends AbstractObjectAssert<SELF, ACTUAL> {
  private final String path;

  protected AbstractAddressAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Address");
  }

  public SELF hasStreet(final @Nullable String value) {
    requireActual();
    return checkProperty("street", actual.street(), value);
  }

  public SELF hasStreetSatisfying(final Consumer<? super String> requirements) {
    requireActual();
    AssertionPath.within(path + ".street", actual.street(), requirements);
    return myself;
  }

  public SELF hasCity(final @Nullable String value) {
    requireActual();
    return checkProperty("city", actual.city(), value);
  }

  public SELF hasCitySatisfying(final Consumer<? super String> requirements) {
    requireActual();
    AssertionPath.within(path + ".city", actual.city(), requirements);
    return myself;
  }

  private void requireActual() {
    if (actual == null) {
      failWithMessage("%nExpecting %s not to be null", path);
    }
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
