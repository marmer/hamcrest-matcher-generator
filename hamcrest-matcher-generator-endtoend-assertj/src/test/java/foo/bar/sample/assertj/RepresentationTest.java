/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.lines.Child;
import foo.bar.sample.assertj.lines.Line;
import foo.bar.sample.assertj.lines.LineAssert;
import foo.bar.sample.assertj.lines.Parent;
import foo.bar.sample.assertj.lines.ParentAssert;
import foo.bar.sample.assertj.lines.PropertyRepresentation;
import foo.bar.sample.assertj.lines.Shipment;
import foo.bar.sample.assertj.lines.ShipmentAssert;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RepresentationTest {

    private static final Line FIRST = new Line("A-1", 2);
    private static final Line SECOND = new Line("B-2", 5);
    private static final Line THIRD = new Line("C-3", 1);
    private static final Shipment SHIPMENT =
            new Shipment(null, FIRST, List.of(SECOND), new Line[] {THIRD});

    @BeforeEach
    @AfterEach
    void useTheDefaultRepresentation() {
        Assertions.useDefaultRepresentation();
    }

    @Test
    @DisplayName("Once registered, a collection failure prints its elements by their properties")
    void onceRegisteredACollectionFailurePrintsItsElementsByTheirProperties() {
        Assertions.useRepresentation(new PropertyRepresentation());

        assertThatThrownBy(
                        () ->
                                Assertions.assertThat(List.of(FIRST, SECOND))
                                        .satisfiesExactlyInAnyOrder(
                                                line ->
                                                        Assertions.assertThat(line.getSku())
                                                                .isEqualTo("A-1"),
                                                line ->
                                                        Assertions.assertThat(line.getSku())
                                                                .isEqualTo("A-1")))
                .hasMessageContaining("Line[sku=\"A-1\", quantity=2]")
                .hasMessageContaining("Line[sku=\"B-2\", quantity=5]");
    }

    @Test
    @DisplayName("Without the registration a collection failure prints its elements by toString")
    void withoutTheRegistrationACollectionFailurePrintsItsElementsByToString() {
        LineAssert.assertThat(FIRST).hasSku("A-1");

        assertThatThrownBy(
                        () ->
                                Assertions.assertThat(List.of(FIRST, SECOND))
                                        .satisfiesExactlyInAnyOrder(
                                                line ->
                                                        Assertions.assertThat(line.getSku())
                                                                .isEqualTo("A-1"),
                                                line ->
                                                        Assertions.assertThat(line.getSku())
                                                                .isEqualTo("A-1")))
                .hasMessageContaining("Line@")
                .hasMessageNotContaining("Line[sku=");
    }

    @Test
    @DisplayName("Once registered, a single value is printed by its properties as well")
    void onceRegisteredASingleValueIsPrintedByItsProperties() {
        Assertions.useRepresentation(new PropertyRepresentation());

        assertThatThrownBy(() -> Assertions.assertThat(FIRST).isEqualTo(SECOND))
                .hasMessageContaining("Line[sku=\"A-1\", quantity=2]")
                .hasMessageContaining("Line[sku=\"B-2\", quantity=5]");
    }

    @Test
    @DisplayName(
            "Once registered, the elements of an array property are printed by their properties")
    void onceRegisteredTheElementsOfAnArrayPropertyArePrintedByTheirProperties() {
        Assertions.useRepresentation(new PropertyRepresentation());

        assertThatThrownBy(
                        () ->
                                Assertions.assertThat(SHIPMENT.getParcels())
                                        .satisfiesExactlyInAnyOrder(
                                                line ->
                                                        Assertions.assertThat(line.getSku())
                                                                .isEqualTo("X-0")))
                .hasMessageContaining("Line[sku=\"C-3\", quantity=1]");
    }

    @Test
    @DisplayName("Once registered, the elements of a list property are printed by their properties")
    void onceRegisteredTheElementsOfAListPropertyArePrintedByTheirProperties() {
        Assertions.useRepresentation(new PropertyRepresentation());

        assertThatThrownBy(
                        () ->
                                Assertions.assertThat(SHIPMENT.getLines())
                                        .satisfiesExactlyInAnyOrder(
                                                line ->
                                                        Assertions.assertThat(line.getSku())
                                                                .isEqualTo("X-0")))
                .hasMessageContaining("Line[sku=\"B-2\", quantity=5]");
    }

    @Test
    @DisplayName("A null property is printed as null, a configured value by its properties")
    void aNullPropertyIsPrintedAsNullAndAConfiguredValueByItsProperties() {
        Assertions.useRepresentation(new PropertyRepresentation());

        assertThatThrownBy(
                        () ->
                                ShipmentAssert.assertThat(
                                                new Shipment(null, null, List.of(), new Line[0]))
                                        .hasMainLine(THIRD))
                .hasMessage(
                        String.format(
                                "%nExpecting Shipment.mainLine to be:%n  Line[sku=\"C-3\","
                                        + " quantity=1]%nbut was:%n  null"));
        assertThatThrownBy(() -> ShipmentAssert.assertThat(SHIPMENT).hasMainLine(THIRD))
                .hasMessage(
                        String.format(
                                "%nExpecting Shipment.mainLine to be:%n  Line[sku=\"C-3\","
                                        + " quantity=1]%nbut was:%n  Line[sku=\"A-1\","
                                        + " quantity=2]"));
    }

    @Test
    @DisplayName("A cycle of configured types is printed once, without a stack overflow")
    void aCycleOfConfiguredTypesIsPrintedOnceWithoutAStackOverflow() {
        Assertions.useRepresentation(new PropertyRepresentation());
        final Parent parent = new Parent();
        parent.setChild(new Child("a", parent));

        assertThatThrownBy(() -> ParentAssert.assertThat(parent).hasChild(new Child("b", null)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting Parent.child to be:")
                .hasMessageContaining("Child[name=\"a\", parent=Parent[child=")
                .hasMessageContaining("Child@");
    }
}
