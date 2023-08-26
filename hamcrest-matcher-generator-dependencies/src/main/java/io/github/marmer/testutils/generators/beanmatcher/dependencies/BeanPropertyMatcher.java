package io.github.marmer.testutils.generators.beanmatcher.dependencies;

import static org.hamcrest.Matchers.allOf;

import java.util.*;
import java.util.stream.Collectors;

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
    private final Map<String, List<Matcher<?>>> hasPropertyMatcher = new LinkedHashMap<>();
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
        fullMatcher.addAll(hasPropertyMatcherToList());
        return allOf(fullMatcher.toArray(new Matcher[0]));
    }

    public BeanPropertyMatcher<T> with(final String propertyName, final Matcher<?> matcher) {
        addToHasPropertyMatcher(propertyName, Matchers.hasProperty(propertyName,matcher));
        return this;
    }

    public BeanPropertyMatcher<T> with(final String propertyName) {
        addToHasPropertyMatcher(propertyName, Matchers.hasProperty(propertyName));
        return this;
    }

    public void reset(final String propertyName) {
        hasPropertyMatcher.remove(propertyName);
    }

    @Override
    protected void describeMismatchSafely(final T item, final Description mismatchDescription) {
        var missmatchDescriptionAllreadyAdded = false;

        if (!instanceOfMatcher.matches(item)) {
            mismatchDescription.appendText("Is an instance of " + item.getClass());
            missmatchDescriptionAllreadyAdded = true;
        }

        for (final Matcher<?> matcher : hasPropertyMatcherToList()) {
            if (!matcher.matches(item)) {
                if (missmatchDescriptionAllreadyAdded) {
                    mismatchDescription.appendText(" and ");
                }

                matcher.describeMismatch(item, mismatchDescription);
                missmatchDescriptionAllreadyAdded = true;
            }
        }
    }

    private List<Matcher<?>> hasPropertyMatcherToList() {
        return hasPropertyMatcher.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private void addToHasPropertyMatcher(String propertyName, Matcher<?> matcher) {
        hasPropertyMatcher.computeIfAbsent(
                propertyName,
                key -> new ArrayList<>()
        ).add(matcher);
    }
}
