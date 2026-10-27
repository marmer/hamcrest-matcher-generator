package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import java.lang.Boolean;
import java.lang.Byte;
import java.lang.Character;
import java.lang.Class;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.Short;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
  private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;

  public SimplePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
  }

  public SimplePojoMatcher withIntProperty(final Matcher<? super Integer> matcher) {
    beanPropertyMatcher.with("intProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withShortProperty(final Matcher<? super Short> matcher) {
    beanPropertyMatcher.with("shortProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withLongProperty(final Matcher<? super Long> matcher) {
    beanPropertyMatcher.with("longProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withDoubleProperty(final Matcher<? super Double> matcher) {
    beanPropertyMatcher.with("doubleProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withFloatProperty(final Matcher<? super Float> matcher) {
    beanPropertyMatcher.with("floatProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withCharProperty(final Matcher<? super Character> matcher) {
    beanPropertyMatcher.with("charProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withByteProperty(final Matcher<? super Byte> matcher) {
    beanPropertyMatcher.with("byteProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withBooleanProperty(final Matcher<? super Boolean> matcher) {
    beanPropertyMatcher.with("booleanProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SimplePojoMatcher resetIntProperty() {
    beanPropertyMatcher.reset("intProperty");
    return this;
  }

  public SimplePojoMatcher resetShortProperty() {
    beanPropertyMatcher.reset("shortProperty");
    return this;
  }

  public SimplePojoMatcher resetLongProperty() {
    beanPropertyMatcher.reset("longProperty");
    return this;
  }

  public SimplePojoMatcher resetDoubleProperty() {
    beanPropertyMatcher.reset("doubleProperty");
    return this;
  }

  public SimplePojoMatcher resetFloatProperty() {
    beanPropertyMatcher.reset("floatProperty");
    return this;
  }

  public SimplePojoMatcher resetCharProperty() {
    beanPropertyMatcher.reset("charProperty");
    return this;
  }

  public SimplePojoMatcher resetByteProperty() {
    beanPropertyMatcher.reset("byteProperty");
    return this;
  }

  public SimplePojoMatcher resetBooleanProperty() {
    beanPropertyMatcher.reset("booleanProperty");
    return this;
  }

  public SimplePojoMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojoMatcher withIntProperty(final int value) {
    beanPropertyMatcher.with("intProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withShortProperty(final short value) {
    beanPropertyMatcher.with("shortProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withLongProperty(final long value) {
    beanPropertyMatcher.with("longProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withDoubleProperty(final double value) {
    beanPropertyMatcher.with("doubleProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withFloatProperty(final float value) {
    beanPropertyMatcher.with("floatProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withCharProperty(final char value) {
    beanPropertyMatcher.with("charProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withByteProperty(final byte value) {
    beanPropertyMatcher.with("byteProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withBooleanProperty(final boolean value) {
    beanPropertyMatcher.with("booleanProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
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
