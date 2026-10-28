package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public abstract class AbstractBoxAssert<SELF extends AbstractBoxAssert<SELF, ACTUAL>, ACTUAL extends Box<?, ?>> extends AbstractObjectAssert<SELF, ACTUAL> {
  private static final List<String> PROPERTY_NAMES = List.of("content", "contents", "numbers", "supers", "anything");

  private final String path;

  private final Set<String> addressed = new HashSet<>();

  protected AbstractBoxAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Box");
  }

  public SELF hasContent(final @Nullable Object value) {
    isNotNull();
    return checkProperty("content", actual.getContent(), value);
  }

  public SELF hasContentSatisfying(final Consumer<? super Object> requirements) {
    isNotNull();
    addressed.add("content");
    AssertionPath.within(path + ".content", actual.getContent(), requirements);
    return myself;
  }

  public SELF ignoringContent() {
    addressed.add("content");
    return myself;
  }

  public SELF hasContents(final @Nullable List<?> value) {
    isNotNull();
    return checkProperty("contents", actual.getContents(), value);
  }

  public SELF hasContentsSatisfying(final Consumer<? super List<?>> requirements) {
    isNotNull();
    addressed.add("contents");
    AssertionPath.restartingWithin(actual.getContents(), requirements);
    return myself;
  }

  public SELF ignoringContents() {
    addressed.add("contents");
    return myself;
  }

  public SELF hasNumbers(final @Nullable Map<? extends String, ?> value) {
    isNotNull();
    return checkProperty("numbers", actual.getNumbers(), value);
  }

  public SELF hasNumbersSatisfying(final Consumer<? super Map<? extends String, ?>> requirements) {
    isNotNull();
    addressed.add("numbers");
    AssertionPath.restartingWithin(actual.getNumbers(), requirements);
    return myself;
  }

  public SELF ignoringNumbers() {
    addressed.add("numbers");
    return myself;
  }

  public SELF hasSupers(final @Nullable List<? super Integer> value) {
    isNotNull();
    return checkProperty("supers", actual.getSupers(), value);
  }

  public SELF hasSupersSatisfying(final Consumer<? super List<? super Integer>> requirements) {
    isNotNull();
    addressed.add("supers");
    AssertionPath.restartingWithin(actual.getSupers(), requirements);
    return myself;
  }

  public SELF ignoringSupers() {
    addressed.add("supers");
    return myself;
  }

  public SELF hasAnything(final @Nullable List<?> value) {
    isNotNull();
    return checkProperty("anything", actual.getAnything(), value);
  }

  public SELF hasAnythingSatisfying(final Consumer<? super List<?>> requirements) {
    isNotNull();
    addressed.add("anything");
    AssertionPath.restartingWithin(actual.getAnything(), requirements);
    return myself;
  }

  public SELF ignoringAnything() {
    addressed.add("anything");
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
