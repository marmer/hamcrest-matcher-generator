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
public abstract class AbstractGadgetAssert<SELF extends AbstractGadgetAssert<SELF, ACTUAL>, ACTUAL extends Gadget> extends AbstractObjectAssert<SELF, ACTUAL> {
  private static final List<String> PROPERTY_NAMES = List.of("class");

  private final String path;

  private final Set<String> addressed = new HashSet<>();

  protected AbstractGadgetAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Gadget", actual);
  }

  public SELF hasClass(final boolean value) {
    isNotNull();
    return checkProperty("class", actual.isClass(), value);
  }

  public SELF hasClassSatisfying(final Consumer<? super Boolean> requirements) {
    isNotNull();
    addressed.add("class");
    AssertionPath.within(path + ".class", actual.isClass(), requirements);
    return myself;
  }

  public SELF isClass() {
    isNotNull();
    return checkProperty("class", actual.isClass(), true);
  }

  public SELF isNotClass() {
    isNotNull();
    return checkProperty("class", actual.isClass(), false);
  }

  public SELF ignoringClass() {
    addressed.add("class");
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
