package io.github.marmer.matchergenerator.core.model;

/**
 * Whether a property may be {@code null}.
 */
public enum Nullability {
    /**
     * Nothing is known about the nullability of the property.
     */
    UNSPECIFIED,
    NULLABLE,
    NON_NULL
}
