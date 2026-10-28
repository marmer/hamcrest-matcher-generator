package some.other.pck;

import io.github.marmer.matchergenerator.assertj.AssertionPath;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
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
public abstract class AbstractSimplePojoAssert<SELF extends AbstractSimplePojoAssert<SELF, ACTUAL>, ACTUAL extends SimplePojo> extends AbstractObjectAssert<SELF, ACTUAL> {
  private final String path;

  protected AbstractSimplePojoAssert(final @Nullable ACTUAL actual, final Class<?> selfType) {
    super(actual, selfType);
    this.path = AssertionPath.rootedAt("SimplePojo");
  }

  public SELF hasName(final @Nullable String value) {
    isNotNull();
    return checkProperty("name", actual.getName(), value);
  }

  public SELF hasNameSatisfying(final Consumer<? super String> requirements) {
    isNotNull();
    AssertionPath.within(path + ".name", actual.getName(), requirements);
    return myself;
  }

  public SELF hasAge(final int value) {
    isNotNull();
    return checkProperty("age", actual.getAge(), value);
  }

  public SELF hasAgeSatisfying(final Consumer<? super Integer> requirements) {
    isNotNull();
    AssertionPath.within(path + ".age", actual.getAge(), requirements);
    return myself;
  }

  public SELF hasActive(final boolean value) {
    isNotNull();
    return checkProperty("active", actual.isActive(), value);
  }

  public SELF hasActiveSatisfying(final Consumer<? super Boolean> requirements) {
    isNotNull();
    AssertionPath.within(path + ".active", actual.isActive(), requirements);
    return myself;
  }

  public SELF hasTags(final @Nullable List<? extends String> value) {
    isNotNull();
    return checkProperty("tags", actual.getTags(), value);
  }

  public SELF hasTagsSatisfying(final Consumer<? super List<? extends String>> requirements) {
    isNotNull();
    AssertionPath.within(path + ".tags", actual.getTags(), requirements);
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
