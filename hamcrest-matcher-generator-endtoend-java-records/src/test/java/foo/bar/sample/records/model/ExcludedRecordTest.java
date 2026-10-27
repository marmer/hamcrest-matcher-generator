/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExcludedRecordTest {

    @Test
    @DisplayName(
            "A record excluded from its configured package gets no matcher, its package siblings do")
    void anExcludedRecordGetsNoMatcher() {
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("foo.bar.sample.records.model.HiddenRecordMatcher"));
        assertDoesNotThrow(() -> Class.forName("foo.bar.sample.records.model.PersonRecordMatcher"));
    }
}
