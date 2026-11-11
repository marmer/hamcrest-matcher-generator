/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Matcher for beans.
 *
 * <p>A mismatch lists only the failing checks, one line each, with property path, expected and
 * actual value. Generated matchers used as property checks are flattened into dot paths.
 *
 * @param <T> Type of the bean.
 * @author marmer
 * @since 13.06.2017
 */
@NullMarked
public class BeanPropertyMatcher<T> extends TypeSafeMatcher<T> {
    private static final Matcher<Object> PROPERTY_EXISTS = new PropertyExists();

    /**
     * The items being checked by a matcher on this thread, by identity. A pair of matcher and item
     * met again on its way down counts as equal, so cyclic graphs terminate.
     */
    private static final ThreadLocal<Map<BeanPropertyMatcher<?>, Set<Object>>> CHECKING =
            ThreadLocal.withInitial(IdentityHashMap::new);

    /** The matchers being described on this thread, which a cyclic matcher meets again. */
    private static final ThreadLocal<Set<BeanPropertyMatcher<?>>> DESCRIBING =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    @SuppressWarnings("squid:S2293")
    private final Map<String, List<Matcher<?>>> propertyChecks = new LinkedHashMap<>();

    private final Set<String> presetProperties = new LinkedHashSet<>();

    private final Set<String> ignoredProperties = new LinkedHashSet<>();

    private final Class<? super T> expectedClass;

    private final List<String> propertyNames;

    /** The accessors of the properties built with {@link #property}, by property name. */
    private final Map<String, Accessor<? super T>> accessors;

    private boolean checkingAllProperties;

    public BeanPropertyMatcher(final Class<? super T> expectedClass) {
        this(expectedClass, List.of(), Map.of(), false);
    }

    /**
     * @param expectedClass the type to match
     * @param propertyNames all properties of the type in declaration order, used to print values of
     *     the type from their properties instead of via {@code toString()} and to find the
     *     properties a completeness check misses
     */
    public BeanPropertyMatcher(
            final Class<? super T> expectedClass, final String... propertyNames) {
        this(expectedClass, List.of(propertyNames), Map.of(), true);
    }

    /**
     * @param expectedClass the type to match
     * @param properties all properties of the type in declaration order, each with the accessor
     *     that reads it. The values are read through the accessors, so the type does not have to be
     *     accessible by reflection. A name that is not listed is read by name.
     */
    @SafeVarargs
    public BeanPropertyMatcher(
            final Class<? super T> expectedClass, final Property<T>... properties) {
        this(
                expectedClass,
                Arrays.stream(properties).map(Property::name).toList(),
                accessorsOf(properties),
                true);
    }

    private BeanPropertyMatcher(
            final Class<? super T> expectedClass,
            final List<String> propertyNames,
            final Map<String, Accessor<? super T>> accessors,
            final boolean printsPropertyNames) {
        this.expectedClass = expectedClass;
        this.propertyNames = propertyNames;
        this.accessors = accessors;
        if (printsPropertyNames) {
            PropertyAccess.registerGeneratedType(expectedClass, propertyNames);
        }
    }

    /**
     * A property of the matched type with the accessor that reads it.
     *
     * @param <T> the type of the bean
     * @param name the name of the property, as the matcher methods name it
     * @param accessor reads the value of the property from a bean of the type
     */
    public record Property<T>(String name, Accessor<? super T> accessor) {}

    /**
     * Reads the value of a property from a bean, e.g. with the method reference {@code
     * Sub::isDone}. The value may be {@code null}, as the property may be.
     *
     * @param <T> the type of the bean
     */
    @FunctionalInterface
    public interface Accessor<T> {
        /**
         * Reads the value of the property from the bean.
         *
         * @param bean the bean to read the property from
         * @return the value of the property
         * @throws Exception when the getter declares a checked exception and throws it
         */
        @Nullable Object read(T bean) throws Exception;
    }

    /**
     * Names a property of the matched type and the accessor that reads it, e.g. {@code
     * property("done", Sub::isDone)}.
     *
     * @param name the name of the property, as the matcher methods name it
     * @param accessor reads the value of the property from a bean of the type
     * @return the property, for the constructor of the matcher
     */
    public static <T> Property<T> property(final String name, final Accessor<? super T> accessor) {
        return new Property<>(name, accessor);
    }

    private static <V> Map<String, Accessor<? super V>> accessorsOf(
            final Property<V>[] properties) {
        final Map<String, Accessor<? super V>> accessors = new LinkedHashMap<>();
        for (final Property<V> property : properties) {
            accessors.putIfAbsent(property.name(), property.accessor());
        }
        return accessors;
    }

    @Override
    public void describeTo(final Description description) {
        final List<PathWalk.Check> checks = new ArrayList<>();
        collectChecks("", checks);
        if (PathWalk.isInsideForeignMatcher()) {
            description.appendText(typeName() + " {" + PathWalk.describeChecks(checks) + "}");
        } else {
            description.appendText(
                    typeName()
                            + " with "
                            + checks.size()
                            + (checks.size() == 1 ? " property check" : " property checks")
                            + (checkingAllProperties ? ", every property checked or ignored" : ""));
        }
    }

    @Override
    protected boolean matchesSafely(final T item) {
        return failuresOf(item).isEmpty();
    }

    public BeanPropertyMatcher<T> with(final String propertyName, final Matcher<?> matcher) {
        dropPreset(propertyName);
        propertyChecks.computeIfAbsent(propertyName, key -> new ArrayList<>()).add(matcher);
        return this;
    }

    public BeanPropertyMatcher<T> with(final String propertyName) {
        return with(propertyName, PROPERTY_EXISTS);
    }

