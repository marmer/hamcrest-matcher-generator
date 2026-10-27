/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

import static foo.bar.sample.records.model.PersonRecordMatcher.isPersonRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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
    @DisplayName("Generated matcher rejects record with wrong component value")
    void generatedMatcherRejectsRecordWithWrongComponentValue() {
        PersonRecord person = new PersonRecord("Bob", 25);

        assertThat(person, not(isPersonRecord().withFirstName("Alice")));
        assertThat(person, not(isPersonRecord().withAge(99)));
    }
}
