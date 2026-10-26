package some.other.pck;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(
    value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo<?, ?>> {
  private final BeanPropertyMatcher<SimplePojo<?, ?>> beanPropertyMatcher;

  public SimplePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo<?, ?>>(SimplePojo.class);
  }

  public SimplePojoMatcher withProperty(final Matcher<? super Object> matcher) {
    beanPropertyMatcher.with("property", matcher);
    return this;
  }

  public SimplePojoMatcher withNestedGenericProperty(
      final Matcher<? super Map<?, ? extends List<? extends Supplier<?>>>> matcher) {
    beanPropertyMatcher.with("nestedGenericProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withWildcardProperty(
      final Matcher<? super List<? extends Function<?, ? super Consumer<?>>>> matcher) {
    beanPropertyMatcher.with("wildcardProperty", matcher);
    return this;
  }

  public SimplePojoMatcher resetProperty() {
    beanPropertyMatcher.reset("property");
    return this;
  }

  public SimplePojoMatcher resetNestedGenericProperty() {
    beanPropertyMatcher.reset("nestedGenericProperty");
    return this;
  }

  public SimplePojoMatcher resetWildcardProperty() {
    beanPropertyMatcher.reset("wildcardProperty");
    return this;
  }

  public SimplePojoMatcher withProperty(final Object value) {
    beanPropertyMatcher.with("property", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withNestedGenericProperty(
      final Map<?, ? extends List<? extends Supplier<?>>> value) {
    beanPropertyMatcher.with("nestedGenericProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withWildcardProperty(
      final List<? extends Function<?, ? super Consumer<?>>> value) {
    beanPropertyMatcher.with("wildcardProperty", Matchers.equalTo(value));
    return this;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SimplePojo<?, ?> item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SimplePojo<?, ?> item,
      final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SimplePojoMatcher isSimplePojo() {
    return new SimplePojoMatcher();
  }
}
