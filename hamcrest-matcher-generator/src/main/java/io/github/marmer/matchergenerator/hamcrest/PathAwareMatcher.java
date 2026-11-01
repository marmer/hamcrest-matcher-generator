/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base of the collection matchers. A generated matcher flattens them into paths ({@code
 * lines[1].quantity}) instead of describing them as a whole.
 */
@NullMarked
abstract class PathAwareMatcher extends BaseMatcher<Object> {
    static String count(final int size) {
        return size == 1 ? "1 item" : size + " items";
    }

    static List<Object> elementsOf(final Object collectionOrArray) {
        if (collectionOrArray instanceof Collection<?>) {
            return new ArrayList<>((Collection<?>) collectionOrArray);
        }
        final int length = Array.getLength(collectionOrArray);
        final List<Object> elements = new ArrayList<>(length);
        for (var i = 0; i < length; i++) {
            elements.add(Array.get(collectionOrArray, i));
        }
        return elements;
    }

    /**
     * @return whether the value is of a type this matcher can check
     */
    abstract boolean appliesTo(Object value);

    /**
     * Adds the failures for a value this matcher {@link #appliesTo applies to}. Unaddressed
     * properties of generated matchers in the items are added to the report as such.
     */
    abstract void collectFailures(Object value, String path, MismatchReport report);

    abstract void collectChecks(String path, List<PathWalk.Check> checks);

    final void collectFailuresOf(
            final @Nullable Object value, final String path, final MismatchReport report) {
        if (value != null && appliesTo(value)) {
            collectFailures(value, path, report);
        } else {
            report.fail(
                    path,
                    "expected "
                            + PathWalk.describeMatcher(this)
                            + " but was "
                            + PropertyDescription.describeValue(value));
        }
    }

    @Override
    public final boolean matches(final @Nullable Object actual) {
        return linesOf(actual).isEmpty();
    }

    @Override
    public final void describeMismatch(
            final @Nullable Object actual, final Description description) {
        description.appendText(
                String.join(
                        PathWalk.isInsideForeignMatcher() ? ", " : PathWalk.FAILURE_SEPARATOR,
                        linesOf(actual)));
    }

    private List<String> linesOf(final @Nullable Object actual) {
        final MismatchReport report = new MismatchReport();
        collectFailuresOf(actual, "", report);
        return report.lines();
    }
}
