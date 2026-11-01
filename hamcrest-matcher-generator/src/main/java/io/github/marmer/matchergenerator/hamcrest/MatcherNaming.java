/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.hamcrest.Matcher;

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
     * @param property a property of a configured type
     * @return the name of the methods that check the property against a value or a matcher
     */
    public static String withMethodName(final PropertyModel property) {
        return "with" + property.capitalizedName();
    }

    /**
     * @param property a property of a configured type
     * @return the name of the method that counts the property as addressed by the completeness
     *     check
     */
    public static String ignoringMethodName(final PropertyModel property) {
        return "ignoring" + property.capitalizedName();
    }

    /**
     * @param property a property of a configured type
     * @return the name of the method that removes the check of the property
     */
    public static String resetMethodName(final PropertyModel property) {
        return "reset" + property.capitalizedName();
    }

    /**
     * Whether a property of this type is checked deeply with the matcher generated for the type, as
     * opposed to by {@code equals}: the type is no enum and has a property besides the one {@code
     * Object.getClass()} contributes, since it would tell nothing apart otherwise.
     *
     * @param type a configured type, or one of its nested types
     * @return whether a matcher of the type is used to check a property of that type
     */
    public static boolean isComparedDeeply(final TypeModel type) {
        return ((DeclaredType) type.type()).asElement().getKind() != ElementKind.ENUM
                && hasProperties(type);
    }

    /**
     * @param type a configured type, or one of its nested types
     * @return whether the type has an accessor property, see {@link TypeModel#accessorProperties()}
     */
    public static boolean hasProperties(final TypeModel type) {
        return !type.accessorProperties().isEmpty();
    }

    /**
     * @param type the type of a property
     * @return whether the type is Hamcrest's {@code Matcher}, which a property takes as a value and
     *     which therefore gets no overload that takes a matcher
     */
    public static boolean isMatcherType(final TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED
                && Matcher.class
                        .getCanonicalName()
                        .contentEquals(
                                ((TypeElement) ((DeclaredType) type).asElement())
                                        .getQualifiedName());
    }
}
