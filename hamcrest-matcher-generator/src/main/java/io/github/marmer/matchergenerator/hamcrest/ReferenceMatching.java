/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * What the generated {@code isXLike(reference)} methods are made of: presets for the properties of
 * a reference object, deep for the types that have a generated matcher and plain {@code equalTo}
 * for everything else.
 *
 * <p>A reference graph may be cyclic. While one matcher is built, a reference object gets one
 * matcher only, which the object finds again on its way back to itself. The matchers then form the
 * same cycle as the objects, and the checking walks of the {@link BeanPropertyMatcher} count a pair
 * met again as equal.
 */
@NullMarked
public final class ReferenceMatching {
    private static final ThreadLocal<Map<Built, Object>> BUILDING = new ThreadLocal<>();

    private ReferenceMatching() {}

    /**
     * Builds the matcher for a reference object, or returns the one built for it earlier while the
     * outermost call is still running.
     *
     * @param reference the object the matcher is made for
     * @param matcherType the type of the matcher, since one object can be matched as different
     *     types
     * @param create creates the matcher without presets
     * @param preset presets the properties of the created matcher from the reference
     */
    @SuppressWarnings("unchecked")
    public static <M> M matcherFor(
            final Object reference,
            final Class<M> matcherType,
            final Supplier<M> create,
            final Consumer<M> preset) {
        final boolean outermost = BUILDING.get() == null;
        if (outermost) {
            BUILDING.set(new HashMap<>());
        }
        try {
            final Map<Built, Object> built = BUILDING.get();
            final Built key = new Built(reference, matcherType);
            final @Nullable Object known = built.get(key);
            if (known != null) {
                return (M) known;
            }
            final M matcher = create.get();
            built.put(key, matcher);
            preset.accept(matcher);
            return matcher;
        } finally {
            if (outermost) {
                BUILDING.remove();
            }
        }
    }

    /**
     * @return a matcher for a value that is compared by {@code equals}
     */
    public static Matcher<?> value(final @Nullable Object value) {
        return value == null ? Matchers.nullValue() : Matchers.equalTo(value);
    }

    /**
     * @param like creates the matcher for a value that is not {@code null}
     * @return a matcher for a value of a type with a generated matcher
     */
    public static <E> Matcher<?> nested(
            final @Nullable E value, final Function<? super E, ? extends Matcher<?>> like) {
        return value == null ? Matchers.nullValue() : like.apply(value);
    }

    /**
     * @return a matcher for a list of a type with a generated matcher, item by item in order
     */
    public static <E> Matcher<?> list(
            final @Nullable Collection<? extends E> items,
            final Function<? super E, ? extends Matcher<?>> like) {
        return items == null
                ? Matchers.nullValue()
                : CollectionMatchers.itemsInOrder(matchersOf(items, like));
    }

    /**
     * @return a matcher for a collection of a type with a generated matcher, in any order
     */
    public static <E> Matcher<?> collection(
            final @Nullable Collection<? extends E> items,
            final Function<? super E, ? extends Matcher<?>> like) {
        return items == null
                ? Matchers.nullValue()
                : CollectionMatchers.itemsInAnyOrder(matchersOf(items, like));
    }

    /**
     * @return a matcher for an array of a type with a generated matcher, treated like a list
     */
    public static <E> Matcher<?> array(
            final E @Nullable [] items, final Function<? super E, ? extends Matcher<?>> like) {
        return items == null ? Matchers.nullValue() : list(List.of(items), like);
    }

    /**
     * @return a matcher for a map with values of a type with a generated matcher and exactly the
     *     keys of the reference, which are compared by {@code equals}
     */
    public static <V> Matcher<?> map(
            final @Nullable Map<?, ? extends V> values,
            final Function<? super V, ? extends Matcher<?>> like) {
        if (values == null) {
            return Matchers.nullValue();
        }
        final Map<Object, Matcher<Object>> valueMatchers = new LinkedHashMap<>();
        values.forEach((key, value) -> valueMatchers.put(key, itemMatcher(value, like)));
        return CollectionMatchers.entries(valueMatchers);
    }

    private static <E> List<Matcher<Object>> matchersOf(
            final Collection<? extends E> items,
            final Function<? super E, ? extends Matcher<?>> like) {
        final List<Matcher<Object>> matchers = new ArrayList<>();
        items.forEach(item -> matchers.add(itemMatcher(item, like)));
        return matchers;
    }

    @SuppressWarnings("unchecked")
    private static <E> Matcher<Object> itemMatcher(
            final @Nullable E item, final Function<? super E, ? extends Matcher<?>> like) {
        return (Matcher<Object>) nested(item, like);
    }

    /** A reference object, compared by identity, and the matcher type it is matched as. */
    private static final class Built {
        private final Object reference;
        private final Class<?> matcherType;

        Built(final Object reference, final Class<?> matcherType) {
            this.reference = reference;
            this.matcherType = matcherType;
        }

        @Override
        public boolean equals(final @Nullable Object other) {
            return other instanceof Built
                    && ((Built) other).reference == reference
                    && ((Built) other).matcherType.equals(matcherType);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(reference) + matcherType.hashCode();
        }
    }
}
