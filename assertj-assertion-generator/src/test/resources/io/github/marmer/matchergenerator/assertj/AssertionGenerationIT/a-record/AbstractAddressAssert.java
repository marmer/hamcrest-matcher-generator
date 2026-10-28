package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Class;
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public abstract class AbstractAddressAssert<SELF extends AbstractAddressAssert<SELF, ACTUAL>, ACTUAL extends Address> extends AbstractObjectAssert<SELF, ACTUAL> {
  private static final List<String> PROPERTY_NAMES = List.of("street", "city");

  private final String path;

  private final Set<String> addressed = new HashSet<>();

  protected AbstractAddressAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Address");
  }

  public SELF hasStreet(final @Nullable String value) {
    isNotNull();
    return checkProperty("street", actual.street(), value);
  }

  public SELF hasStreetSatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    addressed.add("street");
    AssertionPath.within(path + ".street", actual.street(), requirements);
    return myself;
  }

  public SELF ignoringStreet() {
    addressed.add("street");
    return myself;
  }

  public SELF hasCity(final @Nullable String value) {
    isNotNull();
    return checkProperty("city", actual.city(), value);
  }

  public SELF hasCitySatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    addressed.add("city");
    AssertionPath.within(path + ".city", actual.city(), requirements);
    return myself;
  }

  public SELF ignoringCity() {
    addressed.add("city");
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
    addressed.add(property);
    if (!Objects.deepEquals(actualValue, expectedValue)) {
      final Representation representation = getWritableAssertionInfo().representation();
      throw failureWithActualExpected(actualValue, expectedValue, "%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s", path, property, representation.toStringOf(expectedValue), representation.toStringOf(actualValue));
    }
    return myself;
  }

  public SELF satisfiesCheckingAllProperties(final Consumer<? super SELF> requirements) {
    isNotNull();
    requirements.accept(myself);
    return hasAllPropertiesChecked();
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
