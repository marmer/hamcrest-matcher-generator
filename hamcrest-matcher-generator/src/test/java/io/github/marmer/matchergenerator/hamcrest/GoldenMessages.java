/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Objects;
import org.hamcrest.Matcher;

/**
 * Compares mismatch messages with golden files {@code <directory>/<scenario>.txt} next to this
 * class in the test resources.
 */
final class GoldenMessages {
    private final String directory;

    /**
     * @param directory the resource directory of the golden files, by convention the test class
     *     name
     */
    GoldenMessages(final String directory) {
        this.directory = directory;
    }

    /**
     * @return the message of the assertion error {@code assertThat(actual, matcher)} throws
     */
    static String messageOf(final Object actual, final Matcher<?> matcher) {
        return Objects.requireNonNull(
                assertThrows(
                                AssertionError.class,
                                () -> assertThat(actual, (Matcher<Object>) matcher))
                        .getMessage());
    }

    void assertMessage(final String scenario, final String actualMessage) throws IOException {
        GoldenFiles.forModule()
                .assertGolden(
                        GoldenMessages.class.getPackageName().replace('.', '/')
                                + "/"
                                + directory
                                + "/"
                                + scenario
                                + ".txt",
                        actualMessage + "\n");
    }
}
