package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import io.github.marmer.matchergenerator.hamcrest.ReferenceMatching;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated("io.github.marmer.matchergenerator.core.MatcherGenerationProcessor")
public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;

  public SimplePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class, BeanPropertyMatcher.property("foo", (SimplePojo bean) -> bean.getFoo()), BeanPropertyMatcher.property("class", (SimplePojo bean) -> bean.getClass()));
  }

  public SimplePojoMatcher withFoo(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("foo", matcher);
    return this;
  }

  public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SimplePojoMatcher resetFoo() {
    beanPropertyMatcher.reset("foo");
    return this;
  }

  public SimplePojoMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojoMatcher withFoo(final String value) {
    beanPropertyMatcher.with("foo", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher ignoringFoo() {
    beanPropertyMatcher.ignore("foo");
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

  public static SimplePojoMatcher isSimplePojoLike(final SimplePojo reference) {
    return ReferenceMatching.matcherFor(reference, SimplePojoMatcher.class, SimplePojoMatcher::new, matcher -> matcher.presetFrom(reference));
  }

  private void presetFrom(final SimplePojo reference) {
    beanPropertyMatcher.checkingAllProperties();
    beanPropertyMatcher.preset("foo", ReferenceMatching.value(reference.getFoo()));
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
