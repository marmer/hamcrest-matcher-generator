package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class SimplePojoEnumMatcher extends TypeSafeMatcher<SimplePojoEnum> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojoEnum> beanPropertyMatcher;

  public SimplePojoEnumMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoEnum>(SimplePojoEnum.class, "someStringProperty", "declaringClass", "class");
  }

  public SimplePojoEnumMatcher withSomeStringProperty(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("someStringProperty", matcher);
    return this;
  }

  public SimplePojoEnumMatcher withDeclaringClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("declaringClass", matcher);
    return this;
  }

  public SimplePojoEnumMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SimplePojoEnumMatcher resetSomeStringProperty() {
    beanPropertyMatcher.reset("someStringProperty");
    return this;
  }

  public SimplePojoEnumMatcher resetDeclaringClass() {
    beanPropertyMatcher.reset("declaringClass");
    return this;
  }

  public SimplePojoEnumMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojoEnumMatcher withSomeStringProperty(final String value) {
    beanPropertyMatcher.with("someStringProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoEnumMatcher withDeclaringClass(final Class<?> value) {
    beanPropertyMatcher.with("declaringClass", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoEnumMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  @Override
  public BeanPropertyMatcher<SimplePojoEnum> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SimplePojoEnum item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SimplePojoEnum item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SimplePojoEnumMatcher isSimplePojoEnum() {
    return new SimplePojoEnumMatcher();
  }
}
