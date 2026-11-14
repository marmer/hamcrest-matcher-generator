/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * The names a configuration site configures, in its {@code value} attribute (the value entries) and
 * its {@code exclude} attribute (the exclusion entries), and what they stand for in one round.
 * Built per configuration site and round, with {@link Elements} as its only seam. It keeps javac
 * elements and values, so it must not outlive the round; {@link Memory} keeps names from round to
 * round as strings.
 *
 * <p>Two questions about a value entry are kept apart, since their answers differ:
 *
 * <ul>
 *   <li>{@link #entriesMatchingTypeOrPackage}: does the name match a type or a package? Keyed by
 *       the name as configured.
 *   <li>{@link #entriesNamingTypes}: does the name match a type, a package does not count? Keyed by
 *       the canonical name of the type.
 * </ul>
 */
final class ConfiguredNames {
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String EXCLUDE_ATTRIBUTE = "exclude";

    private final Elements elements;
    private final Map<String, AnnotationValue> valueEntries;
    private final Map<String, AnnotationValue> exclusionEntries;

    private ConfiguredNames(
            final Elements elements,
            final Map<String, AnnotationValue> valueEntries,
            final Map<String, AnnotationValue> exclusionEntries) {
        this.elements = elements;
        this.valueEntries = valueEntries;
        this.exclusionEntries = exclusionEntries;
    }

    /** The names of the annotation, each once, with the value that named it first. */
    static ConfiguredNames of(final Elements elements, final AnnotationMirror configuration) {
        final Map<String, AnnotationValue> values =
                distinctEntries(elements, configuration, VALUE_ATTRIBUTE);
        final Map<String, AnnotationValue> exclusions =
                distinctEntries(elements, configuration, EXCLUDE_ATTRIBUTE);
        return new ConfiguredNames(elements, values, exclusions);
    }

    /**
     * The entries of a list attribute, each name once, with the value that named it first, so that
     * a diagnostic can point at it.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, AnnotationValue> distinctEntries(
            final Elements elements, final AnnotationMirror configuration, final String attribute) {
        final Map<String, AnnotationValue> entries = new LinkedHashMap<>();
        final List<AnnotationValue> values =
                (List<AnnotationValue>)
                        ElementSupport.annotationValue(elements, configuration, attribute)
                                .getValue();
        values.forEach(entry -> entries.putIfAbsent((String) entry.getValue(), entry));
        return entries;
    }

    /** The configured names of the {@code value} attribute, in the order of the annotation. */
    Set<String> names() {
        return valueEntries.keySet();
    }

    /** The names, in their order, that match neither a type nor a package in this round. */
    List<String> namesMatchingNothing() {
        return namesMatchingNothing(valueEntries.keySet());
    }

    /** Those of the given names, in their order, that match neither a type nor a package. */
    List<String> namesMatchingNothing(final Collection<String> names) {
        return names.stream().filter(name -> typesNamed(name).isEmpty()).toList();
    }

    /** The value entries of the given names, in the order of the annotation. */
    Map<String, AnnotationValue> entriesOf(final Collection<String> names) {
        final Map<String, AnnotationValue> found = new LinkedHashMap<>();
        valueEntries.forEach(
                (name, entry) -> {
                    if (names.contains(name)) {
                        found.put(name, entry);
                    }
                });
        return found;
    }

    /**
     * The value entries of those of the given names that match a type or a package, keyed by the
     * name as configured, in the order of the annotation. A name that is a package is among them,
     * unlike in {@link #entriesNamingTypes}.
     */
    Map<String, AnnotationValue> entriesMatchingTypeOrPackage(final Collection<String> names) {
        final Map<String, AnnotationValue> resolved = new LinkedHashMap<>();
        entriesOf(names)
                .forEach(
                        (name, entry) -> {
                            if (!typesNamed(name).isEmpty()) {
                                resolved.put(name, entry);
                            }
                        });
        return resolved;
    }

    /**
     * The value entries of those of the given names that match a type, keyed by the canonical
     * qualified name of that type, in the order of the annotation. A name that is only a package is
     * not among them, unlike in {@link #entriesMatchingTypeOrPackage}. Two names of one type are
     * one entry, the first.
     */
    Map<String, AnnotationValue> entriesNamingTypes(final Collection<String> names) {
        final Map<String, AnnotationValue> named = new LinkedHashMap<>();
        entriesOf(names)
                .forEach(
                        (name, entry) -> {
                            final TypeElement type = elements.getTypeElement(name);
                            if (type != null) {
                                named.putIfAbsent(type.getQualifiedName().toString(), entry);
                            }
                        });
        return named;
    }

    /** Whether a type is excluded: an exclusion entry names it or its package. */
    Predicate<TypeElement> exclusions() {
        return type -> exclusionEntries.keySet().stream().anyMatch(entry -> isNamedBy(type, entry));
    }

    /**
     * The exclusion entries that name none of the given types, neither by qualified name nor by
     * package.
     */
    Map<String, AnnotationValue> exclusionsNamingNone(final Collection<TypeElement> types) {
        final Map<String, AnnotationValue> unmatched = new LinkedHashMap<>();
        exclusionEntries.forEach(
                (name, exclusion) -> {
                    if (types.stream().noneMatch(type -> isNamedBy(type, name))) {
                        unmatched.put(name, exclusion);
                    }
                });
        return unmatched;
    }

    /**
     * Whether a configuration entry names the type, either by its qualified name or by its package.
     */
    private boolean isNamedBy(final TypeElement type, final String entry) {
        return entry.equals(type.getQualifiedName().toString())
                || entry.equals(elements.getPackageOf(type).getQualifiedName().toString());
    }

    /**
     * The types a configured name stands for, each with the configured types it stands for. Empty
     * when the name matches neither a type nor a package in this round.
     */
    Map<TypeElement, Set<TypeElement>> typesNamed(final String qualifiedTypeOrPackageName) {
        final Map<TypeElement, Set<TypeElement>> found = new LinkedHashMap<>();
        elements.getAllPackageElements(qualifiedTypeOrPackageName).stream()
                .flatMap(packageElement -> packageElement.getEnclosedElements().stream())
                .map(TypeElement.class::cast)
                .forEach(
                        type ->
                                found.computeIfAbsent(type, key -> new LinkedHashSet<>())
                                        .add(type));

        final TypeElement configuredType = elements.getTypeElement(qualifiedTypeOrPackageName);
        final TypeElement highestNestingType = highestNestingType(configuredType);
        if (highestNestingType != null) {
            found.computeIfAbsent(highestNestingType, key -> new LinkedHashSet<>())
                    .add(configuredType);
        }
        return found;
    }

    private static TypeElement highestNestingType(final TypeElement typeElement) {
        if (typeElement != null
                && typeElement.getEnclosingElement() instanceof TypeElement enclosing) {
            return highestNestingType(enclosing);
        }
        return typeElement;
    }

    /**
     * The configured names that matched neither a type nor a package when they were seen, by the
     * qualified name of their configuration class, in the order of the annotation. Strings only: an
     * {@code Element}, a mirror or a value must not outlive the round it comes from.
     */
    static final class Memory {
        private final Map<String, List<String>> namesBySite = new LinkedHashMap<>();

        /** Remembers the names of a configuration class, replacing what it remembered before. */
        void remember(final String configurationName, final List<String> names) {
            namesBySite.put(configurationName, List.copyOf(names));
        }

        /** Forgets the names of a configuration class. */
        void forget(final String configurationName) {
            namesBySite.remove(configurationName);
        }

        /** The remembered names by configuration class, a copy that later changes do not touch. */
        Map<String, List<String>> remembered() {
            return new LinkedHashMap<>(namesBySite);
        }
    }
}
