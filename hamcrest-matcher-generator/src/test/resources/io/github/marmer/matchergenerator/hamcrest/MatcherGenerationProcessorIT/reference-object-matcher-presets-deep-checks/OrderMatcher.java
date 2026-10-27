package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
import io.github.marmer.matchergenerator.hamcrest.ReferenceMatching;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class OrderMatcher extends TypeSafeMatcher<Order> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<Order> beanPropertyMatcher;

  public OrderMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<Order>(Order.class, "id", "quantity", "firstLine", "lines", "lineSet", "wildcardLines", "linesBySku", "skuByLine", "lineArray", "tags", "color", "marker", "detail", "class");
  }

  public OrderMatcher withId(final Matcher<? super String> matcher) {
    beanPropertyMatcher.with("id", matcher);
    return this;
  }

  public OrderMatcher withQuantity(final Matcher<? super Integer> matcher) {
    beanPropertyMatcher.with("quantity", matcher);
    return this;
  }

  public OrderMatcher withFirstLine(final Matcher<? super Line> matcher) {
    beanPropertyMatcher.with("firstLine", matcher);
    return this;
  }

  public OrderMatcher withLines(final Matcher<? super List<? extends Line>> matcher) {
    beanPropertyMatcher.with("lines", matcher);
    return this;
  }

  public OrderMatcher withLineSet(final Matcher<? super Set<? extends Line>> matcher) {
    beanPropertyMatcher.with("lineSet", matcher);
    return this;
  }

  public OrderMatcher withWildcardLines(final Matcher<? super Collection<? extends Line>> matcher) {
    beanPropertyMatcher.with("wildcardLines", matcher);
    return this;
  }

  public OrderMatcher withLinesBySku(
      final Matcher<? super Map<? extends String, ? extends Line>> matcher) {
    beanPropertyMatcher.with("linesBySku", matcher);
    return this;
  }

  public OrderMatcher withSkuByLine(
      final Matcher<? super Map<? extends Line, ? extends String>> matcher) {
    beanPropertyMatcher.with("skuByLine", matcher);
    return this;
  }

  public OrderMatcher withLineArray(final Matcher<? super Line[]> matcher) {
    beanPropertyMatcher.with("lineArray", matcher);
    return this;
  }

  public OrderMatcher withTags(final Matcher<? super List<? extends String>> matcher) {
    beanPropertyMatcher.with("tags", matcher);
    return this;
  }

  public OrderMatcher withColor(final Matcher<? super Color> matcher) {
    beanPropertyMatcher.with("color", matcher);
    return this;
  }

  public OrderMatcher withMarker(final Matcher<? super Marker> matcher) {
    beanPropertyMatcher.with("marker", matcher);
    return this;
  }

  public OrderMatcher withDetail(final Matcher<? super Order.Detail> matcher) {
    beanPropertyMatcher.with("detail", matcher);
    return this;
  }

  public OrderMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public OrderMatcher resetId() {
    beanPropertyMatcher.reset("id");
    return this;
  }

  public OrderMatcher resetQuantity() {
    beanPropertyMatcher.reset("quantity");
    return this;
  }

  public OrderMatcher resetFirstLine() {
    beanPropertyMatcher.reset("firstLine");
    return this;
  }

  public OrderMatcher resetLines() {
    beanPropertyMatcher.reset("lines");
    return this;
  }

  public OrderMatcher resetLineSet() {
    beanPropertyMatcher.reset("lineSet");
    return this;
  }

  public OrderMatcher resetWildcardLines() {
    beanPropertyMatcher.reset("wildcardLines");
    return this;
  }

  public OrderMatcher resetLinesBySku() {
    beanPropertyMatcher.reset("linesBySku");
    return this;
  }

  public OrderMatcher resetSkuByLine() {
    beanPropertyMatcher.reset("skuByLine");
    return this;
  }

  public OrderMatcher resetLineArray() {
    beanPropertyMatcher.reset("lineArray");
    return this;
  }

  public OrderMatcher resetTags() {
    beanPropertyMatcher.reset("tags");
    return this;
  }

  public OrderMatcher resetColor() {
    beanPropertyMatcher.reset("color");
    return this;
  }

  public OrderMatcher resetMarker() {
    beanPropertyMatcher.reset("marker");
    return this;
  }

  public OrderMatcher resetDetail() {
    beanPropertyMatcher.reset("detail");
    return this;
  }

  public OrderMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public OrderMatcher withId(final String value) {
    beanPropertyMatcher.with("id", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withQuantity(final int value) {
    beanPropertyMatcher.with("quantity", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withFirstLine(final Line value) {
    beanPropertyMatcher.with("firstLine", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withLines(final List<? extends Line> value) {
    beanPropertyMatcher.with("lines", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withLineSet(final Set<? extends Line> value) {
    beanPropertyMatcher.with("lineSet", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withWildcardLines(final Collection<? extends Line> value) {
    beanPropertyMatcher.with("wildcardLines", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withLinesBySku(final Map<? extends String, ? extends Line> value) {
    beanPropertyMatcher.with("linesBySku", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withSkuByLine(final Map<? extends Line, ? extends String> value) {
    beanPropertyMatcher.with("skuByLine", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withLineArray(final Line[] value) {
    beanPropertyMatcher.with("lineArray", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withTags(final List<? extends String> value) {
    beanPropertyMatcher.with("tags", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withColor(final Color value) {
    beanPropertyMatcher.with("color", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withMarker(final Marker value) {
    beanPropertyMatcher.with("marker", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withDetail(final Order.Detail value) {
    beanPropertyMatcher.with("detail", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher ignoringId() {
    beanPropertyMatcher.ignore("id");
    return this;
  }

  public OrderMatcher ignoringQuantity() {
    beanPropertyMatcher.ignore("quantity");
    return this;
  }

  public OrderMatcher ignoringFirstLine() {
    beanPropertyMatcher.ignore("firstLine");
    return this;
  }

  public OrderMatcher ignoringLines() {
    beanPropertyMatcher.ignore("lines");
    return this;
  }

  public OrderMatcher ignoringLineSet() {
    beanPropertyMatcher.ignore("lineSet");
    return this;
  }

  public OrderMatcher ignoringWildcardLines() {
    beanPropertyMatcher.ignore("wildcardLines");
    return this;
  }

  public OrderMatcher ignoringLinesBySku() {
    beanPropertyMatcher.ignore("linesBySku");
    return this;
  }

  public OrderMatcher ignoringSkuByLine() {
    beanPropertyMatcher.ignore("skuByLine");
    return this;
  }

  public OrderMatcher ignoringLineArray() {
    beanPropertyMatcher.ignore("lineArray");
    return this;
  }

  public OrderMatcher ignoringTags() {
    beanPropertyMatcher.ignore("tags");
    return this;
  }

  public OrderMatcher ignoringColor() {
    beanPropertyMatcher.ignore("color");
    return this;
  }

  public OrderMatcher ignoringMarker() {
    beanPropertyMatcher.ignore("marker");
    return this;
  }

  public OrderMatcher ignoringDetail() {
    beanPropertyMatcher.ignore("detail");
    return this;
  }

  public OrderMatcher ignoringClass() {
    beanPropertyMatcher.ignore("class");
    return this;
  }

  public OrderMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  public static OrderMatcher isOrderLike(final Order reference) {
    return ReferenceMatching.matcherFor(reference, OrderMatcher.class, OrderMatcher::new, matcher -> matcher.presetFrom(reference));
  }

  private void presetFrom(final Order reference) {
    beanPropertyMatcher.checkingAllProperties();
    beanPropertyMatcher.preset("id", ReferenceMatching.value(reference.getId()));
    beanPropertyMatcher.preset("quantity", ReferenceMatching.value(reference.getQuantity()));
    beanPropertyMatcher.preset("firstLine", ReferenceMatching.nested(reference.getFirstLine(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("lines", ReferenceMatching.list(reference.getLines(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("lineSet", ReferenceMatching.collection(reference.getLineSet(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("wildcardLines", ReferenceMatching.collection(reference.getWildcardLines(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("linesBySku", ReferenceMatching.map(reference.getLinesBySku(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("skuByLine", ReferenceMatching.value(reference.getSkuByLine()));
    beanPropertyMatcher.preset("lineArray", ReferenceMatching.array(reference.getLineArray(), LineMatcher::isLineLike));
    beanPropertyMatcher.preset("tags", ReferenceMatching.value(reference.getTags()));
    beanPropertyMatcher.preset("color", ReferenceMatching.value(reference.getColor()));
    beanPropertyMatcher.preset("marker", ReferenceMatching.value(reference.getMarker()));
    beanPropertyMatcher.preset("detail", ReferenceMatching.nested(reference.getDetail(), DetailMatcher::isDetailLike));
  }

  @Override
  public BeanPropertyMatcher<Order> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final Order item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final Order item, final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static OrderMatcher isOrder() {
    return new OrderMatcher();
  }

  @Generated(
      value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
      date = "2020-01-01T00:00"
  )
  public static class DetailMatcher extends TypeSafeMatcher<Order.Detail> implements GeneratedBeanMatcher {
    private final BeanPropertyMatcher<Order.Detail> beanPropertyMatcher;

    public DetailMatcher() {
      beanPropertyMatcher = new BeanPropertyMatcher<Order.Detail>(Order.Detail.class, "order", "class");
    }

    public DetailMatcher withOrder(final Matcher<? super Order> matcher) {
      beanPropertyMatcher.with("order", matcher);
      return this;
    }

    public DetailMatcher withClass(final Matcher<? super Class<?>> matcher) {
      beanPropertyMatcher.with("class", matcher);
      return this;
    }

    public DetailMatcher resetOrder() {
      beanPropertyMatcher.reset("order");
      return this;
    }

    public DetailMatcher resetClass() {
      beanPropertyMatcher.reset("class");
      return this;
    }

    public DetailMatcher withOrder(final Order value) {
      beanPropertyMatcher.with("order", Matchers.equalTo(value));
      return this;
    }

    public DetailMatcher withClass(final Class<?> value) {
      beanPropertyMatcher.with("class", Matchers.equalTo(value));
      return this;
    }

    public DetailMatcher ignoringOrder() {
      beanPropertyMatcher.ignore("order");
      return this;
    }

    public DetailMatcher ignoringClass() {
      beanPropertyMatcher.ignore("class");
      return this;
    }

    public DetailMatcher checkingAllProperties() {
      beanPropertyMatcher.checkingAllProperties();
      return this;
    }

    public static DetailMatcher isDetailLike(final Order.Detail reference) {
      return ReferenceMatching.matcherFor(reference, DetailMatcher.class, DetailMatcher::new, matcher -> matcher.presetFrom(reference));
    }

    private void presetFrom(final Order.Detail reference) {
      beanPropertyMatcher.checkingAllProperties();
      beanPropertyMatcher.preset("order", ReferenceMatching.nested(reference.getOrder(), OrderMatcher::isOrderLike));
    }

    @Override
    public BeanPropertyMatcher<Order.Detail> beanPropertyMatcher() {
      return beanPropertyMatcher;
    }

    @Override
    public void describeTo(final Description description) {
      beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final Order.Detail item) {
      return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final Order.Detail item, final Description description) {
      beanPropertyMatcher.describeMismatch(item, description);
    }

    public static DetailMatcher isDetail() {
      return new DetailMatcher();
    }
  }
}
