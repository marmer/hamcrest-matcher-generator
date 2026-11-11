/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PackagePrivatePojoTest {

    @Test
    @DisplayName("The matcher of a package-private type reads its public getter and matches")
    void theMatcherOfAPackagePrivateTypeMatches() {
        assertThat(new PackagePrivatePojo("Ada"), new PackagePrivatePojoMatcher().withName("Ada"));
    }

    @Test
    @DisplayName("The matcher of a package-private type gives a regular mismatch for a wrong value")
    void theMatcherOfAPackagePrivateTypeGivesARegularMismatch() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        new PackagePrivatePojo("Bob"),
                                        new PackagePrivatePojoMatcher().withName("Ada")));

        assertEquals(
                """

            Expected: PackagePrivatePojo with 1 property check
                 but: name: expected "Ada" but was "Bob"
            """,
                error.getMessage() + "\n");
    }
}
