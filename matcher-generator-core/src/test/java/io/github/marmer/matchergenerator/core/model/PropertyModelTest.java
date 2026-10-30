/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

import static io.github.marmer.matchergenerator.core.model.ModelTestSupport.property;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropertyModelTest {

    @Test
    @DisplayName("The capitalized name upper cases the first letter of the property name")
    void theCapitalizedNameUpperCasesTheFirstLetter() {
        assertEquals("Foo", property("foo", "getFoo").capitalizedName());
    }

    @Test
    @DisplayName("The capitalized name keeps the rest of the property name as it is")
    void theCapitalizedNameKeepsTheRestOfThePropertyName() {
        assertEquals("FooBar", property("fooBar", "getFooBar").capitalizedName());
    }

    @Test
    @DisplayName("The capitalized name of an empty property name is empty")
    void theCapitalizedNameOfAnEmptyNameIsEmpty() {
        assertEquals("", property("", "get").capitalizedName());
    }
}
