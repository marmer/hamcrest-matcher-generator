/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductionClassFootprintTest {
    @Test
    @DisplayName(
            "A production class annotated with GenerateMatchers carries no trace of it in its"
                    + " class file (QS-N1)")
    void testProductionClass_CarriesNoTraceOfTheConfigurationAnnotation() throws IOException {
        try (InputStream classFile =
                PackageConfigurationProduction.class.getResourceAsStream(
                        "PackageConfigurationProduction.class")) {
            assertNotNull(classFile);
            final String content =
                    new String(classFile.readAllBytes(), StandardCharsets.ISO_8859_1);

            assertFalse(content.contains("GenerateMatchers"));
            assertFalse(content.contains("RuntimeInvisibleAnnotations"));
        }
    }
}
