/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Carries the property path from the root type into the generated assertion of the value a {@code
 * hasFooSatisfying(..)} requirement is run for, so that its failure messages name the whole path,
 * such as {@code Order.customer.address.city}. An assertion for any other object created inside the
 * requirement starts at its own type. Inside a collection or map the path restarts at the element
 * type, as in {@code OrderLine.sku}. Called by generated code only.
 */
@NullMarked
public final class AssertionPath {

    /**
     * The enclosing requirements, innermost first: the value a requirement is run for and the path
     * its assertion starts at; an empty path restarts at the type.
     */
    private static final ThreadLocal<Deque<Requirement>> ENCLOSING =
            ThreadLocal.withInitial(ArrayDeque::new);

    private AssertionPath() {}

    /**
     * The path a generated assertion starts at.
     *
     * @param typeName the simple name of the asserted type
     * @param actual the asserted object
     * @return the path of the innermost enclosing requirement when {@code actual} is the very value
     *     that requirement runs for, otherwise the type name, as it is when there is no requirement
     *     or it restarts
     */
    public static String rootedAt(final String typeName, final @Nullable Object actual) {
        final @Nullable Requirement enclosing = ENCLOSING.get().peek();
        if (enclosing == null || enclosing.value != actual) {
            return typeName;
        }
        return enclosing.path.orElse(typeName);
    }

    /**
     * Runs the requirements for a property value with its path as the one assertions created in the
     * requirements start at.
     *
     * @param path the path of the property
     * @param value the property value
     * @param requirements the requirements the value has to meet
     * @param <T> the type of the value
     */
    public static <T extends @Nullable Object> void within(
            final String path, final T value, final Consumer<? super T> requirements) {
        run(Optional.of(path), value, requirements);
    }

    /**
     * Runs the requirements for the elements of a collection or map property. Assertions created in
     * the requirements start at their own type, not at the path of the property.
     *
     * @param value the property value
     * @param requirements the requirements the value has to meet
     * @param <T> the type of the value
     */
    public static <T extends @Nullable Object> void restartingWithin(
            final T value, final Consumer<? super T> requirements) {
        run(Optional.empty(), value, requirements);
    }

    private static <T extends @Nullable Object> void run(
            final Optional<String> path, final T value, final Consumer<? super T> requirements) {
        final Deque<Requirement> enclosing = ENCLOSING.get();
        enclosing.push(new Requirement(path, value));
        try {
            requirements.accept(value);
        } finally {
            enclosing.pop();
        }
    }

    private static final class Requirement {
        private final Optional<String> path;
        private final @Nullable Object value;

        private Requirement(final Optional<String> path, final @Nullable Object value) {
            this.path = path;
            this.value = value;
        }
    }
}