    /**
     * Presets the check of a property, e.g. from a reference object. A preset is replaced by the
     * next {@link #with(String, Matcher)} for the property and dropped by {@link #reset(String)}
     * and {@link #ignore(String)}.
     */
    public BeanPropertyMatcher<T> preset(final String propertyName, final Matcher<?> matcher) {
        propertyChecks.put(propertyName, new ArrayList<>(List.of(matcher)));
        presetProperties.add(propertyName);
        return this;
    }

    /**
     * Consciously excludes a property from the completeness check. Ignoring does not check the
     * property.
     */
    public BeanPropertyMatcher<T> ignore(final String propertyName) {
        dropPreset(propertyName);
        ignoredProperties.add(propertyName);
        return this;
    }

    /**
     * Opts this matcher into the completeness check: every property must be checked or ignored.
     * Applies to this matcher only, not to the matchers nested in it.
     */
    public BeanPropertyMatcher<T> checkingAllProperties() {
        checkingAllProperties = true;
        return this;
    }

    public void reset(final String propertyName) {
        propertyChecks.remove(propertyName);
        presetProperties.remove(propertyName);
        ignoredProperties.remove(propertyName);
    }

    private void dropPreset(final String propertyName) {
        if (presetProperties.remove(propertyName)) {
            propertyChecks.remove(propertyName);
        }
    }

    @Override
    protected void describeMismatchSafely(final T item, final Description mismatchDescription) {
        final List<String> failures = failuresOf(item);
        mismatchDescription.appendText(
                PathWalk.isInsideForeignMatcher()
                        ? typeName() + " {" + String.join(", ", failures) + "}"
                        : String.join(PathWalk.FAILURE_SEPARATOR, failures));
    }

    String typeName() {
        return expectedClass.getSimpleName();
    }

    boolean accepts(final Object value) {
        return expectedClass.isInstance(value);
    }

    private List<String> failuresOf(final Object item) {
        final MismatchReport report = new MismatchReport();
        if (!expectedClass.isInstance(item)) {
            report.fail(
                    "expected an instance of "
                            + expectedClass.getName()
                            + " but was a "
                            + item.getClass().getName()
                            + " ("
                            + PropertyDescription.describeValue(item)
                            + ")");
        }
        collectFailures(item, "", report);
        return report.lines();
    }

    /**
     * Adds the failures of an item. Properties neither checked nor ignored by a matcher with the
     * completeness check are added to the unaddressed paths of the report, not to its failures.
     */
    void collectFailures(final Object item, final String prefix, final MismatchReport report) {
        final Set<Object> items =
                CHECKING.get()
                        .computeIfAbsent(
                                this, key -> Collections.newSetFromMap(new IdentityHashMap<>()));
        if (!items.add(item)) {
            return;
        }
        try {
            collectFailuresOnce(item, prefix, report);
        } finally {
            items.remove(item);
            if (items.isEmpty()) {
                final Map<BeanPropertyMatcher<?>, Set<Object>> checking = CHECKING.get();
                checking.remove(this);
                if (checking.isEmpty()) {
                    CHECKING.remove();
                }
            }
        }
    }

    private void collectFailuresOnce(
            final Object item, final String prefix, final MismatchReport report) {
        if (checkingAllProperties) {
            propertyNames.stream()
                    .filter(name -> !PropertyAccess.isClassProperty(name))
                    .filter(name -> !isAddressed(name))
                    .forEach(name -> report.addUnaddressed(prefix + name));
        }
        propertyChecks.forEach(
                (name, matchers) -> {
                    final Object value = valueOf(item, name);
                    for (final Matcher<?> matcher : matchers) {
                        collectFailure(name, matcher, value, prefix + name, report);
                    }
                });
    }

    /**
     * @return the value of the property: read through its accessor when the item is of the matched
     *     type, otherwise (and for a property without accessor) by name, see {@link
     *     PropertyAccess#read}
     */
    private @Nullable Object valueOf(final Object item, final String name) {
        final @Nullable Accessor<? super T> accessor = accessors.get(name);
        if (accessor == null || !expectedClass.isInstance(item)) {
            return PropertyAccess.read(item, name);
        }
        // The item is an instance of the matched class: the check above.
        @SuppressWarnings("unchecked")
        final T bean = (T) item;
        return PropertyAccess.readWith(name, accessor, bean);
    }

    private boolean isAddressed(final String name) {
        return propertyChecks.containsKey(name) || ignoredProperties.contains(name);
    }

    private void collectFailure(
            final String name,
            final Matcher<?> matcher,
            final @Nullable Object value,
            final String path,
            final MismatchReport report) {
        if (value == PropertyAccess.NO_SUCH_PROPERTY) {
            report.fail(
                    path,
                    "expected "
                            + PathWalk.describeExpected(matcher)
                            + " but "
                            + typeName()
                            + " has no property \""
                            + name
                            + "\"");
        } else {
            PathWalk.collectValueFailures(matcher, value, path, report);
        }
    }

    void collectChecks(final String prefix, final List<PathWalk.Check> checks) {
        if (!DESCRIBING.get().add(this)) {
            return;
        }
        try {
            propertyChecks.forEach(
                    (name, matchers) -> {
                        for (final Matcher<?> matcher : matchers) {
                            PathWalk.collectValueChecks(prefix + name, matcher, checks);
                        }
                    });
        } finally {
            final Set<BeanPropertyMatcher<?>> describing = DESCRIBING.get();
            describing.remove(this);
            if (describing.isEmpty()) {
                DESCRIBING.remove();
            }
        }
    }

    private static final class PropertyExists extends BaseMatcher<Object> {
        @Override
        public boolean matches(final @Nullable Object actual) {
            return true;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("the property to exist");
        }
    }
}
