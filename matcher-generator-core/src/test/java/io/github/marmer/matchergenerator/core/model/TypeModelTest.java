/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

import static io.github.marmer.matchergenerator.core.model.ModelTestSupport.anyType;
import static io.github.marmer.matchergenerator.core.model.ModelTestSupport.property;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TypeModelTest {

    @Test
    @DisplayName("The accessor properties keep the first property of every name")
    void theAccessorPropertiesKeepTheFirstPropertyOfEveryName() {
        final PropertyModel first = property("foo", "getFoo");
        final PropertyModel second = property("foo", "isFoo");

        final List<PropertyModel> properties = typeWith(first, second).accessorProperties();

        assertEquals(List.of(first), properties);
    }

    @Test
    @DisplayName("The accessor properties drop the getClass accessor of Object")
    void theAccessorPropertiesDropTheGetClassAccessor() {
        final PropertyModel foo = property("foo", "getFoo");

        final List<PropertyModel> properties =
                typeWith(property("class", "getClass"), foo).accessorProperties();

        assertEquals(List.of(foo), properties);
    }

    @Test
    @DisplayName("The accessor properties keep the detection order")
    void theAccessorPropertiesKeepTheDetectionOrder() {
        final PropertyModel bar = property("bar", "getBar");
        final PropertyModel foo = property("foo", "getFoo");
        final PropertyModel baz = property("baz", "getBaz");

        final List<PropertyModel> properties = typeWith(bar, foo, baz).accessorProperties();

        assertEquals(List.of(bar, foo, baz), properties);
    }

    @Test
    @DisplayName("The properties keep the getClass accessor and the duplicate names")
    void thePropertiesKeepTheGetClassAccessorAndTheDuplicateNames() {
        final TypeModel model =
                typeWith(
                        property("class", "getClass"),
                        property("foo", "getFoo"),
                        property("foo", "isFoo"));

        assertEquals(3, model.properties().size());
    }

    private static TypeModel typeWith(final PropertyModel... properties) {
        return new TypeModel(
                "some.pck.Some",
                "Some",
                "some.pck",
                "some.pck",
                List.of(),
                TypeModel.Kind.POJO,
                anyType(),
                List.of(properties),
                List.of());
    }
}
