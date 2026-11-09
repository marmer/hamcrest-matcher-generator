/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssertionNamingTest {

    @Test
    @DisplayName(
            "An abstract assertion class is named after its type, Abstract before and Assert after")
    void anAbstractAssertionClassIsNamedAfterItsType() {
        assertEquals("AbstractOrderAssert", AssertionNaming.abstractName("Order"));
    }

    @Test
    @DisplayName("A concrete assertion class is named after its type with Assert after it")
    void aConcreteAssertionClassIsNamedAfterItsType() {
        assertEquals("OrderAssert", AssertionNaming.concreteName("Order"));
    }

    @Test
    @DisplayName(
            "The factory constant is the type name in upper case, words separated by underscores")
    void theFactoryConstantIsTheTypeNameInUpperCase() {
        assertEquals("ORDER", AssertionNaming.factoryName("Order"));
        assertEquals("ORDER_LINE", AssertionNaming.factoryName("OrderLine"));
        assertEquals("LINE2_ITEM", AssertionNaming.factoryName("Line2Item"));
    }

    @Test
    @DisplayName("The mixin and the representation of a package have fixed names")
    void theMixinAndTheRepresentationHaveFixedNames() {
        assertEquals("SoftAssertionsProvider", AssertionNaming.SOFT_ASSERTIONS_PROVIDER);
        assertEquals("PropertyRepresentation", AssertionNaming.PROPERTY_REPRESENTATION);
    }
}
