/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
