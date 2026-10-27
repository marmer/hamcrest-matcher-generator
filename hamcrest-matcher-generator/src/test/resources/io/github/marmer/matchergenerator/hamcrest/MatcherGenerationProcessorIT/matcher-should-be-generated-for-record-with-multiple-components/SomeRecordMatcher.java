package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import java.lang.Class;
import java.lang.Integer;
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
public class SomeRecordMatcher extends TypeSafeMatcher<SomeRecord> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SomeRecord> beanPropertyMatcher;

  public SomeRecordMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SomeRecord>(SomeRecord.class, "name", "age", "class");
  }

  public SomeRecordMatcher withName(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("name", matcher);
    return this;
  }

  public SomeRecordMatcher withAge(final Matcher<? super Integer> matcher) {
    beanPropertyMatcher.with("age", matcher);
    return this;
  }

  public SomeRecordMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SomeRecordMatcher resetName() {
    beanPropertyMatcher.reset("name");
    return this;
  }

  public SomeRecordMatcher resetAge() {
    beanPropertyMatcher.reset("age");
    return this;
  }

  public SomeRecordMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SomeRecordMatcher withName(final String value) {
    beanPropertyMatcher.with("name", Matchers.equalTo(value));
    return this;
  }

  public SomeRecordMatcher withAge(final int value) {
    beanPropertyMatcher.with("age", Matchers.equalTo(value));
    return this;
  }

  public SomeRecordMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SomeRecordMatcher ignoringName() {
    beanPropertyMatcher.ignore("name");
    return this;
  }

  public SomeRecordMatcher ignoringAge() {
    beanPropertyMatcher.ignore("age");
    return this;
  }

  public SomeRecordMatcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public SomeRecordMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  @Override
  public BeanPropertyMatcher<SomeRecord> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SomeRecord item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SomeRecord item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SomeRecordMatcher isSomeRecord() {
    return new SomeRecordMatcher();
  }
}
