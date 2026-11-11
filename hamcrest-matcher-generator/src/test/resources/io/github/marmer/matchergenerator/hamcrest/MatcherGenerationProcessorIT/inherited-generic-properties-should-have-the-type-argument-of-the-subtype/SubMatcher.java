package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import io.github.marmer.matchergenerator.hamcrest.ReferenceMatching;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class SubMatcher extends TypeSafeMatcher<Sub> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<Sub> beanPropertyMatcher;

  public SubMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<Sub>(Sub.class, "value", "items", "class");
  }

  public SubMatcher withValue(final Matcher<? super @Nullable String> matcher) {
    beanPropertyMatcher.with("value", matcher);
    return this;
  }

  public SubMatcher withItems(final Matcher<? super List<? extends String>> matcher) {
    beanPropertyMatcher.with("items", matcher);
    return this;
  }

  public SubMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SubMatcher resetValue() {
    beanPropertyMatcher.reset("value");
    return this;
  }

  public SubMatcher resetItems() {
    beanPropertyMatcher.reset("items");
    return this;
  }

  public SubMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SubMatcher withValue(final @Nullable String value) {
    beanPropertyMatcher.with("value", Matchers.equalTo(value));
    return this;
  }

  public SubMatcher withItems(final @Nullable List<? extends String> value) {
    beanPropertyMatcher.with("items", Matchers.equalTo(value));
    return this;
  }

  public SubMatcher withClass(final @Nullable Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SubMatcher ignoringValue() {
    beanPropertyMatcher.ignore("value");
    return this;
  }

  public SubMatcher ignoringItems() {
    beanPropertyMatcher.ignore("items");
    return this;
  }

  public SubMatcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public SubMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  public static SubMatcher isSubLike(final Sub reference) {
    return ReferenceMatching.matcherFor(reference, SubMatcher.class, SubMatcher::new, matcher -> matcher.presetFrom(reference));
  }

  private void presetFrom(final Sub reference) {
    beanPropertyMatcher.checkingAllProperties();
    beanPropertyMatcher.preset("value", ReferenceMatching.value(reference.getValue()));
    beanPropertyMatcher.preset("items", ReferenceMatching.value(reference.getItems()));
  }

  @Override
  public BeanPropertyMatcher<Sub> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final Sub item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final Sub item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SubMatcher isSub() {
    return new SubMatcher();
  }
}
