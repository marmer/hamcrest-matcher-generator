/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PackagePrivateHolderTest {

    @Test
    @DisplayName("A mismatch on a property of a package-private type names the property")
    void aMismatchOnAPropertyOfAPackagePrivateTypeNamesTheProperty() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        new PackagePrivateHolder(new PackagePrivatePojo("Bob")),
                                        new PackagePrivateHolderMatcher()
                                                .withPojo(
                                                        new PackagePrivatePojoMatcher()
                                                                .withName("Ada"))));

        assertEquals(
                """

            Expected: PackagePrivateHolder with 1 property check
                 but: pojo.name: expected "Ada" but was "Bob"
            """,
                error.getMessage() + "\n");
    }

    @Test
    @DisplayName("A package-private value that is the wrong one is printed with its properties")
    void aPackagePrivateValueThatIsTheWrongOneIsPrintedWithItsProperties() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        new PackagePrivateHolder(new PackagePrivatePojo("Bob")),
                                        new PackagePrivateHolderMatcher()
                                                .withPojo(is(nullValue()))));

        assertEquals(
                """

            Expected: PackagePrivateHolder with 1 property check
                 but: pojo: expected is null but was PackagePrivatePojo {name "Bob"}
            """,
                error.getMessage() + "\n");
    }
}
