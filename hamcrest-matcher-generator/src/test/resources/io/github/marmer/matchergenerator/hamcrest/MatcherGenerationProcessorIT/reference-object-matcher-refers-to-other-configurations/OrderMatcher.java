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
import prefixed.some.other.pck.CustomerMatcher;

@Generated(
    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
    date = "2020-01-01T00:00"
)
public class OrderMatcher extends TypeSafeMatcher<Order> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<Order> beanPropertyMatcher;

  public OrderMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<Order>(Order.class, "customer", "class");
  }

  public OrderMatcher withCustomer(final Matcher<? super Customer> matcher) {
    beanPropertyMatcher.with("customer", matcher);
    return this;
  }

  public OrderMatcher withClass(final Matcher<? super Class<?>> matcher) {
    beanPropertyMatcher.with("class", matcher);
    return this;
  }

  public OrderMatcher resetCustomer() {
    beanPropertyMatcher.reset("customer");
    return this;
  }

  public OrderMatcher resetClass() {
    beanPropertyMatcher.reset("class");
    return this;
  }

  public OrderMatcher withCustomer(final Customer value) {
    beanPropertyMatcher.with("customer", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher withClass(final Class<?> value) {
    beanPropertyMatcher.with("class", Matchers.equalTo(value));
    return this;
  }

  public OrderMatcher ignoringCustomer() {
    beanPropertyMatcher.ignore("customer");
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
    beanPropertyMatcher.preset("customer", ReferenceMatching.nested(reference.getCustomer(), CustomerMatcher::isCustomerLike));
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
}
