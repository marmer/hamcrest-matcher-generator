package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
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
public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo<?>> {
  private final BeanPropertyMatcher<SimplePojo<?>> beanPropertyMatcher;

  public SimplePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo<?>>(SimplePojo.class);
  }

  public SimplePojoMatcher withName(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("name", matcher);
    return this;
  }

  public SimplePojoMatcher withNickname(final Matcher<? super @Nullable String> matcher) {
    beanPropertyMatcher.with("nickname", matcher);
    return this;
  }

  public SimplePojoMatcher withCount(final Matcher<? super Integer> matcher) {
    beanPropertyMatcher.with("count", matcher);
    return this;
  }

  public SimplePojoMatcher withLimit(final Matcher<? super @Nullable Integer> matcher) {
    beanPropertyMatcher.with("limit", matcher);
    return this;
  }

  public SimplePojoMatcher withTags(
      final Matcher<? super @Nullable List<? extends String>> matcher) {
    beanPropertyMatcher.with("tags", matcher);
    return this;
  }

  public SimplePojoMatcher withParts(final Matcher<? super String[]> matcher) {
    beanPropertyMatcher.with("parts", matcher);
    return this;
  }

  public SimplePojoMatcher withPayload(final Matcher<? super Object> matcher) {
    beanPropertyMatcher.with("payload", matcher);
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

  public SimplePojoMatcher resetNickname() {
    beanPropertyMatcher.reset("nickname");
    return this;
  }

  public SimplePojoMatcher resetCount() {
    beanPropertyMatcher.reset("count");
    return this;
  }

  public SimplePojoMatcher resetLimit() {
    beanPropertyMatcher.reset("limit");
    return this;
  }

  public SimplePojoMatcher resetTags() {
    beanPropertyMatcher.reset("tags");
    return this;
  }

  public SimplePojoMatcher resetParts() {
    beanPropertyMatcher.reset("parts");
    return this;
  }

  public SimplePojoMatcher resetPayload() {
    beanPropertyMatcher.reset("payload");
    return this;
  }

  public SimplePojoMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojoMatcher withName(final @Nullable String value) {
    beanPropertyMatcher.with("name", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withNickname(final @Nullable String value) {
    beanPropertyMatcher.with("nickname", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withCount(final int value) {
    beanPropertyMatcher.with("count", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withLimit(final @Nullable Integer value) {
    beanPropertyMatcher.with("limit", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withTags(final @Nullable List<? extends String> value) {
    beanPropertyMatcher.with("tags", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withParts(final String @Nullable [] value) {
    beanPropertyMatcher.with("parts", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withPayload(final @Nullable Object value) {
    beanPropertyMatcher.with("payload", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withClass(final @Nullable Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SimplePojo<?> item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SimplePojo<?> item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SimplePojoMatcher isSimplePojo() {
    return new SimplePojoMatcher();
  }

  @Generated(
      value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
      date = "2020-01-01T00:00"
  )
  public static class InnerMatcher extends TypeSafeMatcher<SimplePojo.Inner> {
    private final BeanPropertyMatcher<SimplePojo.Inner> beanPropertyMatcher;

    public InnerMatcher() {
      beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.Inner>(SimplePojo.Inner.class);
    }

    public InnerMatcher withInnerName(final Matcher<? super String> matcher) {
      beanPropertyMatcher.with("innerName", matcher);
      return this;
    }

    public InnerMatcher withClass(final Matcher<? super Class<?>> matcher) {
      beanPropertyMatcher.with("class", matcher);
      return this;
    }

    public InnerMatcher resetInnerName() {
      beanPropertyMatcher.reset("innerName");
      return this;
    }

    public InnerMatcher resetClass() {
      beanPropertyMatcher.reset("class");
      return this;
    }

    public InnerMatcher withInnerName(final @Nullable String value) {
      beanPropertyMatcher.with("innerName", Matchers.equalTo(value));
      return this;
    }

    public InnerMatcher withClass(final @Nullable Class<?> value) {
      beanPropertyMatcher.with("class", Matchers.equalTo(value));
      return this;
    }

    @Override
    public void describeTo(final Description description) {
      beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final SimplePojo.Inner item) {
      return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final SimplePojo.Inner item,
        final Description description) {
      beanPropertyMatcher.describeMismatch(item, description);
    }

    public static InnerMatcher isInner() {
      return new InnerMatcher();
    }
  }
}
