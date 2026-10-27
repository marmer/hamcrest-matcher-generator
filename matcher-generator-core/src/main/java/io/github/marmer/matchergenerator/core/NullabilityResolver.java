/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.Nullability;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

/**
 * Reads the nullability of a property from what the annotation-processing model shows: JSpecify
 * annotations and the nullability annotations Kotlin emits into the bytecode and into KAPT stubs.
 *
 * <p>Kotlin nullness comes from the JetBrains annotations that kotlinc emits, not from {@code
 * kotlin.Metadata}: the annotations are plain annotation mirrors, while reading the metadata needs
 * the Kotlin metadata library, which the core keeps off its dependencies. A type variable is
 * null-agnostic even inside a {@code @NullMarked} scope, so it only counts as nullable or non null
 * when annotated.
 */
final class NullabilityResolver {
    private static final Set<String> NULLABLE_ANNOTATIONS =
            Set.of("org.jspecify.annotations.Nullable", "org.jetbrains.annotations.Nullable");
    private static final Set<String> NON_NULL_ANNOTATIONS =
            Set.of("org.jspecify.annotations.NonNull", "org.jetbrains.annotations.NotNull");
    private static final String NULL_MARKED = "org.jspecify.annotations.NullMarked";
    private static final String NULL_UNMARKED = "org.jspecify.annotations.NullUnmarked";

    private NullabilityResolver() {}

    /**
     * The nullability of the value the accessor returns. A primitive is never {@code null}, an
     * explicit annotation wins, and without one the closest enclosing {@code @NullMarked} or
     * {@code @NullUnmarked} scope decides.
     */
    static Nullability of(final ExecutableElement accessor) {
        if (accessor.getReturnType().getKind().isPrimitive()) {
            return Nullability.NON_NULL;
        }
        if (hasAnyAnnotation(accessor, NULLABLE_ANNOTATIONS)) {
            return Nullability.NULLABLE;
        }
        if (hasAnyAnnotation(accessor, NON_NULL_ANNOTATIONS)) {
            return Nullability.NON_NULL;
        }
        if (accessor.getReturnType().getKind() == TypeKind.TYPEVAR) {
            return Nullability.UNSPECIFIED;
        }
        return nullMarkedScope(accessor);
    }

    /**
     * Declaration annotations sit on the accessor, JSpecify's type-use annotations on its return
     * type. On an array type the latter belong to the component type, so they say nothing about the
     * array itself.
     */
    private static boolean hasAnyAnnotation(
            final ExecutableElement accessor, final Set<String> qualifiedNames) {
        return hasAnyAnnotation(accessor.getAnnotationMirrors(), qualifiedNames)
                || (accessor.getReturnType().getKind() != TypeKind.ARRAY
                        && hasAnyAnnotation(
                                accessor.getReturnType().getAnnotationMirrors(), qualifiedNames));
    }

    private static boolean hasAnyAnnotation(
            final Iterable<? extends AnnotationMirror> mirrors, final Set<String> qualifiedNames) {
        for (final AnnotationMirror mirror : mirrors) {
            if (qualifiedNames.stream().anyMatch(name -> ElementSupport.isOfType(mirror, name))) {
                return true;
            }
        }
        return false;
    }

    private static Nullability nullMarkedScope(final Element element) {
        for (Element scope = element; scope != null; scope = scope.getEnclosingElement()) {
            if (ElementSupport.hasAnnotation(scope, NULL_UNMARKED)) {
                return Nullability.UNSPECIFIED;
            }
            if (ElementSupport.hasAnnotation(scope, NULL_MARKED)) {
                return Nullability.NON_NULL;
            }
        }
        return Nullability.UNSPECIFIED;
    }
}
