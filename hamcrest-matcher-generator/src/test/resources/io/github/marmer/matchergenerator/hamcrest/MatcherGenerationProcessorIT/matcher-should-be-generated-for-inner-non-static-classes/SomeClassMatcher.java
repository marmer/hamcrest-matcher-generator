package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
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
public class SomeClassMatcher extends TypeSafeMatcher<SomeClass> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SomeClass> beanPropertyMatcher;

  public SomeClassMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SomeClass>(SomeClass.class, "class");
  }

  public SomeClassMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SomeClassMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SomeClassMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  @Override
  public BeanPropertyMatcher<SomeClass> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SomeClass item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SomeClass item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SomeClassMatcher isSomeClass() {
    return new SomeClassMatcher();
  }

  @Generated(
      value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
      date = "2020-01-01T00:00"
  )
  public static class SomeNonStaticInnerClassMatcher extends TypeSafeMatcher<SomeClass.SomeNonStaticInnerClass> implements GeneratedBeanMatcher {
    private final BeanPropertyMatcher<SomeClass.SomeNonStaticInnerClass> beanPropertyMatcher;

    public SomeNonStaticInnerClassMatcher() {
      beanPropertyMatcher = new BeanPropertyMatcher<SomeClass.SomeNonStaticInnerClass>(SomeClass.SomeNonStaticInnerClass.class, "class");
    }

    public SomeNonStaticInnerClassMatcher withClass(final Matcher<? super Class<?>> matcher) {
      beanPropertyMatcher.with("class", matcher);
      return this;
    }

    public SomeNonStaticInnerClassMatcher resetClass() {
      beanPropertyMatcher.reset("class");
      return this;
    }

    public SomeNonStaticInnerClassMatcher withClass(final Class<?> value) {
      beanPropertyMatcher.with("class", Matchers.equalTo(value));
      return this;
    }

    @Override
    public BeanPropertyMatcher<SomeClass.SomeNonStaticInnerClass> beanPropertyMatcher() {
      return beanPropertyMatcher;
    }

    @Override
    public void describeTo(final Description description) {
      beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final SomeClass.SomeNonStaticInnerClass item) {
      return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final SomeClass.SomeNonStaticInnerClass item,
        final Description description) {
      beanPropertyMatcher.describeMismatch(item, description);
    }

    public static SomeNonStaticInnerClassMatcher isSomeNonStaticInnerClass() {
      return new SomeNonStaticInnerClassMatcher();
    }
  }
}
