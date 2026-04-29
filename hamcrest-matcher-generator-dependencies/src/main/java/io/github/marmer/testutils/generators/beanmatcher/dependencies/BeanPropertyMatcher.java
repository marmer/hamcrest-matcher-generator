package io.github.marmer.testutils.generators.beanmatcher.dependencies;

import static org.hamcrest.Matchers.allOf;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
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
    private final Class<? super T> expectedClass;

    public BeanPropertyMatcher(final Class<? super T> expectedClass) {
        this.expectedClass = expectedClass;
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
        addToHasPropertyMatcher(propertyName, buildPropertyMatcher(propertyName, matcher));
        return this;
    }

    @SuppressWarnings("unchecked")
    private Matcher<?> buildPropertyMatcher(String propertyName, Matcher<?> valueMatcher) {
        String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        boolean hasGetter = hasAccessibleMethod("get" + capitalized) || hasAccessibleMethod("is" + capitalized);
        if (!hasGetter && hasAccessibleMethod(propertyName)) {
            return new FeatureMatcher<T, Object>((Matcher<? super Object>) valueMatcher, propertyName, propertyName) {
                @Override
                protected Object featureValueOf(T actual) {
                    try {
                        return actual.getClass().getMethod(propertyName).invoke(actual);
                    } catch (ReflectiveOperationException e) {
                        throw new AssertionError("Could not read component '" + propertyName + "'", e);
                    }
                }
            };
        }
        return Matchers.hasProperty(propertyName, valueMatcher);
    }

    private boolean hasAccessibleMethod(String methodName) {
        try {
            Method m = expectedClass.getMethod(methodName);
            return m.getParameterCount() == 0;
        } catch (NoSuchMethodException e) {
            return false;
        }
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
