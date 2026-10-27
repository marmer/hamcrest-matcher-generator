/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Matchers for collection and map properties. Their mismatches carry the path of the failing item
 * or entry. Arrays are treated like lists. For a partial check of a map use Hamcrest's {@code
 * hasEntry}.
 */
@NullMarked
public final class CollectionMatchers {
    private CollectionMatchers() {}

    /**
     * @param itemMatchers one matcher per item, in the order of the items
     * @return a matcher for lists and arrays with exactly these items in this order
     */
    @SafeVarargs
    public static <E> ItemsInOrder<E> itemsInOrder(final Matcher<? super E>... itemMatchers) {
        return new ItemsInOrder<E>(Arrays.<Matcher<? super E>>asList(itemMatchers));
    }

    public static <E> ItemsInOrder<E> itemsInOrder(
            final List<? extends Matcher<? super E>> itemMatchers) {
        return new ItemsInOrder<>(itemMatchers);
    }

    /**
     * @param itemMatchers every matcher needs an item of its own
     * @return a matcher for collections and arrays with exactly these items in any order
     */
    @SafeVarargs
    public static <E> ItemsInAnyOrder<E> itemsInAnyOrder(final Matcher<? super E>... itemMatchers) {
        return new ItemsInAnyOrder<E>(Arrays.<Matcher<? super E>>asList(itemMatchers));
    }

    public static <E> ItemsInAnyOrder<E> itemsInAnyOrder(
            final List<? extends Matcher<? super E>> itemMatchers) {
        return new ItemsInAnyOrder<>(itemMatchers);
    }

    /**
     * @param valueMatchers the matcher for the value of every key
     * @return a matcher for maps with exactly these keys
     */
    public static <K, V> Entries<K, V> entries(
            final Map<K, ? extends Matcher<? super V>> valueMatchers) {
        return new Entries<>(valueMatchers);
    }

    public static <K, V> Entries<K, V> entries(final K key, final Matcher<? super V> matcher) {
        return entriesOf(key, matcher);
    }

    public static <K, V> Entries<K, V> entries(
            final K key1,
            final Matcher<? super V> matcher1,
            final K key2,
            final Matcher<? super V> matcher2) {
        return entriesOf(key1, matcher1, key2, matcher2);
    }

    public static <K, V> Entries<K, V> entries(
            final K key1,
            final Matcher<? super V> matcher1,
            final K key2,
            final Matcher<? super V> matcher2,
            final K key3,
            final Matcher<? super V> matcher3) {
        return entriesOf(key1, matcher1, key2, matcher2, key3, matcher3);
    }

    public static <K, V> Entries<K, V> entries(
            final K key1,
            final Matcher<? super V> matcher1,
            final K key2,
            final Matcher<? super V> matcher2,
            final K key3,
            final Matcher<? super V> matcher3,
            final K key4,
            final Matcher<? super V> matcher4) {
        return entriesOf(key1, matcher1, key2, matcher2, key3, matcher3, key4, matcher4);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Entries<K, V> entriesOf(final @Nullable Object... keysAndMatchers) {
        final Map<K, Matcher<? super V>> valueMatchers = new LinkedHashMap<>();
        for (var i = 0; i < keysAndMatchers.length; i += 2) {
            final K key = (K) keysAndMatchers[i];
            if (valueMatchers.put(key, (Matcher<? super V>) keysAndMatchers[i + 1]) != null) {
                throw new IllegalArgumentException("Key given twice: " + key);
            }
        }
        return new Entries<>(valueMatchers);
    }
}
