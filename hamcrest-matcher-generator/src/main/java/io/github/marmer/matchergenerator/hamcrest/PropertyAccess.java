/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
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

    /**
     * What printing knows of a generated type: its printed properties and, when the matcher of the
     * type gave them, the accessors that read them.
     */
    private record PrintedType(
            List<String> propertyNames,
            Map<String, ? extends BeanPropertyMatcher.Accessor<?>> accessors) {}

    /**
     * What is printed of each generated type, held by the type itself. A map keyed by the type
     * would keep the class loader of a type alive: its accessors reference that loader, and the map
     * would hold them as long as the type is reachable through the map.
     */
    private static final ClassValue<AtomicReference<@Nullable PrintedType>> PRINTED_TYPES =
            new ClassValue<>() {
                @Override
                protected AtomicReference<@Nullable PrintedType> computeValue(final Class<?> type) {
                    return new AtomicReference<>();
                }
            };

    private PropertyAccess() {}

    /**
     * Registers the printed properties of a type without accessors, which are then read by name.
     */
    static void registerGeneratedType(final Class<?> type, final List<String> propertyNames) {
        registerGeneratedType(type, propertyNames, Map.of());
    }

    /**
     * Registers the printed properties of a type and the accessors that read them. The first
     * registration wins, so several matchers of one type never change how its values print. The
     * {@code class} property is not printed.
     */
    static void registerGeneratedType(
            final Class<?> type,
            final List<String> propertyNames,
            final Map<String, ? extends BeanPropertyMatcher.Accessor<?>> accessors) {
        PRINTED_TYPES
                .get(type)
                .compareAndSet(
                        null,
                        new PrintedType(
                                propertyNames.stream()
                                        .filter(name -> !isClassProperty(name))
                                        .toList(),
                                accessors));
    }

    /**
     * @return whether the property is {@code class}, the one property that {@link
     *     Object#getClass()} gives every object; no other property is exempt
     */
    static boolean isClassProperty(final String propertyName) {
        return propertyName.equals("class");
    }

    static @Nullable List<String> propertiesOfGeneratedType(final Class<?> type) {
        final @Nullable PrintedType printed = PRINTED_TYPES.get(type).get();
        return printed == null ? null : printed.propertyNames();
    }

    /**
     * Reads a property of a value that is printed: through the accessor the matcher of its type
     * gave, otherwise by name.
     *
     * @return the property value or {@link #NO_SUCH_PROPERTY}
     */
    static @Nullable Object readPrinted(final Object item, final String propertyName) {
        final @Nullable PrintedType printed = PRINTED_TYPES.get(item.getClass()).get();
        final BeanPropertyMatcher.@Nullable Accessor<?> accessor =
                printed == null ? null : printed.accessors().get(propertyName);
        if (accessor == null) {
            return read(item, propertyName);
        }
        // The accessors of a type are registered for its exact class, which the item has.
        @SuppressWarnings("unchecked")
        final BeanPropertyMatcher.Accessor<Object> typed =
                (BeanPropertyMatcher.Accessor<Object>) accessor;
        return readWith(propertyName, typed, item);
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
        return call(propertyName, () -> accessor.read(bean));
    }

    /**
     * Calls a getter that may declare a checked exception, with the failure of {@link #readWith}:
     * any {@link Exception} fails with {@code Could not read property 'name'} and the exception as
     * cause. An {@link Error} propagates unwrapped.
     *
     * @return the value the getter returns
     */
    static <V> @Nullable V call(final String propertyName, final Callable<V> getter) {
        try {
            return getter.call();
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
