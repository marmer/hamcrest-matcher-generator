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

@Generated("io.github.marmer.matchergenerator.core.MatcherGenerationProcessor")
@NullMarked
public abstract class AbstractSubAssert<SELF extends AbstractSubAssert<SELF, ACTUAL>, ACTUAL extends Sub> extends AbstractObjectAssert<SELF, ACTUAL> {
  private static final List<String> PROPERTY_NAMES = List.of("value", "items");

  private final String path;

  private final Set<String> addressed = new HashSet<>();

  protected AbstractSubAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Sub", actual);
  }

  public SELF hasValue(final @Nullable String value) {
    isNotNull();
    return checkProperty("value", actual.getValue(), value);
  }

  public SELF hasValueSatisfying(final Consumer<? super @Nullable String> requirements) {
    isNotNull();
    addressed.add("value");
    AssertionPath.within(path + ".value", actual.getValue(), requirements);
    return myself;
  }

  public SELF ignoringValue() {
    addressed.add("value");
    return myself;
  }

  public SELF hasItems(final @Nullable List<? extends String> value) {
    isNotNull();
    return checkProperty("items", actual.getItems(), value);
  }

  public SELF hasItemsSatisfying(final Consumer<? super List<? extends String>> requirements) {
    isNotNull();
    addressed.add("items");
    AssertionPath.restartingWithin(actual.getItems(), requirements);
    return myself;
  }

  public SELF ignoringItems() {
    addressed.add("items");
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
