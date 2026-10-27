package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import java.lang.Class;
import java.lang.Object;
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
public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;

  public SimplePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class, "propertyOfBothClasses", "parentPojoProperty", "class");
  }

  public SimplePojoMatcher withPropertyOfBothClasses(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("propertyOfBothClasses", matcher);
    return this;
  }

  public SimplePojoMatcher withParentPojoProperty(final Matcher<? super Object> matcher) {
    beanPropertyMatcher.with("parentPojoProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SimplePojoMatcher resetPropertyOfBothClasses() {
    beanPropertyMatcher.reset("propertyOfBothClasses");
    return this;
  }

  public SimplePojoMatcher resetParentPojoProperty() {
    beanPropertyMatcher.reset("parentPojoProperty");
    return this;
  }

  public SimplePojoMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojoMatcher withPropertyOfBothClasses(final String value) {
    beanPropertyMatcher.with("propertyOfBothClasses", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withParentPojoProperty(final Object value) {
    beanPropertyMatcher.with("parentPojoProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher ignoringPropertyOfBothClasses() {
    beanPropertyMatcher.ignore("propertyOfBothClasses");
    return this;
  }

  public SimplePojoMatcher ignoringParentPojoProperty() {
    beanPropertyMatcher.ignore("parentPojoProperty");
    return this;
  }

  public SimplePojoMatcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public SimplePojoMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  @Override
  public BeanPropertyMatcher<SimplePojo> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SimplePojo item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SimplePojo item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SimplePojoMatcher isSimplePojo() {
    return new SimplePojoMatcher();
  }
}
