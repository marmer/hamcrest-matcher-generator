/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GenerationContextTest {

    @Test
    @DisplayName("A context without a class path knows no inherited methods")
    void aContextWithoutAClassPathKnowsNoInheritedMethods() {
        final GenerationContext context =
                new GenerationContext("generator", LocalDateTime.of(2020, 1, 1, 0, 0), false);

        assertEquals(Set.of(), context.inheritedMethodNames("java.lang.Object"));
    }

    @Test
    @DisplayName("A context without an error reporter refuses to drop an error silently")
    void aContextWithoutAnErrorReporterRefusesToDropAnErrorSilently() {
        final GenerationContext context =
                new GenerationContext("generator", LocalDateTime.of(2020, 1, 1, 0, 0), false);

        assertThrows(IllegalStateException.class, () -> context.reportError("some error"));
    }

    @Test
    @DisplayName("A context without a processing environment offers no Kotlin output")
    void aContextWithoutAProcessingEnvironmentOffersNoKotlinOutput() {
        final GenerationContext context =
                new GenerationContext("generator", LocalDateTime.of(2020, 1, 1, 0, 0), false);

        assertFalse(context.kotlinOutputAvailable());
    }

    @Test
    @DisplayName("A context without a note reporter refuses to drop a note silently")
    void aContextWithoutANoteReporterRefusesToDropANoteSilently() {
        final GenerationContext context =
                new GenerationContext("generator", LocalDateTime.of(2020, 1, 1, 0, 0), false);

        assertThrows(IllegalStateException.class, () -> context.reportNote("some note"));
    }

    @Test
    @DisplayName("A context without a warning reporter refuses to drop a warning silently")
    void aContextWithoutAWarningReporterRefusesToDropAWarningSilently() {
        final GenerationContext context =
                new GenerationContext("generator", LocalDateTime.of(2020, 1, 1, 0, 0), false);

        assertThrows(IllegalStateException.class, () -> context.reportWarning("some warning"));
    }
}
