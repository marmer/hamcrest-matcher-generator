/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import foo.bar.sample.assertj.checked.PropertyRepresentation;
import foo.bar.sample.assertj.checked.Reading;
import foo.bar.sample.assertj.checked.ReadingAssert;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckedGetterAssertionTest {

    @AfterEach
    void useTheDefaultRepresentation() {
        Assertions.useDefaultRepresentation();
    }

    @Test
    @DisplayName(
            "Once the generated representation is registered, a failure that prints the reading"
                    + " shows the read failure instead of the mismatch")
    void onceTheRepresentationIsRegisteredAFailurePrintingTheReadingShowsTheReadFailure() {
        Assertions.useRepresentation(new PropertyRepresentation());
        final Reading reading = new Reading();

        final Throwable thrown =
                catchThrowable(() -> ReadingAssert.assertThat(reading).isEqualTo(new Reading()));

        assertThat(thrown)
                .isInstanceOf(AssertionError.class)
                .hasMessage("Could not read property 'name'")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName(
            "A check of a property whose getter throws a checked exception fails with the read"
                    + " failure and the exception as cause")
    void aCheckOfAPropertyWhoseGetterThrowsFailsWithTheReadFailure() {
        final Reading reading = new Reading();

        final Throwable thrown =
                catchThrowable(() -> ReadingAssert.assertThat(reading).hasName("sensor"));

        assertThat(thrown)
                .isInstanceOf(AssertionError.class)
                .hasMessage("Could not read property 'name'")
                .hasCauseInstanceOf(IOException.class);
        assertThat(thrown.getCause()).hasMessage("sensor offline");
    }

    @Test
    @DisplayName("A requirement on a property whose getter throws fails with the read failure")
    void aRequirementOnAPropertyWhoseGetterThrowsFailsWithTheReadFailure() {
        final Reading reading = new Reading();

        assertThatThrownBy(() -> ReadingAssert.assertThat(reading).hasNameSatisfying(name -> {}))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Could not read property 'name'")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("The other property checks still fail with their own message")
    void theOtherPropertyChecksStillFailWithTheirOwnMessage() {
        final Reading reading = new Reading();

        assertThatThrownBy(() -> ReadingAssert.assertThat(reading).hasCount(2))
                .hasMessage(String.format("%nExpecting Reading.count to be:%n  2%nbut was:%n  1"));
    }
}
