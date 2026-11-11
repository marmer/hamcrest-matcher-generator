/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatcherGenerationProcessorWorkerFieldsTest {

    @Test
    @DisplayName(
            "The worker keeps no element or mirror in a field, so nothing of an earlier round outlives that round")
    void theWorkerKeepsNoElementOrMirrorInAField() {
        for (final Field field : MatcherGenerationProcessorWorker.class.getDeclaredFields()) {
            assertFalse(
                    field.getGenericType().getTypeName().contains("javax.lang.model"),
                    () -> "Field keeps a language model type: " + field);
        }
    }
}
