package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
@NullMarked
public class PersonRecordMatcher extends TypeSafeMatcher<PersonRecord> {
  private final BeanPropertyMatcher<PersonRecord> beanPropertyMatcher;

  public PersonRecordMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<PersonRecord>(PersonRecord.class);
  }

  public PersonRecordMatcher withName(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("name", matcher);
    return this;
  }

  public PersonRecordMatcher withNickname(final Matcher<? super @Nullable String> matcher) {
    beanPropertyMatcher.with("nickname", matcher);
    return this;
  }

  public PersonRecordMatcher withAge(final Matcher<? super Integer> matcher) {
    beanPropertyMatcher.with("age", matcher);
    return this;
  }

  public PersonRecordMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public PersonRecordMatcher resetName() {
    beanPropertyMatcher.reset("name");
    return this;
  }

  public PersonRecordMatcher resetNickname() {
    beanPropertyMatcher.reset("nickname");
    return this;
  }

  public PersonRecordMatcher resetAge() {
    beanPropertyMatcher.reset("age");
    return this;
  }

  public PersonRecordMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public PersonRecordMatcher withName(final @Nullable String value) {
    beanPropertyMatcher.with("name", Matchers.equalTo(value));
    return this;
  }

  public PersonRecordMatcher withNickname(final @Nullable String value) {
    beanPropertyMatcher.with("nickname", Matchers.equalTo(value));
    return this;
  }

  public PersonRecordMatcher withAge(final int value) {
    beanPropertyMatcher.with("age", Matchers.equalTo(value));
    return this;
  }

  public PersonRecordMatcher withClass(final @Nullable Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final PersonRecord item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final PersonRecord item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static PersonRecordMatcher isPersonRecord() {
    return new PersonRecordMatcher();
  }
}
