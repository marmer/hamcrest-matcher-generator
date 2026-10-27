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
public class SimplePojo1Matcher extends TypeSafeMatcher<SimplePojo1> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojo1> beanPropertyMatcher;

  public SimplePojo1Matcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo1>(SimplePojo1.class, "class");
  }

  public SimplePojo1Matcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SimplePojo1Matcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojo1Matcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SimplePojo1Matcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public SimplePojo1Matcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  public static SimplePojo1Matcher isSimplePojo1Like(final SimplePojo1 reference) {
    return ReferenceMatching.matcherFor(reference, SimplePojo1Matcher.class, SimplePojo1Matcher::new, matcher -> matcher.presetFrom(reference));
  }

  private void presetFrom(final SimplePojo1 reference) {
    beanPropertyMatcher.checkingAllProperties();
  }

  @Override
  public BeanPropertyMatcher<SimplePojo1> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SimplePojo1 item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SimplePojo1 item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SimplePojo1Matcher isSimplePojo1() {
    return new SimplePojo1Matcher();
  }
}
