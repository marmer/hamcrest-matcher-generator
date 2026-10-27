/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

import static foo.bar.sample.records.model.CategoryNodeMatcher.isCategoryNodeLike;
import static foo.bar.sample.records.model.OrderRecordMatcher.isOrderRecordLike;
import static foo.bar.sample.records.model.ShelfRecordMatcher.isShelfRecordLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReferenceObjectTest {

    private static OrderRecord order(final String email, final String city) {
        return new OrderRecord(email, new CustomerRecord("Ada", new AddressRecord(city)));
    }

    private static ShelfRecord shelf(final String code) {
        final BookRecord book = new BookRecord(code, "Title " + code);
        return new ShelfRecord(
                "main", List.of(book), Set.of(book), Map.of(code, book), new BookRecord[] {book});
    }

    @Test
    @DisplayName("An equal copy of the reference object matches")
    void anEqualCopyOfTheReferenceObjectMatches() {
        assertThat(
                order("ada@example.org", "Paris"),
                isOrderRecordLike(order("ada@example.org", "Paris")));
    }

    @Test
    @DisplayName("A nested difference is reported with its full path")
    void aNestedDifferenceIsReportedWithItsFullPath() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        order("ada@example.org", "Berlin"),
                                        isOrderRecordLike(order("ada@example.org", "Paris"))));

        assertEquals(
                """

            Expected: OrderRecord with 3 property checks, every property checked or ignored
                 but: customer.address.city: expected "Paris" but was "Berlin"
            """,
                error.getMessage() + "\n");
    }

    @Test
    @DisplayName("A single property of the reference comparison can be overridden")
    void aSinglePropertyOfTheReferenceComparisonCanBeOverridden() {
        assertThat(
                order("bob@example.org", "Paris"),
                isOrderRecordLike(order("ada@example.org", "Paris")).withEmail("bob@example.org"));
    }

    @Test
    @DisplayName("A single property of the reference comparison can be reset")
    void aSinglePropertyOfTheReferenceComparisonCanBeReset() {
        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        order("ada@example.org", "Paris"),
                                        isOrderRecordLike(order("ada@example.org", "Paris"))
                                                .resetEmail()));

        assertEquals(
                """

            Expected: OrderRecord with 2 property checks, every property checked or ignored
                 but: neither checked nor ignored: email
            """,
                error.getMessage() + "\n");
    }

    @Test
    @DisplayName("A single property of the reference comparison can be ignored")
    void aSinglePropertyOfTheReferenceComparisonCanBeIgnored() {
        assertThat(
                order("bob@example.org", "Paris"),
                isOrderRecordLike(order("ada@example.org", "Paris")).ignoringEmail());
    }

    @Test
    @DisplayName("Lists, other collections, maps and arrays are compared by their items")
    void listsOtherCollectionsMapsAndArraysAreComparedByTheirItems() {
        assertThat(shelf("1"), isShelfRecordLike(shelf("1")));

        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () -> assertThat(shelf("2"), isShelfRecordLike(shelf("1"))));

        assertEquals(
                """

            Expected: ShelfRecord with 8 property checks, every property checked or ignored
                 but: books[0].code: expected "1" but was "2"
                      books[0].title: expected "Title 1" but was "Title 2"
                      favourites: missing BookRecord {code "1", title "Title 1"}; closest BookRecord {code "2", title "Title 2"} differs in code: expected "1" but was "2", title: expected "Title 1" but was "Title 2"
                      byCode: missing key "1" with BookRecord {code "1", title "Title 1"}
                      byCode: unexpected key "2" with BookRecord {code "2", title "Title 2"}
                      featured[0].code: expected "1" but was "2"
                      featured[0].title: expected "Title 1" but was "Title 2"
            """,
                error.getMessage() + "\n");
    }

    @Test
    @DisplayName("A cyclic object graph is compared without running in circles")
    void aCyclicObjectGraphIsComparedWithoutRunningInCircles() {
        final CategoryNode reference =
                new CategoryNode("root").withChild(new CategoryNode("books"));

        assertThat(
                new CategoryNode("root").withChild(new CategoryNode("books")),
                isCategoryNodeLike(reference));

        final AssertionError error =
                assertThrows(
                        AssertionError.class,
                        () ->
                                assertThat(
                                        new CategoryNode("root")
                                                .withChild(new CategoryNode("toys")),
                                        isCategoryNodeLike(reference)));

        assertEquals(
                """

            Expected: CategoryNode with 4 property checks, every property checked or ignored
                 but: children[0].name: expected "books" but was "toys"
            """,
                error.getMessage() + "\n");
    }
}
