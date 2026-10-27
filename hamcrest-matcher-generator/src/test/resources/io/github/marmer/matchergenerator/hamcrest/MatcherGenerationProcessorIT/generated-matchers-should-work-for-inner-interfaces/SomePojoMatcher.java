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
public class SomePojoMatcher extends TypeSafeMatcher<SomePojo> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SomePojo> beanPropertyMatcher;

  public SomePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SomePojo>(SomePojo.class, "class");
  }

  public SomePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SomePojoMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SomePojoMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SomePojoMatcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public SomePojoMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  public static SomePojoMatcher isSomePojoLike(final SomePojo reference) {
    return ReferenceMatching.matcherFor(reference, SomePojoMatcher.class, SomePojoMatcher::new, matcher -> matcher.presetFrom(reference));
  }

  private void presetFrom(final SomePojo reference) {
    beanPropertyMatcher.checkingAllProperties();
  }

  @Override
  public BeanPropertyMatcher<SomePojo> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SomePojo item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SomePojo item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SomePojoMatcher isSomePojo() {
    return new SomePojoMatcher();
  }

  @Generated(
      value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
      date = "2020-01-01T00:00"
  )
  public static class InnerInterfaceMatcher extends TypeSafeMatcher<SomePojo.InnerInterface> implements GeneratedBeanMatcher {
    private final BeanPropertyMatcher<SomePojo.InnerInterface> beanPropertyMatcher;

    public InnerInterfaceMatcher() {
      beanPropertyMatcher = new BeanPropertyMatcher<SomePojo.InnerInterface>(SomePojo.InnerInterface.class);
    }

    public InnerInterfaceMatcher checkingAllProperties() {
      beanPropertyMatcher.checkingAllProperties();
      return this;
    }

    public static InnerInterfaceMatcher isInnerInterfaceLike(
        final SomePojo.InnerInterface reference) {
      return ReferenceMatching.matcherFor(reference, InnerInterfaceMatcher.class, InnerInterfaceMatcher::new, matcher -> matcher.presetFrom(reference));
    }

    private void presetFrom(final SomePojo.InnerInterface reference) {
      beanPropertyMatcher.checkingAllProperties();
    }

    @Override
    public BeanPropertyMatcher<SomePojo.InnerInterface> beanPropertyMatcher() {
      return beanPropertyMatcher;
    }

    @Override
    public void describeTo(final Description description) {
      beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final SomePojo.InnerInterface item) {
      return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final SomePojo.InnerInterface item,
        final Description description) {
      beanPropertyMatcher.describeMismatch(item, description);
    }

    public static InnerInterfaceMatcher isInnerInterface() {
      return new InnerInterfaceMatcher();
    }
  }
}
