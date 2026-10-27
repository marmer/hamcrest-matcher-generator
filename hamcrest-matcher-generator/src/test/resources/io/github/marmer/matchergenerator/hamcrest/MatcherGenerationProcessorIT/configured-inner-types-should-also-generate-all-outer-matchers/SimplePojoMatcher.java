package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import java.lang.Override;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;

  public SimplePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
  }

  public SimplePojoMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
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
  public static class InnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType> implements GeneratedBeanMatcher {
    private final BeanPropertyMatcher<SimplePojo.InnerType> beanPropertyMatcher;

    public InnerTypeMatcher() {
      beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType>(SimplePojo.InnerType.class);
    }

    public InnerTypeMatcher checkingAllProperties() {
      beanPropertyMatcher.checkingAllProperties();
      return this;
    }

    @Override
    public BeanPropertyMatcher<SimplePojo.InnerType> beanPropertyMatcher() {
      return beanPropertyMatcher;
    }

    @Override
    public void describeTo(final Description description) {
      beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final SimplePojo.InnerType item) {
      return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final SimplePojo.InnerType item,
        final Description description) {
      beanPropertyMatcher.describeMismatch(item, description);
    }

    public static InnerTypeMatcher isInnerType() {
      return new InnerTypeMatcher();
    }

    @Generated(
        value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
        date = "2020-01-01T00:00"
    )
    public static class InnerInnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType.InnerInnerType> implements GeneratedBeanMatcher {
      private final BeanPropertyMatcher<SimplePojo.InnerType.InnerInnerType> beanPropertyMatcher;

      public InnerInnerTypeMatcher() {
        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType.InnerInnerType>(SimplePojo.InnerType.InnerInnerType.class);
      }

      public InnerInnerTypeMatcher checkingAllProperties() {
        beanPropertyMatcher.checkingAllProperties();
        return this;
      }

      @Override
      public BeanPropertyMatcher<SimplePojo.InnerType.InnerInnerType> beanPropertyMatcher() {
        return beanPropertyMatcher;
      }

      @Override
      public void describeTo(final Description description) {
        beanPropertyMatcher.describeTo(description);
      }

      @Override
      protected boolean matchesSafely(final SimplePojo.InnerType.InnerInnerType item) {
        return beanPropertyMatcher.matches(item);
      }

      @Override
      protected void describeMismatchSafely(final SimplePojo.InnerType.InnerInnerType item,
          final Description description) {
        beanPropertyMatcher.describeMismatch(item, description);
      }

      public static InnerInnerTypeMatcher isInnerInnerType() {
        return new InnerInnerTypeMatcher();
      }
    }
  }
}
