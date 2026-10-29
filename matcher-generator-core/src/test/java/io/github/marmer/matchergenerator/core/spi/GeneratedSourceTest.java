/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeneratedSourceTest {

    @Test
    @DisplayName("The qualified name of a source joins its package and simple name")
    void theQualifiedNameOfASourceJoinsItsPackageAndSimpleName() {
        final GeneratedSource source =
                new GeneratedSource("some.pck", "Some", GeneratedSource.Language.JAVA, "");

        assertEquals("some.pck.Some", source.qualifiedName());
    }

    @Test
    @DisplayName("The qualified name of a source in the default package is its simple name")
    void theQualifiedNameOfASourceInTheDefaultPackageIsItsSimpleName() {
        final GeneratedSource source =
                new GeneratedSource("", "Some", GeneratedSource.Language.JAVA, "");

        assertEquals("Some", source.qualifiedName());
    }
}
