/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj;

import static foo.bar.sample.assertj.model.ParcelAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import foo.bar.sample.assertj.model.Address;
import foo.bar.sample.assertj.model.AddressAssert;
import foo.bar.sample.assertj.model.Parcel;
import foo.bar.sample.assertj.model.ParcelAssert;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordAssertionTest {

    private static final Parcel PARCEL =
            new Parcel("DHL", new Address("1 Rue de Rivoli", "Paris"), 500);

    @Test
    @DisplayName("The components of a record are asserted with hasFoo")
    void theComponentsOfARecordAreAssertedWithHasFoo() {
        assertThat(PARCEL)
                .hasCarrier("DHL")
                .hasWeightInGrams(500)
                .hasDestination(new Address("1 Rue de Rivoli", "Paris"));
    }

    @Test
    @DisplayName("A wrong record component fails with the path from the root type")
    void aWrongRecordComponentFailsWithThePathFromTheRootType() {
        assertThatThrownBy(() -> assertThat(PARCEL).hasWeightInGrams(600))
                .hasMessage(
                        String.format(
                                "%nExpecting Parcel.weightInGrams to be:%n  600%nbut was:%n  500"));
    }

    @Test
    @DisplayName("A nested record fails with the path through the record")
    void aNestedRecordFailsWithThePathThroughTheRecord() {
        assertThatThrownBy(
                        () ->
                                assertThat(PARCEL)
                                        .hasDestinationSatisfying(
                                                destination ->
                                                        AddressAssert.assertThat(destination)
                                                                .hasCity("Berlin")))
                .hasMessage(
                        String.format(
                                "%nExpecting Parcel.destination.city to be:%n  \"Berlin\"%nbut was:%n  \"Paris\""));
    }

    @Test
    @DisplayName("The generated assertion classes are open for soft assertion proxies")
    void theGeneratedAssertionClassesAreOpenForSoftAssertionProxies() throws NoSuchMethodException {
        assertThat(PARCEL).isNotNull();
        final Class<?> concrete = ParcelAssert.class;
        org.junit.jupiter.api.Assertions.assertFalse(Modifier.isFinal(concrete.getModifiers()));
        org.junit.jupiter.api.Assertions.assertTrue(
                Modifier.isPublic(concrete.getConstructor(Parcel.class).getModifiers()));
        org.junit.jupiter.api.Assertions.assertEquals(
                "org.assertj.core.api.AbstractObjectAssert",
                concrete.getSuperclass().getSuperclass().getName());
    }

    @Test
    @DisplayName("No generated class is named Assertions or SoftAssertions")
    void noGeneratedClassIsNamedAssertionsOrSoftAssertions() {
        org.junit.jupiter.api.Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("foo.bar.sample.assertj.model.Assertions"));
        org.junit.jupiter.api.Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("foo.bar.sample.assertj.model.SoftAssertions"));
    }
}
