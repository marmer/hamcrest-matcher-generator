package io.github.marmer.matchergenerator.hamcrest;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
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

    /**
     * Registers the printed properties of a type. The first registration wins, so several matchers of one type never
     * change how its values print. Properties every object has (such as {@code class}) are not printed.
     */
    static void registerGeneratedType(final Class<?> type, final List<String> propertyNames) {
        PROPERTIES_OF_GENERATED_TYPES.putIfAbsent(type, propertyNames.stream()
            .filter(name -> !isDeclaredByObject(name))
            .collect(Collectors.toUnmodifiableList()));
    }

    private static boolean isDeclaredByObject(final String propertyName) {
        return Arrays.stream(Object.class.getMethods())
            .filter(method -> method.getParameterCount() == 0)
            .anyMatch(method -> method.getName().equals(propertyName)
                || method.getName().equals("get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1)));
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
            // getMethod without parameter types only finds methods without parameters
            return invoke(item.getClass().getMethod(propertyName), item, propertyName);
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
