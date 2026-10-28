/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Carries the property path from the root type into generated assertions that are created inside a
 * {@code hasFooSatisfying(..)} requirement, so that their failure messages name the whole path,
 * such as {@code Order.customer.address.city}. Called by generated code only.
 */
@NullMarked
public final class AssertionPath {

    private static final ThreadLocal<Deque<String>> ENCLOSING =
            ThreadLocal.withInitial(ArrayDeque::new);

    private AssertionPath() {}

    /**
     * The path a generated assertion starts at.
     *
     * @param typeName the simple name of the asserted type
     * @return the path of the enclosing requirement, or the type name when there is none
     */
    public static String rootedAt(final String typeName) {
        final String enclosing = ENCLOSING.get().peek();
        return enclosing == null ? typeName : enclosing;
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
        final Deque<String> enclosing = ENCLOSING.get();
        enclosing.push(path);
        try {
            requirements.accept(value);
        } finally {
            enclosing.pop();
        }
    }
}
