/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Matcher for beans.
 *
 * <p>A mismatch lists only the failing checks, one line each, with property path, expected and
 * actual value. Generated matchers used as property checks are flattened into dot paths.
 *
 * @param <T> Type of the bean.
 * @author marmer
 * @since 13.06.2017
 */
@NullMarked
public class BeanPropertyMatcher<T> extends TypeSafeMatcher<T> {
    private static final String FAILURE_SEPARATOR =
            "\n          "; // aligns with Hamcrest's "     but: "
    private static final Matcher<Object> PROPERTY_EXISTS = new PropertyExists();

    /**
     * Depth of the calls a generated matcher makes into the foreign (non-generated) matchers it
     * holds. Above zero, a generated matcher is part of a Hamcrest matcher such as {@code
     * contains(..)} and renders on one line.
     */
    private static final ThreadLocal<int[]> FOREIGN_CALL_DEPTH =
            ThreadLocal.withInitial(() -> new int[1]);

    @SuppressWarnings("squid:S2293")
    private final Map<String, List<Matcher<?>>> propertyChecks = new LinkedHashMap<>();

    private final Set<String> ignoredProperties = new LinkedHashSet<>();

    private final Class<? super T> expectedClass;

    private final List<String> propertyNames;

    private boolean checkingAllProperties;

    public BeanPropertyMatcher(final Class<? super T> expectedClass) {
        this.expectedClass = expectedClass;
        this.propertyNames = List.of();
    }

    /**
     * @param expectedClass the type to match
     * @param propertyNames all properties of the type in declaration order, used to print values of
     *     the type from their properties instead of via {@code toString()} and to find the
     *     properties a completeness check misses
     */
    public BeanPropertyMatcher(
            final Class<? super T> expectedClass, final String... propertyNames) {
        this.expectedClass = expectedClass;
        this.propertyNames = List.of(propertyNames);
        PropertyAccess.registerGeneratedType(expectedClass, this.propertyNames);
    }

    private static boolean isInsideForeignMatcher() {
        return FOREIGN_CALL_DEPTH.get()[0] > 0;
    }

    private static <R> R callingForeign(final Supplier<R> call) {
        final int[] depth = FOREIGN_CALL_DEPTH.get();
        depth[0]++;
        try {
            return call.get();
        } finally {
            depth[0]--;
        }
    }

    private static @Nullable BeanPropertyMatcher<?> generatedOrNull(final Matcher<?> matcher) {
        return matcher instanceof GeneratedBeanMatcher
                ? ((GeneratedBeanMatcher) matcher).beanPropertyMatcher()
                : null;
    }

    private static String describeValue(final @Nullable Object value) {
        return PropertyDescription.describeValue(value);
    }

    private static String describeMatcher(final Matcher<?> matcher) {
        return callingForeign(() -> PropertyDescription.describe(matcher));
    }

    private static String describeChecks(final List<Check> checks) {
        return checks.stream()
                .map(check -> check.path() + " " + describeMatcher(check.matcher()))
                .collect(Collectors.joining(", "));
    }

    private static String withPath(final String path, final String text) {
        return path.isEmpty() ? text : path + ": " + text;
    }

    private static String articleFor(final String name) {
        return "AEIOUaeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ";
    }

