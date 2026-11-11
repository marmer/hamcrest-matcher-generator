/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.hamcrest.beans.PropertyUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Reads properties of beans and records and knows the property names of the types with a generated
 * matcher.
 */
@NullMarked
final class PropertyAccess {
    static final Object NO_SUCH_PROPERTY = new Object();

    private static final Map<Class<?>, List<String>> PROPERTIES_OF_GENERATED_TYPES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PropertyAccess() {}

    /**
     * Registers the printed properties of a type. The first registration wins, so several matchers
     * of one type never change how its values print. The {@code class} property is not printed.
     */
    static void registerGeneratedType(final Class<?> type, final List<String> propertyNames) {
        PROPERTIES_OF_GENERATED_TYPES.putIfAbsent(
                type, propertyNames.stream().filter(name -> !isClassProperty(name)).toList());
    }

    /**
     * @return whether the property is {@code class}, the one property that {@link
     *     Object#getClass()} gives every object; no other property is exempt
     */
    static boolean isClassProperty(final String propertyName) {
        return propertyName.equals("class");
    }

    static @Nullable List<String> propertiesOfGeneratedType(final Class<?> type) {
        return PROPERTIES_OF_GENERATED_TYPES.get(type);
    }

    /**
     * @return the property value or {@link #NO_SUCH_PROPERTY}
     */
    static @Nullable Object read(final Object item, final String propertyName) {
        final PropertyDescriptor descriptor =
                PropertyUtil.getPropertyDescriptor(propertyName, item);
        if (descriptor != null && descriptor.getReadMethod() != null) {
            return invoke(descriptor.getReadMethod(), item, propertyName);
        }
        try {
            // getMethod without parameter types only finds methods without parameters
            return invoke(item.getClass().getMethod(propertyName), item, propertyName);
        } catch (final NoSuchMethodException e) {
            return NO_SUCH_PROPERTY;
        }
    }

    /**
     * Reads a property through its accessor. An accessor that throws any {@link Exception} fails
     * with the same message as a failing reflective read, {@code Could not read property 'name'},
     * but with the exception itself as cause, where reflection wraps it in an {@link
     * InvocationTargetException}. An {@link Error} propagates unwrapped.
     *
     * @return the property value
     */
    static <T> @Nullable Object readWith(
            final String propertyName,
            final BeanPropertyMatcher.Accessor<? super T> accessor,
            final T bean) {
        try {
            return accessor.read(bean);
        } catch (final Exception e) {
            throw new AssertionError("Could not read property '" + propertyName + "'", e);
        }
    }

    private static @Nullable Object invoke(
            final Method method, final Object item, final String propertyName) {
        try {
            return method.invoke(item);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError("Could not read property '" + propertyName + "'", e);
        }
    }
}
