/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Checks a collection or array without regard to the order: every matcher needs an item of its own,
 * and no further items are allowed. A mismatch names the missing items, for a missing generated
 * type the closest candidate with its differing properties, and the unexpected items.
 *
 * @param <E> Type of the items.
 */
@NullMarked
public final class ItemsInAnyOrder<E> extends PathAwareMatcher {
    private static final int UNASSIGNED = -1;

    private final List<? extends Matcher<? super E>> itemMatchers;

    ItemsInAnyOrder(final List<? extends Matcher<? super E>> itemMatchers) {
        this.itemMatchers = List.copyOf(itemMatchers);
    }

    @Override
    boolean appliesTo(final Object value) {
        return value instanceof Collection<?> || value.getClass().isArray();
    }

    @Override
    void collectFailures(final Object value, final String path, final MismatchReport report) {
        final List<?> items = elementsOf(value);
        final int[] matcherOfItem = assignMatchersToItems(items);
        final boolean[] matcherAssigned = new boolean[itemMatchers.size()];
        final List<@Nullable Object> unusedItems = new ArrayList<>();
        for (var item = 0; item < items.size(); item++) {
            if (matcherOfItem[item] == UNASSIGNED) {
                unusedItems.add(items.get(item));
            } else {
                matcherAssigned[matcherOfItem[item]] = true;
                // the item passed the value checks: this only collects what it leaves unaddressed
                PathWalk.collectValueFailures(
                        itemMatchers.get(matcherOfItem[item]),
                        items.get(item),
                        PathWalk.indexed(path, item),
                        report);
            }
        }
        for (var matcher = 0; matcher < itemMatchers.size(); matcher++) {
            if (!matcherAssigned[matcher]) {
                report.fail(
                        path,
                        describeMissingAndTakeClosest(itemMatchers.get(matcher), unusedItems));
            }
        }
        for (final Object unused : unusedItems) {
            report.fail(path, "unexpected " + PropertyDescription.describeValue(unused));
        }
    }

    /**
     * Finds a maximum assignment of items to matchers, one item per matcher, so that the result
     * does not depend on the order in which matchers happen to claim items. A matcher fits an item
     * by the value checks alone: what a completeness-checked matcher leaves unaddressed does not
     * make an item another one.
     *
     * @return per item the index of its matcher or {@link #UNASSIGNED}
     */
    private int[] assignMatchersToItems(final List<?> items) {
        final boolean[][] fits = new boolean[itemMatchers.size()][items.size()];
        for (var matcher = 0; matcher < itemMatchers.size(); matcher++) {
            for (var item = 0; item < items.size(); item++) {
                fits[matcher][item] =
                        PathWalk.matchesByValues(itemMatchers.get(matcher), items.get(item));
            }
        }
        final int[] matcherOfItem = new int[items.size()];
        Arrays.fill(matcherOfItem, UNASSIGNED);
        for (var matcher = 0; matcher < itemMatchers.size(); matcher++) {
            claim(matcher, fits, new boolean[items.size()], matcherOfItem);
        }
        return matcherOfItem;
    }

    /**
     * The augmenting-path step of the maximum bipartite matching: lets the matcher claim a free
     * item it fits or take over an item from a matcher that can move on to another one.
     *
     * @return whether the matcher got an item
     */
    private static boolean claim(
            final int matcher,
            final boolean[][] fits,
            final boolean[] visited,
            final int[] matcherOfItem) {
        for (var item = 0; item < matcherOfItem.length; item++) {
            if (fits[matcher][item] && !visited[item]) {
                visited[item] = true;
                if (matcherOfItem[item] == UNASSIGNED
                        || claim(matcherOfItem[item], fits, visited, matcherOfItem)) {
                    matcherOfItem[item] = matcher;
                    return true;
                }
            }
        }
        return false;
    }

    /** Describes a missing item. The closest candidate is taken out of the unused items. */
    private static String describeMissingAndTakeClosest(
            final Matcher<?> matcher, final List<@Nullable Object> unused) {
        final String missing = "missing " + PathWalk.describeMatcher(matcher);
        @Nullable Object closest = null;
        @Nullable List<String> closestDifferences = null;
        for (final Object candidate : unused) {
            final List<String> differences = PathWalk.differencesOf(matcher, candidate);
            if (differences != null
                    && (closestDifferences == null
                            || differences.size() < closestDifferences.size())) {
                closest = candidate;
                closestDifferences = differences;
            }
        }
        if (closestDifferences == null) {
            return missing;
        }
        unused.remove(closest);
        return missing
                + "; closest "
                + PropertyDescription.describeValue(closest)
                + " differs in "
                + String.join(", ", closestDifferences);
    }

    @Override
    void collectChecks(final String path, final List<PathWalk.Check> checks) {
        if (itemMatchers.isEmpty()) {
            checks.add(new PathWalk.Check(path, this));
        }
        // no path segment names an item of an unordered collection, so each matcher is one check
        for (final Matcher<?> matcher : itemMatchers) {
            checks.add(new PathWalk.Check(path, matcher));
        }
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(
                itemMatchers.isEmpty() ? "no items" : count(itemMatchers.size()) + " in any order");
    }
}
