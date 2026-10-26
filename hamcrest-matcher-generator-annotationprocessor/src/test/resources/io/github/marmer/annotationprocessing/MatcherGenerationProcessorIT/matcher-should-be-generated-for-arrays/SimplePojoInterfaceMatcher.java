package some.other.pck;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor", date = "2020-01-01T00:00")
public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {
    private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;

    public SimplePojoInterfaceMatcher() {
        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);
    }

    public SimplePojoInterfaceMatcher withSomeStringArray(final Matcher<? super String[]> matcher) {
        beanPropertyMatcher.with("someStringArray", matcher);
        return this;
    }

    public SimplePojoInterfaceMatcher withSomeMultidimensionalStringArray(final Matcher<? super String[][]> matcher) {
        beanPropertyMatcher.with("someMultidimensionalStringArray", matcher);
        return this;
    }

    public SimplePojoInterfaceMatcher withSomeInnerTypeArray(final Matcher<? super AnotherComplexType.SomeInnerType[]> matcher) {
        beanPropertyMatcher.with("someInnerTypeArray", matcher);
        return this;
    }

    public SimplePojoInterfaceMatcher withSomePrimitiveArray(final Matcher<? super byte[]> matcher) {
        beanPropertyMatcher.with("somePrimitiveArray", matcher);
        return this;
    }

    public SimplePojoInterfaceMatcher withSomeMultidimensionalPrimitiveArray(final Matcher<? super byte[][]> matcher) {
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

    public SimplePojoInterfaceMatcher withSomeInnerTypeArray(final AnotherComplexType.SomeInnerType[] value) {
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

    @Override
    public void describeTo(final Description description) {
        beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final SimplePojoInterface item) {
        return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final SimplePojoInterface item, final Description description) {
        beanPropertyMatcher.describeMismatch(item, description);
    }

    public static SimplePojoInterfaceMatcher isSimplePojoInterface() {
        return new SimplePojoInterfaceMatcher();
    }
}
