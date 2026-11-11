/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import java.util.concurrent.Callable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Reads a property of the asserted object for a generated assertion whose getter declares a checked
 * exception, which the generated code has no handler for. A getter that throws fails like a
 * reflective read: with {@code Could not read property 'name'} and the exception as cause. An
 * {@link Error} propagates unwrapped. Called by generated code only.
 */
@NullMarked
public final class PropertyRead {

    private PropertyRead() {}

    /**
     * @param property the name of the property, for the failure
     * @param getter reads the property from the asserted object
     * @param <V> the type of the property value
     * @return the value of the property
     */
    public static <V> @Nullable V of(final String property, final Callable<V> getter) {
        try {
            return getter.call();
        } catch (final Exception e) {
            throw new AssertionError("Could not read property '" + property + "'", e);
        }
    }
}
