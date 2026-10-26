package org.mockito;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.lang.Class;
import java.lang.Override;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor", date = "2020-01-01T00:00")
public class ArgumentMatchersMatcher extends TypeSafeMatcher<ArgumentMatchers> {
    private final BeanPropertyMatcher<ArgumentMatchers> beanPropertyMatcher;

    public ArgumentMatchersMatcher() {
        beanPropertyMatcher = new BeanPropertyMatcher<ArgumentMatchers>(ArgumentMatchers.class);
    }

    public ArgumentMatchersMatcher withClass(final Matcher<? super Class<?>> matcher) {
        beanPropertyMatcher.with("class", matcher);
        return this;
    }

    public ArgumentMatchersMatcher resetClass() {
        beanPropertyMatcher.reset("class");
        return this;
    }

    public ArgumentMatchersMatcher withClass(final Class<?> value) {
        beanPropertyMatcher.with("class", Matchers.equalTo(value));
        return this;
    }

    @Override
    public void describeTo(final Description description) {
        beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final ArgumentMatchers item) {
        return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final ArgumentMatchers item, final Description description) {
        beanPropertyMatcher.describeMismatch(item, description);
    }

    public static ArgumentMatchersMatcher isArgumentMatchers() {
        return new ArgumentMatchersMatcher();
    }
}
