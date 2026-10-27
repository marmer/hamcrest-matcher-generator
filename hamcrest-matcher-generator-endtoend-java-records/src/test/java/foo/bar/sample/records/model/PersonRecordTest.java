/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

import static foo.bar.sample.records.model.PersonRecordMatcher.isPersonRecord;
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
                isPersonRecord()
                        .checkingAllProperties()
                        .withFirstName("Alice")
                        .withAge(30)
                        .ignoringClass());
    }

    @Test
    @DisplayName("Completeness check fails for a property that is neither checked nor ignored")
    void completenessCheckFailsForAPropertyThatIsNeitherCheckedNorIgnored() {
        PersonRecord person = new PersonRecord("Alice", 30);

        AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        person,
                                        isPersonRecord()
                                                .checkingAllProperties()
                                                .withFirstName("Alice")
                                                .withAge(30)));

        assertThat(error.getMessage(), endsWith("neither checked nor ignored: class"));
    }

    @Test
    @DisplayName("Generated matcher rejects record with wrong component value")
    void generatedMatcherRejectsRecordWithWrongComponentValue() {
        PersonRecord person = new PersonRecord("Bob", 25);

        assertThat(person, not(isPersonRecord().withFirstName("Alice")));
        assertThat(person, not(isPersonRecord().withAge(99)));
    }
}
