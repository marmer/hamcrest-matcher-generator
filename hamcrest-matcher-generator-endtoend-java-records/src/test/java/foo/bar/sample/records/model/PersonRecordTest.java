/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

import static foo.bar.sample.records.model.PersonRecordMatcher.isPersonRecord;
import static foo.bar.sample.records.model.ShippingLabelMatcher.isShippingLabel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PersonRecordTest {

    @Test
    @DisplayName("Generated matcher matches record by component values")
    void generatedMatcherMatchesRecordByComponentValues() {
        PersonRecord person = new PersonRecord("Alice", 30);

        assertThat(
                person,
                isPersonRecord()
                        .withFirstName("Alice")
                        .withFirstName(is(equalTo("Alice")))
                        .withAge(30)
                        .withAge(is(30)));
    }

    @Test
    @DisplayName("Completeness check passes when every property is checked or ignored")
    void completenessCheckPassesWhenEveryPropertyIsCheckedOrIgnored() {
        PersonRecord person = new PersonRecord("Alice", 30);

        assertThat(
                person,
                isPersonRecord().checkingAllProperties().withFirstName("Alice").withAge(30));
    }

    @Test
    @DisplayName("Completeness check fails for a component that is neither checked nor ignored")
    void completenessCheckFailsForAComponentThatIsNeitherCheckedNorIgnored() {
        PersonRecord person = new PersonRecord("Alice", 30);

        AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        person,
                                        isPersonRecord()
                                                .checkingAllProperties()
                                                .withFirstName("Alice")));

        assertThat(error.getMessage(), endsWith("neither checked nor ignored: age"));
    }

    @Test
    @DisplayName("Completeness check fails when a newly added component is left unaddressed")
    void completenessCheckFailsWhenANewComponentIsLeftUnaddressed() {
        ShippingLabel label = new ShippingLabel("Ada", "Main Street 1", "Berlin");

        assertThat(
                label,
                isShippingLabel()
                        .checkingAllProperties()
                        .withRecipient("Ada")
                        .withStreet("Main Street 1")
                        .withCity("Berlin"));

        AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        label,
                                        isShippingLabel()
                                                .checkingAllProperties()
                                                .withRecipient("Ada")
                                                .withStreet("Main Street 1")));

        assertThat(error.getMessage(), endsWith("neither checked nor ignored: city"));
    }

    @Test
    @DisplayName("Generated matcher rejects record with wrong component value")
    void generatedMatcherRejectsRecordWithWrongComponentValue() {
        PersonRecord person = new PersonRecord("Bob", 25);

        assertThat(person, not(isPersonRecord().withFirstName("Alice")));
        assertThat(person, not(isPersonRecord().withAge(99)));
    }
}
