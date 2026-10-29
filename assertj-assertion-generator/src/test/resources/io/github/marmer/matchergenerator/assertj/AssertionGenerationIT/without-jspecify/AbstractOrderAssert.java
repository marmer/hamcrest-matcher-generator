package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.presentation.Representation;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public abstract class AbstractOrderAssert<SELF extends AbstractOrderAssert<SELF, ACTUAL>, ACTUAL extends Order> extends AbstractObjectAssert<SELF, ACTUAL> {
  private static final List<String> PROPERTY_NAMES = List.of("id", "quantity");

  private final String path;

  private final Set<String> addressed = new HashSet<>();

  protected AbstractOrderAssert(final ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Order", actual);
  }

  public SELF hasId(final String value) {
    isNotNull();
    return checkProperty("id", actual.getId(), value);
  }

  public SELF hasIdSatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    addressed.add("id");
    AssertionPath.within(path + ".id", actual.getId(), requirements);
    return myself;
  }

  public SELF ignoringId() {
    addressed.add("id");
    return myself;
  }

  public SELF hasQuantity(final int value) {
    isNotNull();
    return checkProperty("quantity", actual.getQuantity(), value);
  }

  public SELF hasQuantitySatisfying(final Consumer<? super Integer> requirements) {
    isNotNull();
    addressed.add("quantity");
    AssertionPath.within(path + ".quantity", actual.getQuantity(), requirements);
    return myself;
  }

  public SELF ignoringQuantity() {
    addressed.add("quantity");
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
    addressed.add(property);
    if (!Objects.deepEquals(actualValue, expectedValue)) {
      final Representation representation = getWritableAssertionInfo().representation();
      throw failureWithActualExpected(actualValue, expectedValue, "%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s", path, property, representation.toStringOf(expectedValue), representation.toStringOf(actualValue));
    }
    return myself;
  }

  public SELF satisfiesCheckingAllProperties(final Consumer<? super SELF> requirements) {
    isNotNull();
    final Set<String> chain = new HashSet<>(addressed);
    addressed.clear();
    try {
      requirements.accept(myself);
      return hasAllPropertiesChecked();
    } finally {
      addressed.addAll(chain);
    }
  }

  public SELF hasAllPropertiesChecked() {
    isNotNull();
    final List<String> neither = PROPERTY_NAMES.stream().filter(property -> !addressed.contains(property)).collect(Collectors.toList());
    if (!neither.isEmpty()) {
      failWithMessage("%nExpecting every property of %s to be checked or ignored, but these were neither: %s", path, String.join(", ", neither));
    }
    return myself;
  }
}
