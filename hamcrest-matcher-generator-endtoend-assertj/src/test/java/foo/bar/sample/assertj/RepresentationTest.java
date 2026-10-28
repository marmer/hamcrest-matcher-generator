/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.lines.Line;
import foo.bar.sample.assertj.lines.PropertyRepresentation;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RepresentationTest {

    private static final Line FIRST = new Line("A-1", 2);
    private static final Line SECOND = new Line("B-2", 5);

    @AfterEach
    void useTheDefaultRepresentationAgain() {
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
}
