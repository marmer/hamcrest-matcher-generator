/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.List;
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
        assertEquals("withName", MatcherNaming.withMethodName("name"));
        assertEquals("ignoringName", MatcherNaming.ignoringMethodName("name"));
        assertEquals("resetName", MatcherNaming.resetMethodName("name"));
        assertEquals("withX", MatcherNaming.withMethodName("x"));
        assertEquals("with", MatcherNaming.withMethodName(""));
        assertEquals("ignoring", MatcherNaming.ignoringMethodName(""));
    }

    @Test
    @DisplayName("The completeness check is switched on by checkingAllProperties")
    void theCompletenessCheckIsSwitchedOnByCheckingAllProperties() {
        assertEquals("checkingAllProperties", MatcherNaming.CHECKING_ALL_PROPERTIES);
    }
}
