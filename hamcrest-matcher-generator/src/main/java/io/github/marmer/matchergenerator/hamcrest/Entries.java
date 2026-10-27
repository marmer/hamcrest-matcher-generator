/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Checks a map: exactly these keys, each value checked by its matcher. Keys are compared by {@code
 * equals}. A mismatch names missing and unexpected keys and the path of every differing value
 * ({@code bySku["B-2"].quantity}).
 *
 * @param <K> Type of the keys.
 * @param <V> Type of the values.
 */
@NullMarked
public final class Entries<K, V> extends PathAwareMatcher {
    private final Map<K, ? extends Matcher<? super V>> valueMatchers;

    Entries(final Map<K, ? extends Matcher<? super V>> valueMatchers) {
        this.valueMatchers = new LinkedHashMap<>(valueMatchers);
    }

    /** Strings are quoted, other keys are not: {@code "B-2"}, {@code RED}, {@code 42}. */
    private static String keyText(final @Nullable Object key) {
        return key instanceof String || PropertyDescription.isGeneratedType(key)
                ? PropertyDescription.describeValue(key)
                : String.valueOf(key);
    }

    @Override
    boolean appliesTo(final Object value) {
        return value instanceof Map<?, ?>;
    }

    @Override
    void collectFailures(final Object value, final String path, final List<String> failures) {
        final Map<?, ?> actual = (Map<?, ?>) value;
        valueMatchers.forEach(
                (key, matcher) -> {
                    if (actual.containsKey(key)) {
                        BeanPropertyMatcher.collectValueFailures(
                                matcher,
                                actual.get(key),
                                path + "[" + keyText(key) + "]",
                                failures);
                    } else {
                        failures.add(
                                BeanPropertyMatcher.withPath(
                                        path,
                                        "missing key "
                                                + keyText(key)
                                                + " with "
                                                + BeanPropertyMatcher.describeMatcher(matcher)));
                    }
                });
        actual.forEach(
                (key, actualValue) -> {
                    if (!valueMatchers.containsKey(key)) {
                        failures.add(
                                BeanPropertyMatcher.withPath(
                                        path,
                                        "unexpected key "
                                                + keyText(key)
                                                + " with "
                                                + PropertyDescription.describeValue(actualValue)));
                    }
                });
    }

    @Override
    void collectChecks(final String path, final List<BeanPropertyMatcher.Check> checks) {
        if (valueMatchers.isEmpty()) {
            checks.add(new BeanPropertyMatcher.Check(path, this));
        }
        valueMatchers.forEach(
                (key, matcher) ->
                        BeanPropertyMatcher.collectValueChecks(
                                path + "[" + keyText(key) + "]", matcher, checks));
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(
                valueMatchers.isEmpty()
                        ? "an empty map"
                        : "a map with exactly the keys "
                                + valueMatchers.keySet().stream()
                                        .map(Entries::keyText)
                                        .collect(Collectors.joining(", ")));
    }
}
