/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropertyReadTest {

    @Test
    @DisplayName("A read of a getter returns the value the getter returns")
    void aReadOfAGetterReturnsTheValueTheGetterReturns() {
        assertEquals("name", PropertyRead.of("name", () -> "name"));
    }

    @Test
    @DisplayName("A getter that throws a checked exception fails like a reflective read")
    void aGetterThatThrowsACheckedExceptionFailsLikeAReflectiveRead() {
        final IOException cause = new IOException("disk gone");

        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                PropertyRead.of(
                                        "name",
                                        () -> {
                                            throw cause;
                                        }));

        assertEquals("Could not read property 'name'", error.getMessage());
        assertSame(cause, error.getCause());
    }

    @Test
    @DisplayName("An error thrown by a getter propagates unwrapped")
    void anErrorThrownByAGetterPropagatesUnwrapped() {
        final Error error = new Error("broken");

        final Error thrown =
                assertThrows(
                        Error.class,
                        () ->
                                PropertyRead.of(
                                        "name",
                                        () -> {
                                            throw error;
                                        }));

        assertSame(error, thrown);
    }
}
