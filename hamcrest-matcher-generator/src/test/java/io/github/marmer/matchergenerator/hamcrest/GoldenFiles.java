/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compares actual output with golden files below a resource root. With the system property {@value
 * #UPDATE_PROPERTY} set to {@code true} (for example {@code -Dgolden.update=true}), the golden file
 * is overwritten with the actual output instead, so the change can be reviewed with {@code git
 * diff}.
 */
final class GoldenFiles {
    static final String UPDATE_PROPERTY = "golden.update";

    private final Path root;
    private final boolean update;

    GoldenFiles(final Path root, final boolean update) {
        this.root = root;
        this.update = update;
    }

    /**
     * @return the golden files of this module, in {@code src/test/resources} and not in {@code
     *     target/}
     */
    static GoldenFiles forModule() {
        return new GoldenFiles(
                Path.of(System.getProperty("basedir", "."), "src", "test", "resources"),
                updateRequested());
    }

    static boolean updateRequested() {
        return Boolean.getBoolean(UPDATE_PROPERTY);
    }

    /**
     * @param path the golden file, relative to the resource root and separated by {@code /}
     * @param actual the output to compare with, or to write as the new golden file
     */
    void assertGolden(final String path, final String actual) throws IOException {
        final Path file = root.resolve(path);
        if (update) {
            if (!Files.isDirectory(root)) {
                throw new IllegalStateException(
                        "No golden file root to update at " + root.toAbsolutePath());
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, actual, StandardCharsets.UTF_8);
            return;
        }
        if (!Files.isRegularFile(file)) {
            throw new AssertionError("Missing golden file: " + path);
        }
        assertEquals(Files.readString(file, StandardCharsets.UTF_8), actual);
    }
}
