/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jspecify.annotations.NullMarked;

/**
 * Checks a list or array item by item: one matcher per item, in order, and no further items. A
 * mismatch names the size and the path of every failing item ({@code lines[1].quantity}).
 *
 * @param <E> Type of the items.
 */
@NullMarked
public final class ItemsInOrder<E> extends PathAwareMatcher {
    private final List<? extends Matcher<? super E>> itemMatchers;

    ItemsInOrder(final List<? extends Matcher<? super E>> itemMatchers) {
        this.itemMatchers = List.copyOf(itemMatchers);
    }

    @Override
    boolean appliesTo(final Object value) {
        return value instanceof List<?> || value.getClass().isArray();
    }

    @Override
    void collectFailures(final Object value, final String path, final MismatchReport report) {
        final List<?> items = elementsOf(value);
        if (items.size() != itemMatchers.size()) {
            report.fail(
                    path,
                    "expected " + count(itemMatchers.size()) + " but was " + count(items.size()));
        }
        for (var i = 0; i < Math.min(items.size(), itemMatchers.size()); i++) {
            PathWalk.collectValueFailures(
                    itemMatchers.get(i), items.get(i), PathWalk.indexed(path, i), report);
        }
    }

    @Override
    void collectChecks(final String path, final List<PathWalk.Check> checks) {
        if (itemMatchers.isEmpty()) {
            checks.add(new PathWalk.Check(path, this));
        }
        for (var i = 0; i < itemMatchers.size(); i++) {
            PathWalk.collectValueChecks(PathWalk.indexed(path, i), itemMatchers.get(i), checks);
        }
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(
                itemMatchers.isEmpty() ? "no items" : count(itemMatchers.size()) + " in order");
    }
}
