/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReferencePresetPlannerTest {

    private static final String PACKAGE = "io.github.marmer.matchergenerator.hamcrest";
    private static final String REFERENCE_MATCHING = PACKAGE + ".ReferenceMatching";
    private static final String ADDRESS = PACKAGE + ".BeanFixtures.Address";
    private static final String ADDRESS_MATCHER = PACKAGE + ".AddressMatcher::isAddressLike";
    private static final String CUSTOMER = PACKAGE + ".BeanFixtures.Customer";
    private static final String COLOR = PACKAGE + ".BeanFixtures.Color";
    private static final String CONTACT = PACKAGE + ".BeanFixtures.Contact";
    private static final String MARKER = PACKAGE + ".ReferencePresetPlannerTest.Marker";

    private final List<String> verboseNotes = new ArrayList<>();

    /** A class of the test class path without any property, only {@code getClass()}. */
    static final class Marker {}

    private static PropertyModel property(
            final String name, final String accessor, final TypeMirror type) {
        return new PropertyModel(name, type, accessor, Nullability.UNSPECIFIED, false);
    }

    /** A property whose accessor declares a checked exception. */
    private static PropertyModel throwingProperty(
            final String name, final String accessor, final TypeMirror type) {
        return new PropertyModel(name, type, accessor, Nullability.UNSPECIFIED, true);
    }

    /** A type of the class path with the given properties and nested types. */
    private static TypeModel generated(
            final String qualifiedName,
            final List<PropertyModel> properties,
            final List<TypeModel> nestedTypes) {
        final String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        return new TypeModel(
                qualifiedName,
                simpleName,
                PACKAGE,
                PACKAGE,
                List.of(),
                TypeModel.Kind.POJO,
                TypeMirrors.typeNamed(qualifiedName),
                properties,
                nestedTypes);
    }

    /** A generated type with the given properties, which makes it compared deeply. */
    private static TypeModel generated(
            final String qualifiedName, final PropertyModel... properties) {
        return generated(qualifiedName, List.of(properties), List.of());
    }

    /** The address, a generated type with a property of its own. */
    private static TypeModel addressModel() {
        return generated(ADDRESS, property("city", "getCity", TypeMirrors.stringType()));
    }

    /**
     * The planner for a configured type, in a round that generated the owner and the other given
     * types, with the notes it reports.
     */
    private ReferencePresetPlanner plannerFor(final TypeModel owner, final TypeModel... others) {
        final List<TypeModel> generatedTypes = new ArrayList<>(List.of(owner));
        generatedTypes.addAll(List.of(others));
        return new ReferencePresetPlanner(owner, generatedTypes, verboseNotes::add);
    }

    @Test
    @DisplayName("A property of a generated type is preset with the matcher of that type")
    void aPropertyOfAGeneratedTypeIsPresetWithItsMatcher() {
        final PropertyModel address =
                property("address", "getAddress", TypeMirrors.typeNamed(ADDRESS));
        final TypeModel customer = generated(CUSTOMER, address);

        assertEquals(
                REFERENCE_MATCHING + ".nested(reference.getAddress(), " + ADDRESS_MATCHER + ")",
                plannerFor(customer, addressModel()).presetOf(address).toString());
        assertEquals(List.of(), verboseNotes);
    }

    @Test
    @DisplayName(
            "A property of a type nested in a generated type is preset with the nested matcher")
    void aPropertyOfANestedGeneratedTypeIsPresetWithTheNestedMatcher() {
        final PropertyModel address =
                property("address", "getAddress", TypeMirrors.typeNamed(ADDRESS));
        final TypeModel customer = generated(CUSTOMER, List.of(address), List.of(addressModel()));

        assertEquals(
                REFERENCE_MATCHING
                        + ".nested(reference.getAddress(), "
                        + PACKAGE
                        + ".CustomerMatcher.AddressMatcher::isAddressLike)",
                plannerFor(customer).presetOf(address).toString());
        assertEquals(List.of(), verboseNotes);
    }

    @Test
    @DisplayName("A property of a generated type without properties is compared by equals silently")
    void aPropertyOfAGeneratedTypeWithoutPropertiesIsComparedByEquals() {
        final PropertyModel marker = property("marker", "getMarker", TypeMirrors.typeNamed(MARKER));
        final TypeModel customer = generated(CUSTOMER, marker);

        assertEquals(
                REFERENCE_MATCHING + ".value(reference.getMarker())",
                plannerFor(customer, generated(MARKER)).presetOf(marker).toString());
        assertEquals(List.of(), verboseNotes);
    }

    @Test
    @DisplayName("An array of a generated type is preset element by element with its matcher")
    void anArrayOfAGeneratedTypeIsPresetWithItsMatcher() {
        final PropertyModel addresses =
                property("addresses", "getAddresses", TypeMirrors.arrayOf(ADDRESS));
        final TypeModel customer = generated(CUSTOMER, addresses);

        assertEquals(
                REFERENCE_MATCHING + ".array(reference.getAddresses(), " + ADDRESS_MATCHER + ")",
                plannerFor(customer, addressModel()).presetOf(addresses).toString());
    }

    @Test
    @DisplayName("A list of a generated type is preset with its matcher")
    void aListOfAGeneratedTypeIsPresetWithItsMatcher() {
        final PropertyModel addresses =
                property("addresses", "getAddresses", TypeMirrors.listOf(ADDRESS));
        final TypeModel customer = generated(CUSTOMER, addresses);

        assertEquals(
                REFERENCE_MATCHING + ".list(reference.getAddresses(), " + ADDRESS_MATCHER + ")",
                plannerFor(customer, addressModel()).presetOf(addresses).toString());
    }

    @Test
    @DisplayName("A collection of a generated type is preset with its matcher")
    void aCollectionOfAGeneratedTypeIsPresetWithItsMatcher() {
        final PropertyModel addresses =
                property("addresses", "getAddresses", TypeMirrors.collectionOf(ADDRESS));
        final TypeModel customer = generated(CUSTOMER, addresses);

        assertEquals(
                REFERENCE_MATCHING
                        + ".collection(reference.getAddresses(), "
                        + ADDRESS_MATCHER
                        + ")",
                plannerFor(customer, addressModel()).presetOf(addresses).toString());
    }

    @Test
    @DisplayName("A map of a generated value type is preset with the matcher of its values")
    void aMapOfAGeneratedValueTypeIsPresetWithItsMatcher() {
        final PropertyModel addressesByName =
                property(
                        "addressesByName",
                        "getAddressesByName",
                        TypeMirrors.mapOf("java.lang.String", ADDRESS));
        final TypeModel customer = generated(CUSTOMER, addressesByName);

        assertEquals(
                REFERENCE_MATCHING
                        + ".map(reference.getAddressesByName(), "
                        + ADDRESS_MATCHER
                        + ")",
                plannerFor(customer, addressModel()).presetOf(addressesByName).toString());
    }

    @Test
    @DisplayName("An enum is compared by equals and gets no verbose note")
    void anEnumIsComparedByEquals() {
        final PropertyModel color = property("color", "getColor", TypeMirrors.typeNamed(COLOR));
        final TypeModel customer = generated(CUSTOMER, color);

        assertEquals(
                REFERENCE_MATCHING + ".value(reference.getColor())",
                plannerFor(customer).presetOf(color).toString());
        assertEquals(List.of(), verboseNotes);
    }

    @Test
    @DisplayName("A java type is compared by equals and gets no verbose note")
    void aJavaTypeIsComparedByEquals() {
        final PropertyModel createdOn =
                property("createdOn", "getCreatedOn", TypeMirrors.typeNamed("java.time.LocalDate"));
        final TypeModel customer = generated(CUSTOMER, createdOn);

        assertEquals(
                REFERENCE_MATCHING + ".value(reference.getCreatedOn())",
                plannerFor(customer).presetOf(createdOn).toString());
        assertEquals(List.of(), verboseNotes);
    }

    @Test
    @DisplayName("A list of a wildcard bounded generated type is preset with its matcher")
    void aListOfAWildcardBoundedGeneratedTypeIsPresetWithItsMatcher() {
        final PropertyModel addresses =
                property("addresses", "getAddresses", TypeMirrors.listOfExtending(ADDRESS));
        final TypeModel customer = generated(CUSTOMER, addresses);

        assertEquals(
                REFERENCE_MATCHING + ".list(reference.getAddresses(), " + ADDRESS_MATCHER + ")",
                plannerFor(customer, addressModel()).presetOf(addresses).toString());
    }

    @Test
    @DisplayName(
            "A user type no generated type stands for is compared by equals with a verbose note")
    void anUnmatchedUserTypeIsComparedByEqualsWithAVerboseNote() {
        final PropertyModel contact =
                property("contact", "getContact", TypeMirrors.typeNamed(CONTACT));
        final TypeModel customer = generated(CUSTOMER, contact);

        assertEquals(
                REFERENCE_MATCHING + ".value(reference.getContact())",
                plannerFor(customer).presetOf(contact).toString());
        assertEquals(
                List.of(
                        "isCustomerLike compares the property 'contact' with equals: no matcher for "
                                + CONTACT
                                + " is generated in this processing round"),
                verboseNotes);
    }

    @Test
    @DisplayName(
            "A property whose getter declares a checked exception is read through the read helper")
    void aPropertyWhoseGetterDeclaresACheckedExceptionIsReadThroughTheReadHelper() {
        final PropertyModel name = throwingProperty("name", "getName", TypeMirrors.stringType());
        final TypeModel customer = generated(CUSTOMER, name);

        assertEquals(
                REFERENCE_MATCHING
                        + ".value("
                        + REFERENCE_MATCHING
                        + ".read(\"name\", reference::getName))",
                plannerFor(customer).presetOf(name).toString());
        assertEquals(List.of(), verboseNotes);
    }

    @Test
    @DisplayName(
            "A generated type whose getter declares a checked exception is read before it is preset")
    void aGeneratedTypeWhoseGetterDeclaresACheckedExceptionIsReadBeforeItIsPreset() {
        final PropertyModel address =
                throwingProperty("address", "getAddress", TypeMirrors.typeNamed(ADDRESS));
        final TypeModel customer = generated(CUSTOMER, address);

        assertEquals(
                REFERENCE_MATCHING
                        + ".nested("
                        + REFERENCE_MATCHING
                        + ".read(\"address\", reference::getAddress), "
                        + ADDRESS_MATCHER
                        + ")",
                plannerFor(customer, addressModel()).presetOf(address).toString());
    }
}
