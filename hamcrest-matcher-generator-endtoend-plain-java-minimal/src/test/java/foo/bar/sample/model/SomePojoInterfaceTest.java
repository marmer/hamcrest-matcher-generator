/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.model;

import static foo.bar.sample.model.SomePojoInterfaceMatcher.isSomePojoInterface;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SomePojoInterfaceTest {
    @Test
    @DisplayName("Interface Matcher should work")
    void testMatchers_InterfaceMatcherShouldWork() throws Exception {
        // Preparation
        final SomePojoInterface somePojoInterface =
                new SomePojoInterface() {
                    @Override
                    public String getSomeProperty() {
                        return "42";
                    }
                };

        // Execution

        // Assertion
        assertThat(somePojoInterface, isSomePojoInterface().withSomeProperty("42"));
    }
}
