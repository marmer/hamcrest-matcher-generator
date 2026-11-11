/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import foo.bar.sample.records.checked.Reading;
import foo.bar.sample.records.checked.ReadingMatcher;
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
    @DisplayName("The other property checks of the matcher still pass or fail on their own")
    void theOtherPropertyChecksOfTheMatcherStillPassOrFailOnTheirOwn() {
        assertThat(new Reading(), ReadingMatcher.isReading().withCount(1));

        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () -> assertThat(new Reading(), ReadingMatcher.isReading().withCount(2)));

        assertEquals(
                "\nExpected: Reading with 1 property check\n     but: count: expected <2> but was <1>",
                error.getMessage());
    }
}
