/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.lang.model.type.TypeMirror;

/**
 * Immutable, library-free description of a configured type and its properties.
 *
 * @param qualifiedName the qualified name of the type
 * @param simpleName the simple name of the type
 * @param packageName the package of the type
 * @param targetPackage the package generated code for the type belongs to
 * @param typeParameters the names of the type parameters, empty for non generic types
 * @param kind whether the type is a record or any other type
 * @param type the declared type, with its own type parameters as arguments
 * @param properties the properties in detection order. Names may occur more than once if different
 *     accessors map to the same name, e.g. {@code getFoo()} and {@code isFoo()}.
 * @param nestedTypes the public member types
 */
public record TypeModel(
        String qualifiedName,
        String simpleName,
        String packageName,
        String targetPackage,
        List<String> typeParameters,
        Kind kind,
        TypeMirror type,
        List<PropertyModel> properties,
        List<TypeModel> nestedTypes) {

    /** The only accessor {@code Object} contributes. It is not a property a type declares. */
    private static final String OBJECT_CLASS_ACCESSOR = "getClass";

    public TypeModel {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(targetPackage, "targetPackage");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(type, "type");
        typeParameters = List.copyOf(typeParameters);
        properties = List.copyOf(properties);
        nestedTypes = List.copyOf(nestedTypes);
    }

    /**
     * The properties a generated type checks: the distinct ones, in detection order, without the
     * {@code getClass()} that every type inherits from {@code Object}.
     *
     * <p>Sites that must keep the {@code getClass()} accessor, such as the Hamcrest equals matchers
     * and the Kotlin DSL, use {@code PropertyModel.distinctByName(properties())} instead. The
     * generated {@code class} property methods and their goldens depend on it.
     *
     * @return the properties with distinct names, without the {@code getClass()} accessor
     */
    public List<PropertyModel> accessorProperties() {
        return PropertyModel.distinctByName(properties).stream()
                .filter(property -> !OBJECT_CLASS_ACCESSOR.equals(property.accessor()))
                .collect(Collectors.toList());
    }

    /** The kind of a configured type. */
    public enum Kind {
        /** Any type whose properties are bean getters: classes, interfaces and enums. */
        POJO,
        RECORD
    }
}
