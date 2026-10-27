package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import io.github.marmer.matchergenerator.hamcrest.ReferenceMatching;
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
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class, "property");
  }

  public SimplePojoMatcher withProperty(final Matcher<? extends String> value) {
    beanPropertyMatcher.with("property", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher ignoringProperty() {
    beanPropertyMatcher.ignore("property");
    return this;
  }

  public SimplePojoMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  public static SimplePojoMatcher isSimplePojoLike(final SimplePojo reference) {
    return ReferenceMatching.matcherFor(reference, SimplePojoMatcher.class, SimplePojoMatcher::new, matcher -> matcher.presetFrom(reference));
  }

  private void presetFrom(final SimplePojo reference) {
    beanPropertyMatcher.checkingAllProperties();
    beanPropertyMatcher.preset("property", ReferenceMatching.value(reference.getProperty()));
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
