package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import io.github.marmer.matchergenerator.hamcrest.ReferenceMatching;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import some.lib.Flipped;
import some.lib.Index;
import some.lib.Lines;
import some.lib.Named;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class HolderMatcher extends TypeSafeMatcher<Holder> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<Holder> beanPropertyMatcher;

  public HolderMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<Holder>(Holder.class, "lines", "index", "named", "flipped", "class");
  }

  public HolderMatcher withLines(final Matcher<? super Lines> matcher) {
    beanPropertyMatcher.with("lines", matcher);
    return this;
  }

  public HolderMatcher withIndex(final Matcher<? super Index> matcher) {
    beanPropertyMatcher.with("index", matcher);
    return this;
  }

  public HolderMatcher withNamed(final Matcher<? super Named<? extends Line>> matcher) {
    beanPropertyMatcher.with("named", matcher);
    return this;
  }

  public HolderMatcher withFlipped(
      final Matcher<? super Flipped<? extends Line, ? extends String>> matcher) {
    beanPropertyMatcher.with("flipped", matcher);
    return this;
  }

  public HolderMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public HolderMatcher resetLines() {
    beanPropertyMatcher.reset("lines");
    return this;
  }

  public HolderMatcher resetIndex() {
    beanPropertyMatcher.reset("index");
    return this;
  }

  public HolderMatcher resetNamed() {
    beanPropertyMatcher.reset("named");
    return this;
  }

  public HolderMatcher resetFlipped() {
    beanPropertyMatcher.reset("flipped");
    return this;
  }

  public HolderMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public HolderMatcher withLines(final Lines value) {
    beanPropertyMatcher.with("lines", Matchers.equalTo(value));
    return this;
  }

  public HolderMatcher withIndex(final Index value) {
    beanPropertyMatcher.with("index", Matchers.equalTo(value));
    return this;
  }

  public HolderMatcher withNamed(final Named<? extends Line> value) {
    beanPropertyMatcher.with("named", Matchers.equalTo(value));
    return this;
  }

  public HolderMatcher withFlipped(final Flipped<? extends Line, ? extends String> value) {
    beanPropertyMatcher.with("flipped", Matchers.equalTo(value));
    return this;
  }

  public HolderMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public HolderMatcher ignoringLines() {
    beanPropertyMatcher.ignore("lines");
    return this;
  }

  public HolderMatcher ignoringIndex() {
    beanPropertyMatcher.ignore("index");
    return this;
  }

  public HolderMatcher ignoringNamed() {
    beanPropertyMatcher.ignore("named");
    return this;
  }

  public HolderMatcher ignoringFlipped() {
    beanPropertyMatcher.ignore("flipped");
    return this;
  }

  public HolderMatcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public HolderMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  public static HolderMatcher isHolderLike(final Holder reference) {
    return ReferenceMatching.matcherFor(reference, HolderMatcher.class, HolderMatcher::new, matcher -> matcher.presetFrom(reference));
  }

  private void presetFrom(final Holder reference) {
    beanPropertyMatcher.checkingAllProperties();
    beanPropertyMatcher.preset("lines", ReferenceMatching.list(reference.getLines(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("index", ReferenceMatching.map(reference.getIndex(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("named", ReferenceMatching.collection(reference.getNamed(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("flipped", ReferenceMatching.map(reference.getFlipped(), LineMatcher::isLineLike));
  }

  @Override
  public BeanPropertyMatcher<Holder> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final Holder item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final Holder item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static HolderMatcher isHolder() {
    return new HolderMatcher();
  }
}
