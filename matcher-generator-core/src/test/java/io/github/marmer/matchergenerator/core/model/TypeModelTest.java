/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

import static io.github.marmer.matchergenerator.core.model.ModelTestSupport.anyType;
import static io.github.marmer.matchergenerator.core.model.ModelTestSupport.property;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TypeModelTest {

    @Test
    @DisplayName("A property whose name no other property of the type has is not shared")
    void aPropertyWhoseNameNoOtherPropertyHasIsNotShared() {
        final PropertyModel foo = property("foo", "getFoo");
        final PropertyModel bar = property("bar", "getBar");

        final TypeModel model = typeWith(foo, bar);

        assertFalse(model.isNameShared(foo));
        assertFalse(model.isNameShared(bar));
    }

    @Test
    @DisplayName("Two properties with the same name share that name")
    void twoPropertiesWithTheSameNameShareThatName() {
        final PropertyModel getFoo = property("foo", "getFoo");
        final PropertyModel isFoo = property("foo", "isFoo");

        final TypeModel model = typeWith(getFoo, isFoo);

        assertTrue(model.isNameShared(getFoo));
        assertTrue(model.isNameShared(isFoo));
    }

    @Test
    @DisplayName("The getClass property next to a user property of another name is not shared")
    void theGetClassPropertyNextToAUserPropertyOfAnotherNameIsNotShared() {
        final PropertyModel getClassProperty = property("class", "getClass");
        final PropertyModel foo = property("foo", "getFoo");

        final TypeModel model = typeWith(getClassProperty, foo);

        assertFalse(model.isNameShared(getClassProperty));
        assertFalse(model.isNameShared(foo));
    }

    @Test
    @DisplayName("The getClass property shares the name class with a user property isClass")
    void theGetClassPropertySharesTheNameClassWithAUserPropertyIsClass() {
        final PropertyModel getClassProperty = property("class", "getClass");
        final PropertyModel isClass = property("class", "isClass");

        final TypeModel model = typeWith(getClassProperty, isClass);

        assertTrue(model.isNameShared(getClassProperty));
        assertTrue(model.isNameShared(isClass));
    }

    @Test
    @DisplayName("By accessors, a user property isClass is not shared with the getClass accessor")
    void byAccessorsAUserPropertyIsClassIsNotSharedWithGetClass() {
        final PropertyModel getClassProperty = property("class", "getClass");
        final PropertyModel isClass = property("class", "isClass");

        final TypeModel model = typeWith(getClassProperty, isClass);

        assertFalse(model.isNameSharedByAccessors(isClass));
    }

    @Test
    @DisplayName("By accessors, getFoo and isFoo still share the name foo")
    void byAccessorsGetFooAndIsFooStillShareTheName() {
        final PropertyModel getFoo = property("foo", "getFoo");
        final PropertyModel isFoo = property("foo", "isFoo");

        final TypeModel model = typeWith(getFoo, isFoo);

        assertTrue(model.isNameSharedByAccessors(getFoo));
        assertTrue(model.isNameSharedByAccessors(isFoo));
    }

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
