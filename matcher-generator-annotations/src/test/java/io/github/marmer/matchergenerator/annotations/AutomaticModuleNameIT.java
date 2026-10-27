/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The manifest of the published jar names the module after the package root, since there is no
 * module-info.java.
 */
class AutomaticModuleNameIT {

    private static final String PACKAGE_ROOT = "io.github.marmer.matchergenerator.annotations";

    @Test
    @DisplayName("The jar manifest has an Automatic-Module-Name equal to the package root")
    void theJarManifestHasAnAutomaticModuleNameEqualToThePackageRoot() throws IOException {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("module.jar")).toFile())) {
            assertEquals(
                    PACKAGE_ROOT,
                    jar.getManifest().getMainAttributes().getValue("Automatic-Module-Name"));
            assertNotNull(
                    jar.getEntry(PACKAGE_ROOT.replace('.', '/') + "/"),
                    "package root contained in the jar");
        }
    }
}
