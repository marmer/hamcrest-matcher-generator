package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import io.github.marmer.matchergenerator.hamcrest.ReferenceMatching;
import java.lang.Class;
import java.lang.Integer;
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
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class, BeanPropertyMatcher.property("name", (SimplePojo bean) -> bean.getName()), BeanPropertyMatcher.property("age", (SimplePojo bean) -> bean.getAge()), BeanPropertyMatcher.property("class", (SimplePojo bean) -> bean.getClass()));
  }

  public SimplePojoMatcher withName(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("name", matcher);
    return this;
  }

  public SimplePojoMatcher withAge(final Matcher<? super Integer> matcher) {
    beanPropertyMatcher.with("age", matcher);
    return this;
  }

  public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SimplePojoMatcher resetName() {
    beanPropertyMatcher.reset("name");
    return this;
  }

  public SimplePojoMatcher resetAge() {
    beanPropertyMatcher.reset("age");
    return this;
  }

  public SimplePojoMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojoMatcher withName(final String value) {
    beanPropertyMatcher.with("name", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withAge(final int value) {
    beanPropertyMatcher.with("age", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher ignoringName() {
    beanPropertyMatcher.ignore("name");
    return this;
  }

  public SimplePojoMatcher ignoringAge() {
    beanPropertyMatcher.ignore("age");
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
    beanPropertyMatcher.preset("name", ReferenceMatching.value(ReferenceMatching.read("name", reference::getName)));
    beanPropertyMatcher.preset("age", ReferenceMatching.value(reference.getAge()));
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
