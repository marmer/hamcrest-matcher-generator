/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoldenFilesTest {
    @TempDir Path root;

    @Test
    void compareModePassesWhenTheGoldenFileEqualsTheActualOutput() throws IOException {
        write("scenario/a.txt", "expected\n");

        new GoldenFiles(root, false).assertGolden("scenario/a.txt", "expected\n");
    }

    @Test
    void compareModeFailsWhenTheActualOutputDiffers() throws IOException {
        write("scenario/a.txt", "expected\n");

        assertThrows(
                AssertionError.class,
                () -> new GoldenFiles(root, false).assertGolden("scenario/a.txt", "actual\n"));
        assertEquals("expected\n", read("scenario/a.txt"));
    }

    @Test
    void compareModeFailsWhenTheGoldenFileIsMissing() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () -> new GoldenFiles(root, false).assertGolden("scenario/a.txt", "x"));

        assertTrue(Objects.requireNonNull(error.getMessage()).contains("Missing golden file"));
        assertFalse(Files.exists(root.resolve("scenario/a.txt")));
    }

    @Test
    void updateModeOverwritesTheGoldenFileWithTheActualOutputAndPasses() throws IOException {
        write("scenario/a.txt", "stale\n");

        new GoldenFiles(root, true).assertGolden("scenario/a.txt", "fresh\n");

        assertEquals("fresh\n", read("scenario/a.txt"));
    }

    @Test
    void updateModeCreatesMissingGoldenFilesIncludingTheirDirectories() throws IOException {
        new GoldenFiles(root, true).assertGolden("new/dir/b.txt", "created\n");

        assertEquals("created\n", read("new/dir/b.txt"));
    }

    @Test
    void updateModeRefusesToCreateAMissingSourceRoot() {
        final Path missing = root.resolve("no-such-root");

        assertThrows(
                IllegalStateException.class,
                () -> new GoldenFiles(missing, true).assertGolden("a.txt", "x"));
        assertFalse(Files.exists(missing));
    }

    @Test
    void updateRequestedIsTrueOnlyWhenTheSystemPropertyIsTrue() {
        final String previous = System.getProperty(GoldenFiles.UPDATE_PROPERTY);
        try {
            System.setProperty(GoldenFiles.UPDATE_PROPERTY, "true");
            assertTrue(GoldenFiles.updateRequested());

            System.setProperty(GoldenFiles.UPDATE_PROPERTY, "false");
            assertFalse(GoldenFiles.updateRequested());

            System.clearProperty(GoldenFiles.UPDATE_PROPERTY);
            assertFalse(GoldenFiles.updateRequested());
        } finally {
            if (previous == null) {
                System.clearProperty(GoldenFiles.UPDATE_PROPERTY);
            } else {
                System.setProperty(GoldenFiles.UPDATE_PROPERTY, previous);
            }
        }
    }

    private void write(final String path, final String content) throws IOException {
        final Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private String read(final String path) throws IOException {
        return Files.readString(root.resolve(path), StandardCharsets.UTF_8);
    }
}
