/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.docs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Jackson reaches the docs build only through Structurizr (docs tooling, no published jar). The
 * docs classpath must still carry the Jackson releases that fix the OpenSSF Scorecard advisories
 * GHSA-7hhh-6rmp-j9qf, GHSA-p6pp-m3f8-5c89 (jackson-core) and GHSA-cxp5-3px4-pw24,
 * GHSA-wv8q-qhhj-9h54 (jackson-databind), all fixed in 2.22.3.
 */
class JacksonVersionTest {

    @Test
    @DisplayName("The docs classpath carries a jackson-databind release that fixes the advisories")
    void theDatabindOnTheDocsClasspathIsAFixedRelease() {
        assertFixedRelease("jackson-databind", new ObjectMapper().version());
    }

    @Test
    @DisplayName("The docs classpath carries a jackson-core release that fixes the advisories")
    void theCoreOnTheDocsClasspathIsAFixedRelease() {
        assertFixedRelease("jackson-core", new JsonFactory().version());
    }

    private static void assertFixedRelease(final String artifact, final Version version) {
        final boolean fixed =
                version.getMajorVersion() > 2
                        || version.getMajorVersion() == 2 && version.getMinorVersion() > 22
                        || version.getMajorVersion() == 2
                                && version.getMinorVersion() == 22
                                && version.getPatchLevel() >= 3;
        assertTrue(fixed, artifact + " " + version + " is older than 2.22.3");
    }
}
