package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.processing.Generated;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.presentation.Representation;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public abstract class AbstractOrderAssert<SELF extends AbstractOrderAssert<SELF, ACTUAL>, ACTUAL extends Order> extends AbstractObjectAssert<SELF, ACTUAL> {
  private final String path;

  protected AbstractOrderAssert(final ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Order");
  }

  public SELF hasId(final String value) {
    isNotNull();
    return checkProperty("id", actual.getId(), value);
  }

  public SELF hasIdSatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    AssertionPath.within(path + ".id", actual.getId(), requirements);
    return myself;
  }

  public SELF hasQuantity(final int value) {
    isNotNull();
    return checkProperty("quantity", actual.getQuantity(), value);
  }

  public SELF hasQuantitySatisfying(final Consumer<? super Integer> requirements) {
    isNotNull();
    AssertionPath.within(path + ".quantity", actual.getQuantity(), requirements);
    return myself;
  }

  @Override
  public SELF isNotNull() {
    if (actual == null) {
      failWithMessage("%nExpecting %s not to be null", path);
    }
    return myself;
  }

  private SELF checkProperty(final String property, final Object actualValue,
      final Object expectedValue) {
    if (!Objects.deepEquals(actualValue, expectedValue)) {
      final Representation representation = getWritableAssertionInfo().representation();
      throw failureWithActualExpected(actualValue, expectedValue, "%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s", path, property, representation.toStringOf(expectedValue), representation.toStringOf(actualValue));
    }
    return myself;
  }
}
