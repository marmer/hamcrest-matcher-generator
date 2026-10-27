package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
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
public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;

  public SimplePojoInterfaceMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class, "someStringProperty");
  }

  public SimplePojoInterfaceMatcher withSomeStringProperty(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("someStringProperty", matcher);
    return this;
  }

  public SimplePojoInterfaceMatcher resetSomeStringProperty() {
    beanPropertyMatcher.reset("someStringProperty");
    return this;
  }

  public SimplePojoInterfaceMatcher withSomeStringProperty(final String value) {
    beanPropertyMatcher.with("someStringProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoInterfaceMatcher ignoringSomeStringProperty() {
    beanPropertyMatcher.ignore("someStringProperty");
    return this;
  }

  public SimplePojoInterfaceMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  @Override
  public BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SimplePojoInterface item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SimplePojoInterface item,
      final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SimplePojoInterfaceMatcher isSimplePojoInterface() {
    return new SimplePojoInterfaceMatcher();
  }
}
