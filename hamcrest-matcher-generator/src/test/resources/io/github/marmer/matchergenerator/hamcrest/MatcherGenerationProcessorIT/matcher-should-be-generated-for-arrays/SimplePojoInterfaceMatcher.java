package some.other.pck;

import io.github.marmer.matchergenerator.hamcrest.BeanPropertyMatcher;
import io.github.marmer.matchergenerator.hamcrest.GeneratedBeanMatcher;
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
public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> implements GeneratedBeanMatcher {
  private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;

  public SimplePojoInterfaceMatcher() {
    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class, "someStringArray", "someMultidimensionalStringArray", "someInnerTypeArray", "somePrimitiveArray", "someMultidimensionalPrimitiveArray");
  }

  public SimplePojoInterfaceMatcher withSomeStringArray(final Matcher<? super String[]> matcher) {
    beanPropertyMatcher.with("someStringArray", matcher);
    return this;
  }

  public SimplePojoInterfaceMatcher withSomeMultidimensionalStringArray(
      final Matcher<? super String[][]> matcher) {
    beanPropertyMatcher.with("someMultidimensionalStringArray", matcher);
    return this;
  }

  public SimplePojoInterfaceMatcher withSomeInnerTypeArray(
      final Matcher<? super AnotherComplexType.SomeInnerType[]> matcher) {
    beanPropertyMatcher.with("someInnerTypeArray", matcher);
    return this;
  }

  public SimplePojoInterfaceMatcher withSomePrimitiveArray(final Matcher<? super byte[]> matcher) {
    beanPropertyMatcher.with("somePrimitiveArray", matcher);
    return this;
  }

  public SimplePojoInterfaceMatcher withSomeMultidimensionalPrimitiveArray(
      final Matcher<? super byte[][]> matcher) {
    beanPropertyMatcher.with("someMultidimensionalPrimitiveArray", matcher);
    return this;
  }

  public SimplePojoInterfaceMatcher resetSomeStringArray() {
    beanPropertyMatcher.reset("someStringArray");
    return this;
  }

  public SimplePojoInterfaceMatcher resetSomeMultidimensionalStringArray() {
    beanPropertyMatcher.reset("someMultidimensionalStringArray");
    return this;
  }

  public SimplePojoInterfaceMatcher resetSomeInnerTypeArray() {
    beanPropertyMatcher.reset("someInnerTypeArray");
    return this;
  }

  public SimplePojoInterfaceMatcher resetSomePrimitiveArray() {
    beanPropertyMatcher.reset("somePrimitiveArray");
    return this;
  }

  public SimplePojoInterfaceMatcher resetSomeMultidimensionalPrimitiveArray() {
    beanPropertyMatcher.reset("someMultidimensionalPrimitiveArray");
    return this;
  }

  public SimplePojoInterfaceMatcher withSomeStringArray(final String[] value) {
    beanPropertyMatcher.with("someStringArray", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoInterfaceMatcher withSomeMultidimensionalStringArray(final String[][] value) {
    beanPropertyMatcher.with("someMultidimensionalStringArray", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoInterfaceMatcher withSomeInnerTypeArray(
      final AnotherComplexType.SomeInnerType[] value) {
    beanPropertyMatcher.with("someInnerTypeArray", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoInterfaceMatcher withSomePrimitiveArray(final byte[] value) {
    beanPropertyMatcher.with("somePrimitiveArray", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoInterfaceMatcher withSomeMultidimensionalPrimitiveArray(final byte[][] value) {
    beanPropertyMatcher.with("someMultidimensionalPrimitiveArray", Matchers.equalTo(value));
    return this;
  }

  public SimplePojoInterfaceMatcher ignoringSomeStringArray() {
    beanPropertyMatcher.ignore("someStringArray");
    return this;
  }

  public SimplePojoInterfaceMatcher ignoringSomeMultidimensionalStringArray() {
    beanPropertyMatcher.ignore("someMultidimensionalStringArray");
    return this;
  }

  public SimplePojoInterfaceMatcher ignoringSomeInnerTypeArray() {
    beanPropertyMatcher.ignore("someInnerTypeArray");
    return this;
  }

  public SimplePojoInterfaceMatcher ignoringSomePrimitiveArray() {
    beanPropertyMatcher.ignore("somePrimitiveArray");
    return this;
  }

  public SimplePojoInterfaceMatcher ignoringSomeMultidimensionalPrimitiveArray() {
    beanPropertyMatcher.ignore("someMultidimensionalPrimitiveArray");
    return this;
  }

  public SimplePojoInterfaceMatcher checkingAllProperties() {
    beanPropertyMatcher.checkingAllProperties();
    return this;
  }

  @Override
  public BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher() {
    return beanPropertyMatcher;
  }

  @Override
  public void describeTo(final Description description) {
    beanPropertyMatcher.describeTo(description);
  }

  @Override
  protected boolean matchesSafely(final SimplePojoInterface item) {
    return beanPropertyMatcher.matches(item);
  }

  @Override
  protected void describeMismatchSafely(final SimplePojoInterface item,
      final Description description) {
    beanPropertyMatcher.describeMismatch(item, description);
  }

  public static SimplePojoInterfaceMatcher isSimplePojoInterface() {
    return new SimplePojoInterfaceMatcher();
  }
}
