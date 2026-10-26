package some.other.pck;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(
    value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {
  private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;

  public SimplePojoInterfaceMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);
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
