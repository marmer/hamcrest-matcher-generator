package io.github.marmer.matchergenerator.hamcrest;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.hamcrest.beans.PropertyUtil;

/**
 * Reads properties of beans and records and knows the property names of the types with a generated matcher.
 */
final class PropertyAccess {
    static final Object NO_SUCH_PROPERTY = new Object();

    private static final Map<Class<?>, List<String>> PROPERTIES_OF_GENERATED_TYPES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private PropertyAccess() {
    }

    static void registerGeneratedType(final Class<?> type, final List<String> propertyNames) {
        PROPERTIES_OF_GENERATED_TYPES.put(type, propertyNames);
    }

    static List<String> propertiesOfGeneratedType(final Class<?> type) {
        return PROPERTIES_OF_GENERATED_TYPES.get(type);
    }

    /**
     * @return the property value or {@link #NO_SUCH_PROPERTY}
     */
    static Object read(final Object item, final String propertyName) {
        final PropertyDescriptor descriptor = PropertyUtil.getPropertyDescriptor(propertyName, item);
        if (descriptor != null && descriptor.getReadMethod() != null) {
            return invoke(descriptor.getReadMethod(), item, propertyName);
        }
        try {
            final Method accessor = item.getClass().getMethod(propertyName);
            return accessor.getParameterCount() == 0 ? invoke(accessor, item, propertyName) : NO_SUCH_PROPERTY;
        } catch (final NoSuchMethodException e) {
            return NO_SUCH_PROPERTY;
        }
    }

    private static Object invoke(final Method method, final Object item, final String propertyName) {
        try {
            return method.invoke(item);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError("Could not read property '" + propertyName + "'", e);
        }
    }
}
