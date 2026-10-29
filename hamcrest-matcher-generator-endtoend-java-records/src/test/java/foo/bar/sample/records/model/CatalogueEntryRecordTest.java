/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

import static foo.bar.sample.records.model.CatalogueEntryRecordMatcher.isCatalogueEntryRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogueEntryRecordTest {

    @Test
    @DisplayName("Generated matcher matches a record whose component is named isbn")
    void generatedMatcherMatchesARecordWithAnIsbnComponent() {
        CatalogueEntryRecord entry = new CatalogueEntryRecord("978-3-16-148410-0", "Dune");

        assertThat(
                entry,
                isCatalogueEntryRecord()
                        .withIsbn("978-3-16-148410-0")
                        .withIsbn(is(startsWith("978-")))
                        .withTitle("Dune"));
    }

    @Test
    @DisplayName("Generated matcher reports a mismatch on the isbn component")
    void generatedMatcherReportsAMismatchOnTheIsbnComponent() {
        CatalogueEntryRecord entry = new CatalogueEntryRecord("978-3-16-148410-0", "Dune");

        AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () -> assertThat(entry, isCatalogueEntryRecord().withIsbn("0-0")));

        assertTrue(error.getMessage().contains("isbn"), () -> "Mismatch: " + error.getMessage());
    }
}
