/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.checked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckedGetterTest {

    @Test
    @DisplayName(
            "A check of a property whose getter throws a checked exception fails with the read"
                    + " failure and the exception as cause")
    void aCheckOfAPropertyWhoseGetterThrowsFailsWithTheReadFailure() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        new Reading(),
                                        ReadingMatcher.isReading().withName("sensor")));

        assertEquals("Could not read property 'name'", error.getMessage());
        assertInstanceOf(IOException.class, error.getCause());
        assertEquals("sensor offline", error.getCause().getMessage());
    }

    @Test
    @DisplayName(
            "A reference object whose getter throws a checked exception fails when its matcher is"
                    + " made, with the read failure and the exception as cause")
    void aReferenceObjectWhoseGetterThrowsFailsWhenItsMatcherIsMade() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class, () -> ReadingMatcher.isReadingLike(new Reading()));

        assertEquals("Could not read property 'name'", error.getMessage());
        assertInstanceOf(IOException.class, error.getCause());
        assertEquals("sensor offline", error.getCause().getMessage());
    }

    @Test
    @DisplayName("Another property check of a matcher whose getter throws still passes")
    void anotherPropertyCheckOfAMatcherWhoseGetterThrowsStillPasses() {
        assertThat(new Reading(), ReadingMatcher.isReading().withCount(1));
    }

    @Test
    @DisplayName("Another property check of a matcher whose getter throws still fails on its own")
    void anotherPropertyCheckOfAMatcherWhoseGetterThrowsStillFailsOnItsOwn() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () -> assertThat(new Reading(), ReadingMatcher.isReading().withCount(2)));

        assertEquals(
                "\nExpected: Reading with 1 property check\n     but: count: expected <2> but was <1>",
                error.getMessage());
    }
}
