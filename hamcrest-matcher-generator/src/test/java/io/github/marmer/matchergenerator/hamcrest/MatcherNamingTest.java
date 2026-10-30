/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatcherNamingTest {

    private static TypeModel modelNamed(final String simpleName) {
        return new TypeModel(
                "some." + simpleName,
                simpleName,
                "some",
                "some",
                List.of(),
                TypeModel.Kind.POJO,
                TypeMirrors.stringType(),
                List.of(),
                List.of());
    }

    @Test
    @DisplayName("A matcher is named after its type, and so is its initializer")
    void aMatcherIsNamedAfterItsType() {
        final TypeModel model = modelNamed("Order");

        assertEquals("OrderMatcher", MatcherNaming.matcherName(model));
        assertEquals("isOrder", MatcherNaming.initializerName(model));
        assertEquals("isOrderLike", MatcherNaming.referenceInitializerName(model));
    }

    @Test
    @DisplayName("Property methods are named after the property with its first letter capitalized")
    void propertyMethodsAreNamedAfterTheProperty() {
        assertEquals("withName", MatcherNaming.withMethodName(propertyNamed("name")));
        assertEquals("ignoringName", MatcherNaming.ignoringMethodName(propertyNamed("name")));
        assertEquals("resetName", MatcherNaming.resetMethodName(propertyNamed("name")));
        assertEquals("withX", MatcherNaming.withMethodName(propertyNamed("x")));
        assertEquals("with", MatcherNaming.withMethodName(propertyNamed("")));
        assertEquals("ignoring", MatcherNaming.ignoringMethodName(propertyNamed("")));
    }

    private static PropertyModel propertyNamed(final String name) {
        return new PropertyModel(name, TypeMirrors.stringType(), "get", Nullability.UNSPECIFIED);
    }

    @Test
    @DisplayName("The completeness check is switched on by checkingAllProperties")
    void theCompletenessCheckIsSwitchedOnByCheckingAllProperties() {
        assertEquals("checkingAllProperties", MatcherNaming.CHECKING_ALL_PROPERTIES);
    }

    private static TypeModel modelOf(final String typeName, final String... accessors) {
        return new TypeModel(
                typeName,
                "Type",
                "some",
                "some",
                List.of(),
                TypeModel.Kind.POJO,
                TypeMirrors.typeNamed(typeName),
                java.util.Arrays.stream(accessors)
                        .map(
                                accessor ->
                                        new PropertyModel(
                                                accessor.substring(3).toLowerCase(Locale.ROOT),
                                                TypeMirrors.stringType(),
                                                accessor,
                                                Nullability.UNSPECIFIED))
                        .toList(),
                List.of());
    }

    @Test
    @DisplayName("A class with a property of its own is compared deeply")
    void aClassWithAPropertyOfItsOwnIsComparedDeeply() {
        assertTrue(MatcherNaming.isComparedDeeply(modelOf("java.lang.String", "getBytes")));
    }

    @Test
    @DisplayName("A type with no property but getClass is not compared deeply")
    void aTypeWithNoPropertyButGetClassIsNotComparedDeeply() {
        assertFalse(MatcherNaming.isComparedDeeply(modelOf("java.lang.String", "getClass")));
        assertFalse(MatcherNaming.isComparedDeeply(modelOf("java.lang.String")));
    }

    @Test
    @DisplayName("Every accessor but getClass is a property of the type")
    void everyAccessorButGetClassIsAProperty() {
        assertTrue(
                MatcherNaming.hasProperties(
                        modelOf("java.util.concurrent.TimeUnit", "getDeclaringClass")));
        assertFalse(MatcherNaming.hasProperties(modelOf("java.lang.String", "getClass")));
    }

    @Test
    @DisplayName("An enum is not compared deeply, whatever its properties")
    void anEnumIsNotComparedDeeply() {
        assertFalse(
                MatcherNaming.isComparedDeeply(
                        modelOf("java.util.concurrent.TimeUnit", "getDeclaringClass")));
    }

    @Test
    @DisplayName("Only a Hamcrest Matcher is a matcher type")
    void onlyAHamcrestMatcherIsAMatcherType() {
        assertTrue(MatcherNaming.isMatcherType(TypeMirrors.typeNamed("org.hamcrest.Matcher")));
        assertFalse(MatcherNaming.isMatcherType(TypeMirrors.stringType()));
    }
}
