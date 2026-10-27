package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import io.github.marmer.matchergenerator.hamcrest.ReferenceMatching;
import java.lang.Class;
import java.lang.Override;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class SimplePojo2Matcher extends TypeSafeMatcher<SimplePojo2> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojo2> beanPropertyMatcher;

  public SimplePojo2Matcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo2>(SimplePojo2.class, "class");
  }

  public SimplePojo2Matcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SimplePojo2Matcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojo2Matcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SimplePojo2Matcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public SimplePojo2Matcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  public static SimplePojo2Matcher isSimplePojo2Like(final SimplePojo2 reference) {
    return ReferenceMatching.matcherFor(reference, SimplePojo2Matcher.class, SimplePojo2Matcher::new, matcher -> matcher.presetFrom(reference));
  }

  private void presetFrom(final SimplePojo2 reference) {
    beanPropertyMatcher.checkingAllProperties();
  }

  @Override
  public BeanPropertyMatcher<SimplePojo2> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SimplePojo2 item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SimplePojo2 item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SimplePojo2Matcher isSimplePojo2() {
    return new SimplePojo2Matcher();
  }
}
