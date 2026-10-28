/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.Locale;

/**
 * The names the generated Hamcrest matchers use. A backend built on top of the Hamcrest backend,
 * such as the Kotlin DSL, calls the generated matchers by these names, so they live in one place.
 */
public final class MatcherNaming {

    /** The method that switches the completeness check of a matcher on. */
    public static final String CHECKING_ALL_PROPERTIES = "checkingAllProperties";

    private MatcherNaming() {}

    /**
     * @param type a configured type, or one of its nested types
     * @return the simple name of the generated matcher class of the type
     */
    public static String matcherName(final TypeModel type) {
        return type.simpleName() + "Matcher";
    }

    /**
     * @param type a configured type, or one of its nested types
     * @return the name of the static method that creates a matcher for the type
     */
    public static String initializerName(final TypeModel type) {
        return "is" + type.simpleName();
    }

    /**
     * @param type a configured type, or one of its nested types
     * @return the name of the static method that creates a matcher presetting every property from a
     *     reference object
     */
    public static String referenceInitializerName(final TypeModel type) {
        return initializerName(type) + "Like";
    }

    /**
     * @param property the name of a property
     * @return the name of the methods that check the property against a value or a matcher
     */
    public static String withMethodName(final String property) {
        return "with" + capitalized(property);
    }

    /**
     * @param property the name of a property
     * @return the name of the method that counts the property as addressed by the completeness
     *     check
     */
    public static String ignoringMethodName(final String property) {
        return "ignoring" + capitalized(property);
    }

    /**
     * @param property the name of a property
     * @return the name of the method that removes the check of the property
     */
    public static String resetMethodName(final String property) {
        return "reset" + capitalized(property);
    }

    private static String capitalized(final String value) {
        return value.isEmpty()
                ? value
                : value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
