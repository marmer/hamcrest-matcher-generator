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
public class MarkerMatcher extends TypeSafeMatcher<Marker> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<Marker> beanPropertyMatcher;

  public MarkerMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<Marker>(Marker.class, "class");
  }

  public MarkerMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public MarkerMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public MarkerMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public MarkerMatcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public MarkerMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  public static MarkerMatcher isMarkerLike(final Marker reference) {
    return ReferenceMatching.matcherFor(reference, MarkerMatcher.class, MarkerMatcher::new, matcher -> matcher.presetFrom());
  }

  private void presetFrom() {
    beanPropertyMatcher.checkingAllProperties();
  }

  @Override
  public BeanPropertyMatcher<Marker> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final Marker item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final Marker item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static MarkerMatcher isMarker() {
    return new MarkerMatcher();
  }
}
