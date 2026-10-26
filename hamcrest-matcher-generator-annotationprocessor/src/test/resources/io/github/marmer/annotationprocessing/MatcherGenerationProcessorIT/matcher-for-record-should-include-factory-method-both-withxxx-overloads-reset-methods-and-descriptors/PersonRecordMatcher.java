package some.other.pck;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.lang.Class;
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
public class PersonRecordMatcher extends TypeSafeMatcher<PersonRecord> {
  private final BeanPropertyMatcher<PersonRecord> beanPropertyMatcher;

  public PersonRecordMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<PersonRecord>(PersonRecord.class);
  }

  public PersonRecordMatcher withFirstName(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("firstName", matcher);
    return this;
  }

  public PersonRecordMatcher withLastName(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("lastName", matcher);
    return this;
  }

  public PersonRecordMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public PersonRecordMatcher resetFirstName() {
    beanPropertyMatcher.reset("firstName");
    return this;
  }

  public PersonRecordMatcher resetLastName() {
    beanPropertyMatcher.reset("lastName");
    return this;
  }

  public PersonRecordMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public PersonRecordMatcher withFirstName(final String value) {
    beanPropertyMatcher.with("firstName", Matchers.equalTo(value));
    return this;
  }

  public PersonRecordMatcher withLastName(final String value) {
    beanPropertyMatcher.with("lastName", Matchers.equalTo(value));
    return this;
  }

  public PersonRecordMatcher withClass(final Class<?> value) {
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
