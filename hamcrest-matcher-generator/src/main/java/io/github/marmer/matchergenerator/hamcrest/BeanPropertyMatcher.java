/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    static final String FAILURE_SEPARATOR = "\n          "; // aligns with Hamcrest's "     but: "
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

    private final Class<? super T> expectedClass;

    public BeanPropertyMatcher(final Class<? super T> expectedClass) {
        this.expectedClass = expectedClass;
    }

    /**
     * @param expectedClass the type to match
     * @param propertyNames all properties of the type in declaration order, used to print values of
     *     the type from their properties instead of via {@code toString()}
     */
    public BeanPropertyMatcher(
            final Class<? super T> expectedClass, final String... propertyNames) {
        this(expectedClass);
        PropertyAccess.registerGeneratedType(expectedClass, List.of(propertyNames));
    }

    static boolean isInsideForeignMatcher() {
        return FOREIGN_CALL_DEPTH.get()[0] > 0;
    }

    static <R> R callingForeign(final Supplier<R> call) {
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

    static String describeMatcher(final Matcher<?> matcher) {
        return callingForeign(() -> PropertyDescription.describe(matcher));
    }

    private static String describeChecks(final List<Check> checks) {
        return checks.stream()
                .map(check -> check.path() + " " + describeMatcher(check.matcher()))
                .collect(Collectors.joining(", "));
    }

    static String withPath(final String path, final String text) {
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
                            + (checks.size() == 1 ? " property check" : " property checks"));
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

    public void reset(final String propertyName) {
        propertyChecks.remove(propertyName);
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
                            + PropertyDescription.describeValue(item)
                            + ")");
        }
        collectFailures(item, "", failures);
        return failures;
    }

    private void collectFailures(
            final Object item, final String prefix, final List<String> failures) {
        propertyChecks.forEach(
                (name, matchers) -> {
                    final Object value = PropertyAccess.read(item, name);
                    for (final Matcher<?> matcher : matchers) {
                        collectFailure(name, matcher, value, prefix + name, failures);
                    }
                });
    }

    private void collectFailure(
            final String name,
            final Matcher<?> matcher,
            final @Nullable Object value,
            final String path,
            final List<String> failures) {
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
        } else {
            collectValueFailures(matcher, value, path, failures);
        }
    }

    /**
     * Adds the failures of one value checked by one matcher. The matcher is a generated matcher
     * (flattened into dot paths), a collection matcher (flattened into indexed paths) or any other
     * matcher.
     */
    static void collectValueFailures(
            final Matcher<?> matcher,
            final @Nullable Object value,
            final String path,
            final List<String> failures) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (nested != null && nested.expectedClass.isInstance(value)) {
            nested.collectFailures(value, path + ".", failures);
        } else if (nested != null) {
            failures.add(
                    withPath(
                            path,
                            "expected "
                                    + describeExpected(matcher)
                                    + " but was "
                                    + PropertyDescription.describeValue(value)));
        } else if (matcher instanceof PathAwareMatcher) {
            ((PathAwareMatcher) matcher).collectFailuresOf(value, path, failures);
        } else if (!matchesQuietly(matcher, value)) {
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

    static boolean matchesQuietly(final Matcher<?> matcher, final @Nullable Object value) {
        return callingForeign(() -> matcher.matches(value));
    }

    /**
     * @return the failures of a generated matcher for the item, or {@code null} if the matcher is
     *     no generated matcher or the item is of another type
     */
    static @Nullable List<String> differencesOf(
            final Matcher<?> matcher, final @Nullable Object item) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (nested == null || !nested.expectedClass.isInstance(item)) {
            return null;
        }
        final List<String> differences = new ArrayList<>();
        nested.collectFailures(item, "", differences);
        return differences;
    }

    private void collectChecks(final String prefix, final List<Check> checks) {
        propertyChecks.forEach(
                (name, matchers) -> {
                    for (final Matcher<?> matcher : matchers) {
                        collectValueChecks(prefix + name, matcher, checks);
                    }
                });
    }

    static void collectValueChecks(
            final String path, final Matcher<?> matcher, final List<Check> checks) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (nested != null) {
            nested.collectChecks(path + ".", checks);
        } else if (matcher instanceof PathAwareMatcher) {
            ((PathAwareMatcher) matcher).collectChecks(path, checks);
        } else {
            checks.add(new Check(path, matcher));
        }
    }

    record Check(String path, Matcher<?> matcher) {}

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
