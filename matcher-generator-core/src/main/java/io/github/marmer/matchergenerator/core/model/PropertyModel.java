/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;

/**
 * A readable value of a configured type.
 *
 * @param name the property name, e.g. {@code foo} for the accessor {@code getFoo()}
 * @param type the type of the property value
 * @param accessor the name of the method that reads the property
 * @param nullability whether the value may be {@code null}
 */
public record PropertyModel(
        String name, TypeMirror type, String accessor, Nullability nullability) {

    public PropertyModel {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(accessor, "accessor");
        Objects.requireNonNull(nullability, "nullability");
    }

    /**
     * Keeps the first property of every name, in the given order.
     *
     * @param properties properties, which may contain the same name more than once
     * @return the properties with distinct names
     */
    public static List<PropertyModel> distinctByName(final List<PropertyModel> properties) {
        final Map<String, PropertyModel> distinct = new LinkedHashMap<>();
        properties.forEach(property -> distinct.putIfAbsent(property.name(), property));
        return new ArrayList<>(distinct.values());
    }
}
