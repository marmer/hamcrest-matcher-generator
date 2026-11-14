/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;

/**
 * What the configured names of a configuration expand to: the types to plan, without the excluded
 * ones, and the types the type model holds for them, nested public member types included. Planning
 * and generating those types is the worker's concern, not this class's.
 */
final class TypeExpansion {
    /** Kind of the {@code kotlin.Metadata} annotation that marks a file facade. */
    private static final int KOTLIN_FILE_FACADE_KIND = 2;

    private TypeExpansion() {}

    /**
     * The types to plan for the given configured names, each with the configured types it stands
     * for. A nested configured type is planned with its highest nesting type, which then is not a
     * configured type itself. Excluded generated types are dropped with all their configured types,
     * and the excluded configured types of the generated types that stay. A generated type without
     * configured types left is dropped.
     */
    static Map<TypeElement, Set<TypeElement>> typesToPlan(
            final ConfiguredNames configuredNames, final Collection<String> names) {
        return withoutExcludedTypes(
                typesNamed(configuredNames, names), configuredNames.exclusions());
    }

    /**
     * Every exclusion that names none of the types the processor models for the configured names,
     * neither a generated type nor one of their public member types.
     */
    static Map<String, AnnotationValue> unmatchedExclusions(final ConfiguredNames configuredNames) {
        final List<TypeElement> modelledTypes =
                typesNamed(configuredNames, configuredNames.names()).keySet().stream()
                        .flatMap(generatedType -> withPublicMemberTypes(generatedType).stream())
                        .toList();
        return configuredNames.exclusionsNamingNone(modelledTypes);
    }

    /**
     * Whether the type is a type of the user. The class Kotlin generates for top level functions,
     * the file facade, is not: it has no properties by design.
     */
    static boolean isUserType(final TypeElement type) {
        return ElementSupport.kotlinMetadata(type).stream()
                .flatMap(mirror -> mirror.getElementValues().entrySet().stream())
                .filter(entry -> entry.getKey().getSimpleName().contentEquals("k"))
                .noneMatch(
                        entry ->
                                Integer.valueOf(KOTLIN_FILE_FACADE_KIND)
                                        .equals(entry.getValue().getValue()));
    }

    private static Map<TypeElement, Set<TypeElement>> typesNamed(
            final ConfiguredNames configuredNames, final Collection<String> names) {
        final Map<TypeElement, Set<TypeElement>> result = new LinkedHashMap<>();
        names.forEach(
                name ->
                        configuredNames
                                .typesNamed(name)
                                .forEach(
                                        (type, configured) ->
                                                result.computeIfAbsent(
                                                                type, key -> new LinkedHashSet<>())
                                                        .addAll(configured)));
        return result;
    }

    /**
     * The type followed by its public member types, recursively, which is the set of types the type
     * model holds.
     */
    private static List<TypeElement> withPublicMemberTypes(final TypeElement type) {
        final List<TypeElement> result = new ArrayList<>();
        result.add(type);
        type.getEnclosedElements().stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(ElementSupport::isPublic)
                .forEach(member -> result.addAll(withPublicMemberTypes(member)));
        return result;
    }

    private static Map<TypeElement, Set<TypeElement>> withoutExcludedTypes(
            final Map<TypeElement, Set<TypeElement>> typesByGeneratedType,
            final Predicate<TypeElement> excluded) {
        final Map<TypeElement, Set<TypeElement>> result = new LinkedHashMap<>();
        typesByGeneratedType.forEach(
                (generatedType, configuredTypes) -> {
                    final Set<TypeElement> kept =
                            configuredTypes.stream()
                                    .filter(excluded.negate())
                                    .collect(Collectors.toCollection(LinkedHashSet::new));
                    if (!excluded.test(generatedType) && !kept.isEmpty()) {
                        result.put(generatedType, kept);
                    }
                });
        return result;
    }
}
