/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.lines.Invoice;
import foo.bar.sample.assertj.lines.InvoiceAssert;
import foo.bar.sample.assertj.lines.Line;
import foo.bar.sample.assertj.lines.LineAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollectionAssertionTest {

    private static final Line LINE = new Line("A-1", 2);
    private static final Invoice INVOICE =
            new Invoice(List.of(LINE, new Line("B-2", 5)), Map.of("A-1", LINE));

    @Test
    @DisplayName("A failing element of a list names the path from its own type")
    void aFailingElementOfAListNamesThePathFromItsOwnType() {
        assertThatThrownBy(
                        () ->
                                InvoiceAssert.assertThat(INVOICE)
                                        .hasLinesSatisfying(
                                                lines ->
                                                        lines.forEach(
                                                                line ->
                                                                        LineAssert.assertThat(line)
                                                                                .hasSku("B-2"))))
                .hasMessage(
                        String.format(
                                "%nExpecting Line.sku to be:%n  \"B-2\"%nbut was:%n  \"A-1\""));
    }

    @Test
    @DisplayName("A failing value of a map names the path from the element type")
    void aFailingValueOfAMapNamesThePathFromTheElementType() {
        assertThatThrownBy(
                        () ->
                                InvoiceAssert.assertThat(INVOICE)
                                        .hasLinesByCodeSatisfying(
                                                linesByCode ->
                                                        LineAssert.assertThat(
                                                                        linesByCode.get("A-1"))
                                                                .hasQuantity(3)))
                .hasMessage(String.format("%nExpecting Line.quantity to be:%n  3%nbut was:%n  2"));
    }

    @Test
    @DisplayName("Elements that meet their requirements pass")
    void elementsThatMeetTheirRequirementsPass() {
        assertThatCode(
                        () ->
                                InvoiceAssert.assertThat(INVOICE)
                                        .hasLinesSatisfying(
                                                lines ->
                                                        lines.forEach(
                                                                line ->
                                                                        LineAssert.assertThat(line)
                                                                                .hasQuantity(
                                                                                        line
                                                                                                .getQuantity()))))
                .doesNotThrowAnyException();
    }
}
