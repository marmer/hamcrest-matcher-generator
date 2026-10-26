package my.base.pck.some.other.pck;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.lang.Override;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import some.other.pck.SomePojo;

@Generated(
    value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class SomePojoMatcher extends TypeSafeMatcher<SomePojo> {
  private final BeanPropertyMatcher<SomePojo> beanPropertyMatcher;

  public SomePojoMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SomePojo>(SomePojo.class);
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
      value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor",
      date = "2020-01-01T00:00"
  )
  public static class InnerInterfaceMatcher extends TypeSafeMatcher<SomePojo.InnerInterface> {
    private final BeanPropertyMatcher<SomePojo.InnerInterface> beanPropertyMatcher;

    public InnerInterfaceMatcher() {
      beanPropertyMatcher = new BeanPropertyMatcher<SomePojo.InnerInterface>(SomePojo.InnerInterface.class);
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
