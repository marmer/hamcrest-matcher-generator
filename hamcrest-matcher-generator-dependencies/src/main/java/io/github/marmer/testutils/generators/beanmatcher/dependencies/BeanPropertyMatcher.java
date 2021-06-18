package io.github.marmer.testutils.generators.beanmatcher.dependencies;

import static org.hamcrest.Matchers.allOf;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;


/**
 * Matcher for beans.
 *
 * @param <T> Type of the bean.
 * @author marmer
 * @since 13.06.2017
 */
public class BeanPropertyMatcher<T> extends TypeSafeMatcher<T> {

    @SuppressWarnings("squid:S2293")
    private final List<Matcher<?>> hasPropertyMatcher = new ArrayList<>();
    private final Matcher<?> instanceOfMatcher;

    public BeanPropertyMatcher(final Class<? super T> expectedClass) {
        instanceOfMatcher = Matchers.instanceOf(expectedClass);
    }

    @Override
    public void describeTo(final Description description) {
        getFullInnerMatcher().describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final T item) {
        return getFullInnerMatcher().matches(item);
    }

    @SuppressWarnings("unchecked")
    private Matcher<?> getFullInnerMatcher() {
        @SuppressWarnings("squid:S2293") final List<Matcher<?>> fullMatcher = new ArrayList<>();
        fullMatcher.add(instanceOfMatcher);
        fullMatcher.addAll(hasPropertyMatcher);

        return allOf(fullMatcher.toArray(new Matcher[fullMatcher.size()]));
    }

    public BeanPropertyMatcher<T> with(final String propertyName, final Matcher<?> matcher) {
        hasPropertyMatcher.add(Matchers.hasProperty(propertyName, matcher));

        return this;
    }

    public BeanPropertyMatcher<T> with(final String propertyName) {
        hasPropertyMatcher.add(Matchers.hasProperty(propertyName));

        return this;
    }

    @Override
    protected void describeMismatchSafely(final T item, final Description mismatchDescription) {
        boolean missmatchDescriptionAllreadyAdded = false;

        if (!instanceOfMatcher.matches(item)) {
            mismatchDescription.appendText("Is an instance of " + item.getClass());
            missmatchDescriptionAllreadyAdded = true;
        }

        for (final Matcher<?> matcher : hasPropertyMatcher) {
            if (!matcher.matches(item)) {
                if (missmatchDescriptionAllreadyAdded) {
                    mismatchDescription.appendText(" and ");
                }

                matcher.describeMismatch(item, mismatchDescription);
                missmatchDescriptionAllreadyAdded = true;
            }
        }
    }
}
