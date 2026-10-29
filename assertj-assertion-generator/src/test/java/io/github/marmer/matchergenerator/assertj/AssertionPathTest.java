/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssertionPathTest {

    @Test
    @DisplayName("Outside of any requirement an assertion starts at its own type")
    void outsideOfAnyRequirementAnAssertionStartsAtItsOwnType() {
        assertEquals("Order", AssertionPath.rootedAt("Order", "unrelated"));
    }

    @Test
    @DisplayName("Inside a requirement an assertion starts at the path of the property")
    void insideARequirementAnAssertionStartsAtThePathOfTheProperty() {
        final AtomicReference<String> started = new AtomicReference<>();

        AssertionPath.within(
                "Order.customer",
                "value",
                value -> started.set(AssertionPath.rootedAt("Customer", value)));

        assertEquals("Order.customer", started.get());
    }

    @Test
    @DisplayName("Inside a requirement an assertion for another object starts at its own type")
    void insideARequirementAnAssertionForAnotherObjectStartsAtItsOwnType() {
        final AtomicReference<String> started = new AtomicReference<>();

        AssertionPath.within(
                "Order.customer",
                "value",
                value -> started.set(AssertionPath.rootedAt("Order", "other")));

        assertEquals("Order", started.get());
    }

    @Test
    @DisplayName("The requirement gets the property value, also when it is null")
    void theRequirementGetsThePropertyValueAlsoWhenItIsNull() {
        final List<String> values = new ArrayList<>();
        final AtomicReference<String> nullValue = new AtomicReference<>("not called");

        AssertionPath.within("Order.id", "42", values::add);
        AssertionPath.<@Nullable String>within("Order.id", null, nullValue::set);

        assertEquals(List.of("42"), values);
        assertNull(nullValue.get());
    }

    @Test
    @DisplayName("The innermost requirement decides the path and the outer one is restored")
    void theInnermostRequirementDecidesThePathAndTheOuterOneIsRestored() {
        final List<String> started = new ArrayList<>();

        AssertionPath.within(
                "Order.customer",
                "outer",
                outer -> {
                    AssertionPath.within(
                            "Order.customer.address",
                            "inner",
                            inner -> started.add(AssertionPath.rootedAt("Address", inner)));
                    started.add(AssertionPath.rootedAt("Customer", outer));
                });
        started.add(AssertionPath.rootedAt("Order", "unrelated"));

        assertEquals(List.of("Order.customer.address", "Order.customer", "Order"), started);
    }

    @Test
    @DisplayName("Inside a restarting requirement an assertion starts at its own type")
    void insideARestartingRequirementAnAssertionStartsAtItsOwnType() {
        final List<String> started = new ArrayList<>();

        AssertionPath.within(
                "Order.lines",
                "outer",
                outer -> {
                    AssertionPath.restartingWithin(
                            List.of("line"),
                            lines ->
                                    lines.forEach(
                                            line ->
                                                    started.add(
                                                            AssertionPath.rootedAt(
                                                                    "OrderLine", line))));
                    started.add(AssertionPath.rootedAt("Lines", outer));
                });

        assertEquals(List.of("OrderLine", "Order.lines"), started);
    }

    @Test
    @DisplayName("A requirement inside a restarting one continues from the element type")
    void aRequirementInsideARestartingOneContinuesFromTheElementType() {
        final List<String> started = new ArrayList<>();

        AssertionPath.restartingWithin(
                List.of("line"),
                lines ->
                        AssertionPath.within(
                                "OrderLine.parcel",
                                "parcel",
                                parcel -> started.add(AssertionPath.rootedAt("Parcel", parcel))));

        assertEquals(List.of("OrderLine.parcel"), started);
        assertEquals("Order", AssertionPath.rootedAt("Order", "unrelated"));
    }

    @Test
    @DisplayName("The path is restored when the requirement fails")
    void thePathIsRestoredWhenTheRequirementFails() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        AssertionPath.within(
                                "Order.customer",
                                "value",
                                value -> {
                                    throw new IllegalStateException("failed");
                                }));

        assertEquals("Order", AssertionPath.rootedAt("Order", "unrelated"));
    }
}
