/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import java.util.Locale;

/**
 * The names the generated AssertJ classes use. The generators of a target package and the name
 * clash check both need them, so they live in one place, as the names of the Hamcrest matchers do
 * in {@code MatcherNaming}.
 */
final class AssertionNaming {

    /** The simple name of the generated mixin that gives a package its assertThat and then. */
    static final String SOFT_ASSERTIONS_PROVIDER = "SoftAssertionsProvider";

    /** The simple name of the generated representation that prints the configured types. */
    static final String PROPERTY_REPRESENTATION = "PropertyRepresentation";

    private AssertionNaming() {}

    /**
     * @param simpleName the simple name of a configured type, or of one of its nested types
     * @return the simple name of the abstract assertion class of the type
     */
    static String abstractName(final String simpleName) {
        return "Abstract" + simpleName + "Assert";
    }

    /**
     * @param simpleName the simple name of a configured type, or of one of its nested types
     * @return the simple name of the concrete assertion class of the type
     */
    static String concreteName(final String simpleName) {
        return simpleName + "Assert";
    }

    /**
     * @param simpleName the simple name of a configured type, or of one of its nested types
     * @return the name of the constant that holds the factory of the type, such as {@code
     *     ORDER_LINE} for {@code OrderLine}
     */
    static String factoryName(final String simpleName) {
        return simpleName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }
}
