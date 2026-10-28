package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Class;
import java.lang.Object;
import java.lang.Override;
import java.lang.Runnable;
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
public abstract class AbstractInvoiceAssert<SELF extends AbstractInvoiceAssert<SELF, ACTUAL>, ACTUAL extends Invoice> extends AbstractObjectAssert<SELF, ACTUAL> {
  private static final List<String> PROPERTY_NAMES = List.of("number", "lines", "linesByCode", "array", "tags", "task");

  private final String path;

  private final Set<String> addressed = new HashSet<>();

  protected AbstractInvoiceAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("Invoice");
  }

  public SELF hasNumber(final @Nullable String value) {
    isNotNull();
    return checkProperty("number", actual.getNumber(), value);
  }

  public SELF hasNumberSatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    addressed.add("number");
    AssertionPath.within(path + ".number", actual.getNumber(), requirements);
    return myself;
  }

  public SELF ignoringNumber() {
    addressed.add("number");
    return myself;
  }

  public SELF hasLines(final @Nullable List<? extends Line> value) {
    isNotNull();
    return checkProperty("lines", actual.getLines(), value);
  }

  public SELF hasLinesSatisfying(final Consumer<? super List<? extends Line>> requirements) {
    isNotNull();
    addressed.add("lines");
    AssertionPath.restartingWithin(actual.getLines(), requirements);
    return myself;
  }

  public SELF ignoringLines() {
    addressed.add("lines");
    return myself;
  }

  public SELF hasLinesByCode(final @Nullable Map<? extends String, ? extends Line> value) {
    isNotNull();
    return checkProperty("linesByCode", actual.getLinesByCode(), value);
  }

  public SELF hasLinesByCodeSatisfying(
      final Consumer<? super Map<? extends String, ? extends Line>> requirements) {
    isNotNull();
    addressed.add("linesByCode");
    AssertionPath.restartingWithin(actual.getLinesByCode(), requirements);
    return myself;
  }

  public SELF ignoringLinesByCode() {
    addressed.add("linesByCode");
    return myself;
  }

  public SELF hasArray(final Line @Nullable [] value) {
    isNotNull();
    return checkProperty("array", actual.getArray(), value);
  }

  public SELF hasArraySatisfying(final Consumer<? super Line[]> requirements) {
    isNotNull();
    addressed.add("array");
    AssertionPath.restartingWithin(actual.getArray(), requirements);
    return myself;
  }

  public SELF ignoringArray() {
    addressed.add("array");
    return myself;
  }

  public SELF hasTags(final @Nullable Tags value) {
    isNotNull();
    return checkProperty("tags", actual.getTags(), value);
  }

  public SELF hasTagsSatisfying(final Consumer<? super Tags> requirements) {
    isNotNull();
    addressed.add("tags");
    AssertionPath.restartingWithin(actual.getTags(), requirements);
    return myself;
  }

  public SELF ignoringTags() {
    addressed.add("tags");
    return myself;
  }

  public SELF hasTask(final @Nullable Runnable value) {
    isNotNull();
    return checkProperty("task", actual.getTask(), value);
  }

  public SELF hasTaskSatisfying(final Consumer<? super Runnable> requirements) {
    isNotNull();
    addressed.add("task");
    AssertionPath.within(path + ".task", actual.getTask(), requirements);
    return myself;
  }

  public SELF ignoringTask() {
    addressed.add("task");
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
