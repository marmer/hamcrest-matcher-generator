/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Walks a value and its matchers along property paths ({@code customer.address.city}, {@code
 * lines[1].quantity}, {@code bySku["B-2"]}) and renders what it finds. Shared by the {@link
 * BeanPropertyMatcher} and the collection matchers, which hand values and matchers to each other.
 * Three kinds of matcher meet here: generated matchers (flattened into dot paths), collection
 * matchers (flattened into bracketed paths) and any other matcher (one check, one failure line).
 */
@NullMarked
final class PathWalk {
    static final String FAILURE_SEPARATOR = "\n          "; // aligns with Hamcrest's "     but: "

    /**
     * Depth of the calls a generated matcher makes into the foreign (non-generated) matchers it
     * holds. Above zero, a generated matcher is part of a Hamcrest matcher such as {@code
     * contains(..)} and renders on one line.
     */
    private static final ThreadLocal<int[]> FOREIGN_CALL_DEPTH =
            ThreadLocal.withInitial(() -> new int[1]);

    private PathWalk() {}

    /** A single check: a matcher for the value at a path. */
    record Check(String path, Matcher<?> matcher) {}

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

    static @Nullable BeanPropertyMatcher<?> generatedOrNull(final Matcher<?> matcher) {
        return matcher instanceof GeneratedBeanMatcher
                ? ((GeneratedBeanMatcher) matcher).beanPropertyMatcher()
                : null;
    }

    // ---- paths

    static String withPath(final String path, final String text) {
        return path.isEmpty() ? text : path + ": " + text;
    }

    /** The path of a bracketed segment: {@code lines[1]}, {@code bySku["B-2"]}. */
    static String segment(final String path, final String text) {
        return path + "[" + text + "]";
    }

    static String indexed(final String path, final int index) {
        return segment(path, String.valueOf(index));
    }

    // ---- rendering

    static String describeMatcher(final Matcher<?> matcher) {
        return callingForeign(() -> PropertyDescription.describe(matcher));
    }

    static String describeChecks(final List<Check> checks) {
        return checks.stream()
                .map(check -> check.path() + " " + describeMatcher(check.matcher()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Describes an expectation. A generated matcher used as a leaf (its property is not an instance
     * of the matched type, often {@code null}) is described on one line, not as a block.
     */
    static String describeExpected(final Matcher<?> matcher) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (nested == null) {
            return describeMatcher(matcher);
        }
        final List<Check> checks = new ArrayList<>();
        nested.collectChecks("", checks);
        final String type = articleFor(nested.typeName()) + nested.typeName();
        return checks.isEmpty() ? type : type + " with " + describeChecks(checks);
    }

    private static String articleFor(final String name) {
        return "AEIOUaeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ";
    }

    // ---- walking

    /**
     * Adds the failures of one value checked by one matcher. The matcher is a generated matcher
     * (flattened into dot paths), a collection matcher (flattened into bracketed paths) or any
     * other matcher.
     */
    static void collectValueFailures(
            final Matcher<?> matcher,
            final @Nullable Object value,
            final String path,
            final MismatchReport report) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (nested != null && value != null && nested.accepts(value)) {
            nested.collectFailures(value, path + ".", report);
        } else if (nested != null) {
            report.fail(
                    path,
                    "expected "
                            + describeExpected(matcher)
                            + " but was "
                            + PropertyDescription.describeValue(value));
        } else if (matcher instanceof PathAwareMatcher) {
            ((PathAwareMatcher) matcher).collectFailuresOf(value, path, report);
        } else if (!matchesQuietly(matcher, value)) {
            final PropertyDescription mismatch = new PropertyDescription();
            callingForeign(
                    () -> {
                        matcher.describeMismatch(value, mismatch);
                        return null;
                    });
            report.fail(path, "expected " + describeExpected(matcher) + " but " + mismatch);
        }
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

    static boolean matchesQuietly(final Matcher<?> matcher, final @Nullable Object value) {
        return callingForeign(() -> matcher.matches(value));
    }

    /**
     * @return whether the value passes the checks of the matcher. A matcher with the completeness
     *     check is not asked for the properties it leaves unaddressed: they say nothing about
     *     whether the value is the one meant.
     */
    static boolean matchesByValues(final Matcher<?> matcher, final @Nullable Object value) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (nested != null || matcher instanceof PathAwareMatcher) {
            final MismatchReport report = new MismatchReport();
            collectValueFailures(matcher, value, "", report);
            return !report.hasFailures();
        }
        return matchesQuietly(matcher, value);
    }

    /**
     * @return the failures of a generated matcher for the item, without the unaddressed properties,
     *     or {@code null} if the matcher is no generated matcher or the item is of another type
     */
    static @Nullable List<String> differencesOf(
            final Matcher<?> matcher, final @Nullable Object item) {
        final BeanPropertyMatcher<?> nested = generatedOrNull(matcher);
        if (nested == null || item == null || !nested.accepts(item)) {
            return null;
        }
        final MismatchReport report = new MismatchReport();
        nested.collectFailures(item, "", report);
        return report.failures();
    }
}
