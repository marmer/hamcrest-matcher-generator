/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

/**
 * Whether a property may be {@code null}. The core derives it from JSpecify annotations and from
 * the JetBrains nullability annotations that kotlinc emits, see {@code NullabilityResolver}.
 */
public enum Nullability {
    /** Nothing is known about the nullability of the property. */
    UNSPECIFIED,
    /**
     * The property is annotated as nullable, for example with JSpecify's {@code @Nullable} or
     * Kotlin's {@code T?}.
     */
    NULLABLE,
    /**
     * The property is primitive, annotated as non null, or declared inside a JSpecify
     * {@code @NullMarked} scope without a {@code @Nullable}.
     */
    NON_NULL
}