    /**
     * Describes an expectation. A generated matcher used as a leaf (its property is not an instance
     * of the matched type, often {@code null}) is described on one line, not as a block.
     */
    private static String describeExpected(final Matcher<?> matcher) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (nested == null) {
            return describeMatcher(matcher);
        }
        final List<Check> checks = new ArrayList<>();
        nested.collectChecks("", checks);
        final String type = articleFor(nested.typeName()) + nested.typeName();
        return checks.isEmpty() ? type : type + " with " + describeChecks(checks);
    }

    @Override
    public void describeTo(final Description description) {
        final List<Check> checks = new ArrayList<>();
        collectChecks("", checks);
        if (isInsideForeignMatcher()) {
            description.appendText(typeName() + " {" + describeChecks(checks) + "}");
        } else {
            description.appendText(
                    typeName()
                            + " with "
                            + checks.size()
                            + (checks.size() == 1 ? " property check" : " property checks")
                            + (checkingAllProperties ? ", every property checked or ignored" : ""));
        }
    }

    @Override
    protected boolean matchesSafely(final T item) {
        return failuresOf(item).isEmpty();
    }

    public BeanPropertyMatcher<T> with(final String propertyName, final Matcher<?> matcher) {
        propertyChecks.computeIfAbsent(propertyName, key -> new ArrayList<>()).add(matcher);
        return this;
    }

    public BeanPropertyMatcher<T> with(final String propertyName) {
        return with(propertyName, PROPERTY_EXISTS);
    }

    /**
     * Consciously excludes a property from the completeness check. Ignoring does not check the
     * property.
     */
    public BeanPropertyMatcher<T> ignore(final String propertyName) {
        ignoredProperties.add(propertyName);
        return this;
    }

    /**
     * Opts this matcher into the completeness check: every property must be checked or ignored.
     * Applies to this matcher only, not to the matchers nested in it.
     */
    public BeanPropertyMatcher<T> checkingAllProperties() {
        checkingAllProperties = true;
        return this;
    }

    public void reset(final String propertyName) {
        propertyChecks.remove(propertyName);
        ignoredProperties.remove(propertyName);
    }

    @Override
    protected void describeMismatchSafely(final T item, final Description mismatchDescription) {
        final List<String> failures = failuresOf(item);
        mismatchDescription.appendText(
                isInsideForeignMatcher()
                        ? typeName() + " {" + String.join(", ", failures) + "}"
                        : String.join(FAILURE_SEPARATOR, failures));
    }

    private String typeName() {
        return expectedClass.getSimpleName();
    }

    private List<String> failuresOf(final Object item) {
        final List<String> failures = new ArrayList<>();
        if (!expectedClass.isInstance(item)) {
            failures.add(
                    "expected an instance of "
                            + expectedClass.getName()
                            + " but was a "
                            + item.getClass().getName()
                            + " ("
                            + describeValue(item)
                            + ")");
        }
        final List<String> unaddressed = new ArrayList<>();
        collectFailures(item, "", failures, unaddressed);
        if (!unaddressed.isEmpty()) {
            failures.add("neither checked nor ignored: " + String.join(", ", unaddressed));
        }
        return failures;
    }

    private void collectFailures(
            final Object item,
            final String prefix,
            final List<String> failures,
            final List<String> unaddressed) {
        if (checkingAllProperties) {
            propertyNames.stream()
                    .filter(name -> !isAddressed(name))
                    .forEach(name -> unaddressed.add(prefix + name));
        }
        propertyChecks.forEach(
                (name, matchers) -> {
                    final Object value = PropertyAccess.read(item, name);
                    for (final Matcher<?> matcher : matchers) {
                        collectFailure(name, matcher, value, prefix + name, failures, unaddressed);
                    }
                });
    }

    private boolean isAddressed(final String name) {
        return propertyChecks.containsKey(name) || ignoredProperties.contains(name);
    }

    private void collectFailure(
            final String name,
            final Matcher<?> matcher,
            final @Nullable Object value,
            final String path,
            final List<String> failures,
            final List<String> unaddressed) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (value == PropertyAccess.NO_SUCH_PROPERTY) {
            failures.add(
                    withPath(
                            path,
                            "expected "
                                    + describeExpected(matcher)
                                    + " but "
                                    + typeName()
                                    + " has no property \""
                                    + name
                                    + "\""));
        } else if (nested != null && nested.expectedClass.isInstance(value)) {
            nested.collectFailures(value, path + ".", failures, unaddressed);
        } else if (nested != null) {
            failures.add(
                    withPath(
                            path,
                            "expected "
                                    + describeExpected(matcher)
                                    + " but was "
                                    + describeValue(value)));
        } else if (!callingForeign(() -> matcher.matches(value))) {
            final PropertyDescription mismatch = new PropertyDescription();
            callingForeign(
                    () -> {
                        matcher.describeMismatch(value, mismatch);
                        return null;
                    });
            failures.add(
                    withPath(path, "expected " + describeExpected(matcher) + " but " + mismatch));
        }
    }

    private void collectChecks(final String prefix, final List<Check> checks) {
        propertyChecks.forEach(
                (name, matchers) -> {
                    for (final Matcher<?> matcher : matchers) {
                        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
                        if (nested != null) {
                            nested.collectChecks(prefix + name + ".", checks);
                        } else {
                            checks.add(new Check(prefix + name, matcher));
                        }
                    }
                });
    }

    private record Check(String path, Matcher<?> matcher) {}

    private static final class PropertyExists extends BaseMatcher<Object> {
        @Override
        public boolean matches(final @Nullable Object actual) {
            return true;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("the property to exist");
        }
    }
}
