package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import io.github.marmer.matchergenerator.hamcrest.ReferenceMatching;
import java.lang.Boolean;
import java.lang.Class;
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
public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;

  public SimplePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class, "someStringProperty", "somePrimitiveBooleanProperty", "someNonePrimitiveBooleanProperty", "class");
  }

  public SimplePojoMatcher withSomeStringProperty(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("someStringProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withSomePrimitiveBooleanProperty(
      final Matcher<? super Boolean> matcher) {
    beanPropertyMatcher.with("somePrimitiveBooleanProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withSomeNonePrimitiveBooleanProperty(
      final Matcher<? super Boolean> matcher) {
    beanPropertyMatcher.with("someNonePrimitiveBooleanProperty", matcher);
    return this;
  }

  public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public SimplePojoMatcher resetSomeStringProperty() {
    beanPropertyMatcher.reset("someStringProperty");
    return this;
  }

  public SimplePojoMatcher resetSomePrimitiveBooleanProperty() {
    beanPropertyMatcher.reset("somePrimitiveBooleanProperty");
    return this;
  }

  public SimplePojoMatcher resetSomeNonePrimitiveBooleanProperty() {
    beanPropertyMatcher.reset("someNonePrimitiveBooleanProperty");
    return this;
  }

  public SimplePojoMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public SimplePojoMatcher withSomeStringProperty(final String value) {
    beanPropertyMatcher.with("someStringProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withSomePrimitiveBooleanProperty(final boolean value) {
    beanPropertyMatcher.with("somePrimitiveBooleanProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withSomeNonePrimitiveBooleanProperty(final Boolean value) {
    beanPropertyMatcher.with("someNonePrimitiveBooleanProperty", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoMatcher ignoringSomeStringProperty() {
    beanPropertyMatcher.ignore("someStringProperty");
    return this;
  }

  public SimplePojoMatcher ignoringSomePrimitiveBooleanProperty() {
    beanPropertyMatcher.ignore("somePrimitiveBooleanProperty");
    return this;
  }

  public SimplePojoMatcher ignoringSomeNonePrimitiveBooleanProperty() {
    beanPropertyMatcher.ignore("someNonePrimitiveBooleanProperty");
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
    beanPropertyMatcher.preset("someStringProperty", ReferenceMatching.value(reference.getSomeStringProperty()));
    beanPropertyMatcher.preset("somePrimitiveBooleanProperty", ReferenceMatching.value(reference.isSomePrimitiveBooleanProperty()));
    beanPropertyMatcher.preset("someNonePrimitiveBooleanProperty", ReferenceMatching.value(reference.getSomeNonePrimitiveBooleanProperty()));
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

  @Generated(
      value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
      date = "2020-01-01T00:00"
  )
  public static class InnerStaticPojoMatcher extends TypeSafeMatcher<SimplePojo.InnerStaticPojo> implements GeneratedBeanMatcher {
    private final BeanPropertyMatcher<SimplePojo.InnerStaticPojo> beanPropertyMatcher;

    public InnerStaticPojoMatcher() {
      beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerStaticPojo>(SimplePojo.InnerStaticPojo.class, "innerStaticPojoProperty", "class");
    }

    public InnerStaticPojoMatcher withInnerStaticPojoProperty(
        final Matcher<? super String> matcher) {
      beanPropertyMatcher.with("innerStaticPojoProperty", matcher);
      return this;
    }

    public InnerStaticPojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
      beanPropertyMatcher.with("class", matcher);
      return this;
    }

    public InnerStaticPojoMatcher resetInnerStaticPojoProperty() {
      beanPropertyMatcher.reset("innerStaticPojoProperty");
      return this;
    }

    public InnerStaticPojoMatcher resetClass() {
      beanPropertyMatcher.reset("class");
      return this;
    }

    public InnerStaticPojoMatcher withInnerStaticPojoProperty(final String value) {
      beanPropertyMatcher.with("innerStaticPojoProperty", Matchers.equalTo(value));
      return this;
    }

    public InnerStaticPojoMatcher withClass(final Class<?> value) {
      beanPropertyMatcher.with("class", Matchers.equalTo(value));
      return this;
    }

    public InnerStaticPojoMatcher ignoringInnerStaticPojoProperty() {
      beanPropertyMatcher.ignore("innerStaticPojoProperty");
      return this;
    }

    public InnerStaticPojoMatcher ignoringClass() {
      beanPropertyMatcher.ignore("class");
      return this;
    }

    public InnerStaticPojoMatcher checkingAllProperties() {
      beanPropertyMatcher.checkingAllProperties();
      return this;
    }

    public static InnerStaticPojoMatcher isInnerStaticPojoLike(
        final SimplePojo.InnerStaticPojo reference) {
      return ReferenceMatching.matcherFor(reference, InnerStaticPojoMatcher.class, InnerStaticPojoMatcher::new, matcher -> matcher.presetFrom(reference));
    }

    private void presetFrom(final SimplePojo.InnerStaticPojo reference) {
      beanPropertyMatcher.checkingAllProperties();
      beanPropertyMatcher.preset("innerStaticPojoProperty", ReferenceMatching.value(reference.getInnerStaticPojoProperty()));
    }

    @Override
    public BeanPropertyMatcher<SimplePojo.InnerStaticPojo> beanPropertyMatcher() {
      return beanPropertyMatcher;
    }

    @Override
    public void describeTo(final Description description) {
      beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final SimplePojo.InnerStaticPojo item) {
      return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final SimplePojo.InnerStaticPojo item,
        final Description description) {
      beanPropertyMatcher.describeMismatch(item, description);
    }

    public static InnerStaticPojoMatcher isInnerStaticPojo() {
      return new InnerStaticPojoMatcher();
    }

    @Generated(
        value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
        date = "2020-01-01T00:00"
    )
    public static class InnerInnerStaticPojoMatcher extends TypeSafeMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo> implements GeneratedBeanMatcher {
      private final BeanPropertyMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo> beanPropertyMatcher;

      public InnerInnerStaticPojoMatcher() {
        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo>(SimplePojo.InnerStaticPojo.InnerInnerStaticPojo.class, "class");
      }

      public InnerInnerStaticPojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
        beanPropertyMatcher.with("class", matcher);
        return this;
      }

      public InnerInnerStaticPojoMatcher resetClass() {
        beanPropertyMatcher.reset("class");
        return this;
      }

      public InnerInnerStaticPojoMatcher withClass(final Class<?> value) {
        beanPropertyMatcher.with("class", Matchers.equalTo(value));
        return this;
      }

      public InnerInnerStaticPojoMatcher ignoringClass() {
        beanPropertyMatcher.ignore("class");
        return this;
      }

      public InnerInnerStaticPojoMatcher checkingAllProperties() {
        beanPropertyMatcher.checkingAllProperties();
        return this;
      }

      public static InnerInnerStaticPojoMatcher isInnerInnerStaticPojoLike(
          final SimplePojo.InnerStaticPojo.InnerInnerStaticPojo reference) {
        return ReferenceMatching.matcherFor(reference, InnerInnerStaticPojoMatcher.class, InnerInnerStaticPojoMatcher::new, matcher -> matcher.presetFrom());
      }

      private void presetFrom() {
        beanPropertyMatcher.checkingAllProperties();
      }

      @Override
      public BeanPropertyMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo> beanPropertyMatcher(
          ) {
        return beanPropertyMatcher;
      }

      @Override
      public void describeTo(final Description description) {
        beanPropertyMatcher.describeTo(description);
      }

      @Override
      protected boolean matchesSafely(final SimplePojo.InnerStaticPojo.InnerInnerStaticPojo item) {
        return beanPropertyMatcher.matches(item);
      }

      @Override
      protected void describeMismatchSafely(
          final SimplePojo.InnerStaticPojo.InnerInnerStaticPojo item,
          final Description description) {
        beanPropertyMatcher.describeMismatch(item, description);
      }

      public static InnerInnerStaticPojoMatcher isInnerInnerStaticPojo() {
        return new InnerInnerStaticPojoMatcher();
      }
    }
  }
}
