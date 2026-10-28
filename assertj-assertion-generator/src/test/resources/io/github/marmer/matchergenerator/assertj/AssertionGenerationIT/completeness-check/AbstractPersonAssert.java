package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Boolean;
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
public abstract class AbstractPersonAssert<SELF extends AbstractPersonAssert<SELF, ACTUAL>, ACTUAL extends Person> extends AbstractObjectAssert<SELF, ACTUAL> {
  private static final List<String> PROPERTY_NAMES = List.of("name", "phone", "active");

  private final String path;

  private final Set<String> addressed = new HashSet<>();

  protected AbstractPersonAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Person");
  }

  public SELF hasName(final @Nullable String value) {
    isNotNull();
    return checkProperty("name", actual.getName(), value);
  }

  public SELF hasNameSatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    addressed.add("name");
    AssertionPath.within(path + ".name", actual.getName(), requirements);
    return myself;
  }

  public SELF ignoringName() {
    addressed.add("name");
    return myself;
  }

  public SELF hasPhone(final @Nullable String value) {
    isNotNull();
    return checkProperty("phone", actual.getPhone(), value);
  }

  public SELF hasPhoneSatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    addressed.add("phone");
    AssertionPath.within(path + ".phone", actual.getPhone(), requirements);
    return myself;
  }

  public SELF ignoringPhone() {
    addressed.add("phone");
    return myself;
  }

  public SELF hasActive(final boolean value) {
    isNotNull();
    return checkProperty("active", actual.isActive(), value);
  }

  public SELF hasActiveSatisfying(final Consumer<? super Boolean> requirements) {
    isNotNull();
    addressed.add("active");
    AssertionPath.within(path + ".active", actual.isActive(), requirements);
    return myself;
  }

  public SELF isActive() {
    isNotNull();
    return checkProperty("active", actual.isActive(), true);
  }

  public SELF isNotActive() {
    isNotNull();
    return checkProperty("active", actual.isActive(), false);
  }

  public SELF ignoringActive() {
    addressed.add("active");
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
