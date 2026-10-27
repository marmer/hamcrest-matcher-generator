/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isOrder;
import static io.github.marmer.matchergenerator.hamcrest.BeanFixtures.isOrderLine;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.entries;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInAnyOrder;
import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInOrder;
import static io.github.marmer.matchergenerator.hamcrest.GoldenMessages.messageOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Color;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Order;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.OrderLine;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.TestMatcher;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

/** Mismatch messages are compared with golden files under {@code CollectionMatchersTest/}. */
class CollectionMatchersTest {
    private static final OrderLine LINE_1 = new OrderLine("A-1", 1);
    private static final OrderLine LINE_2 = new OrderLine("B-2", 2);
    private static final OrderLine LINE_3 = new OrderLine("C-3", 5);

    private final GoldenMessages golden = new GoldenMessages("CollectionMatchersTest");

    private static Order orderWithLines(final OrderLine... lines) {
        return Order.builder().lines(List.of(lines)).lineArray(lines).build();
    }

    private static Order orderWithBySku(final Map<String, OrderLine> bySku) {
        return Order.builder().bySku(bySku).build();
    }

    private static TestMatcher<OrderLine> line(final String sku, final int quantity) {
        return isOrderLine().with("sku", equalTo(sku)).with("quantity", equalTo(quantity));
    }

    @Test
    void itemsInOrderReportsTheSizeAndTheIndexedPathOfEveryFailingItem() throws IOException {
        golden.assertMessage(
                "items-in-order",
                messageOf(
                        orderWithLines(LINE_1, LINE_2, LINE_3),
                        isOrder().with("lines", itemsInOrder(line("A-1", 1), line("B-2", 3)))));
    }

    @Test
    void itemsInOrderChecksTheItemsBothListsHaveEvenWhenTheSizeDiffers() throws IOException {
        golden.assertMessage(
                "items-in-order-fewer-items",
                messageOf(
                        orderWithLines(LINE_1),
                        isOrder()
                                .with(
                                        "lines",
                                        itemsInOrder(
                                                line("X-9", 1), line("B-2", 2), line("C-3", 5)))));
    }

    @Test
    void itemsInOrderDescribesASingleItemInTheSingular() throws IOException {
        golden.assertMessage(
                "items-in-order-one-item",
                messageOf(
                        orderWithLines(LINE_1, LINE_2),
                        isOrder().with("lines", itemsInOrder(line("A-1", 1)))));
    }

    @Test
    void itemsInOrderDescribesOtherMatchersOfItemsByTheMatcher() throws IOException {
        golden.assertMessage(
                "items-in-order-foreign-matcher",
                messageOf(
                        Order.builder().tags(List.of("a", "b")).build(),
                        isOrder().with("tags", itemsInOrder(startsWith("a"), startsWith("c")))));
    }

    @Test
    void itemsInOrderReportsAnItemOfAnotherTypeAsAWholeExpectation() throws IOException {
        golden.assertMessage(
                "items-in-order-null-item",
                messageOf(
                        Order.builder().lineArray(new OrderLine[] {LINE_1, null}).build(),
                        isOrder().with("lineArray", itemsInOrder(line("A-1", 1), line("B-2", 2)))));
    }

    @Test
    void itemsInOrderTreatsArraysLikeLists() throws IOException {
        golden.assertMessage(
                "items-in-order-array",
                messageOf(
                        orderWithLines(LINE_1, LINE_2, LINE_3),
                        isOrder()
                                .with(
                                        "lineArray",
                                        itemsInOrder(
                                                line("A-1", 1),
                                                line("B-2", 3),
                                                line("C-3", 5),
                                                line("D-4", 1)))));
    }

    @Test
    void itemsInOrderReportsAValueItCannotCheck() throws IOException {
        golden.assertMessage(
                "items-in-order-no-list",
                messageOf(
                        Order.builder().tags(null).build(),
                        isOrder().with("tags", itemsInOrder(equalTo("a"), equalTo("b")))));
    }

    @Test
    void itemsInOrderOnItsOwnReportsTheFailuresWithoutAPrefix() throws IOException {
        golden.assertMessage(
                "items-in-order-top-level",
                messageOf(
                        List.of(LINE_1, LINE_2, LINE_3),
                        itemsInOrder(line("A-1", 1), line("B-2", 3))));
    }

    @Test
    void itemsInOrderWithoutItemsIsDescribedAsNoItems() throws IOException {
        golden.assertMessage(
                "items-in-order-empty",
                messageOf(orderWithLines(LINE_1), isOrder().with("lines", itemsInOrder())));
    }

    @Test
    void itemsInOrderReportsACollectionThatIsNoList() throws IOException {
        golden.assertMessage(
                "items-in-order-no-list-value",
                messageOf(
                        Order.builder().bySku(Map.of("A-1", LINE_1)).build(),
                        isOrder().with("bySku", itemsInOrder(line("A-1", 1)))));
    }

    @Test
    void itemsInOrderMatchesTheItemsInOrder() {
        assertThat(
                orderWithLines(LINE_1, LINE_2),
                isOrder().with("lines", itemsInOrder(line("A-1", 1), line("B-2", 2))));
        assertThat(List.of("a", "b"), itemsInOrder(List.of(equalTo("a"), equalTo("b"))));
        assertThat(new int[] {1, 2}, itemsInOrder(equalTo(1), equalTo(2)));
    }

    @Test
    void itemsInAnyOrderNamesTheMissingItemAndTheClosestCandidate() throws IOException {
        golden.assertMessage(
                "items-in-any-order-closest",
                messageOf(
                        orderWithLines(LINE_1, LINE_2),
                        isOrder().with("lines", itemsInAnyOrder(line("B-2", 3), line("A-1", 1)))));
    }

    @Test
    void itemsInAnyOrderNamesTheDifferencesOfTheClosestCandidate() throws IOException {
        golden.assertMessage(
                "items-in-any-order-closest-several-differences",
                messageOf(
                        orderWithLines(LINE_1, LINE_2),
                        isOrder().with("lines", itemsInAnyOrder(line("B-2", 3), line("X-9", 1)))));
    }

    @Test
    void itemsInAnyOrderPicksTheCandidateWithTheFewestDifferences() throws IOException {
        golden.assertMessage(
                "items-in-any-order-fewest-differences",
                messageOf(
                        orderWithLines(
                                new OrderLine("X-1", 9),
                                new OrderLine("B-2", 9),
                                new OrderLine("Y-1", 9),
                                LINE_3),
                        isOrder().with("lines", itemsInAnyOrder(line("B-2", 3), line("C-3", 5)))));
    }

    @Test
    void itemsInAnyOrderFindsNoClosestCandidateAmongItemsOfAnotherType() throws IOException {
        golden.assertMessage(
                "items-in-any-order-other-type",
                messageOf(
                        Order.builder().tags(List.of("a")).build(),
                        isOrder().with("tags", itemsInAnyOrder(line("A-1", 1)))));
    }

    @Test
    void itemsInAnyOrderNamesUnexpectedItems() throws IOException {
        golden.assertMessage(
                "items-in-any-order-unexpected",
                messageOf(
                        orderWithLines(LINE_1, LINE_2, LINE_3),
                        isOrder().with("lines", itemsInAnyOrder(line("A-1", 1), line("B-2", 2)))));
    }

    @Test
    void itemsInAnyOrderNamesMissingAndUnexpectedItemsWithoutAClosestCandidateForOtherMatchers()
            throws IOException {
        golden.assertMessage(
                "items-in-any-order-foreign-matcher",
                messageOf(
                        Order.builder().tags(List.of("a", "b")).build(),
                        isOrder().with("tags", itemsInAnyOrder(startsWith("a"), startsWith("c")))));
    }

    @Test
    void itemsInAnyOrderNeedsAnItemOfItsOwnForEveryMatcher() throws IOException {
        golden.assertMessage(
                "items-in-any-order-own-item",
                messageOf(
                        orderWithLines(LINE_1),
                        isOrder()
                                .with(
                                        "lines",
                                        itemsInAnyOrder(
                                                isOrderLine().with("sku", startsWith("A")),
                                                isOrderLine().with("sku", startsWith("A"))))));
    }

    @Test
    void itemsInAnyOrderTreatsArraysLikeCollections() throws IOException {
        golden.assertMessage(
                "items-in-any-order-array",
                messageOf(
                        orderWithLines(LINE_1, LINE_2),
                        isOrder()
                                .with(
                                        "lineArray",
                                        itemsInAnyOrder(line("B-2", 3), line("A-1", 1)))));
    }

    @Test
    void itemsInAnyOrderReportsAValueItCannotCheck() throws IOException {
        golden.assertMessage(
                "items-in-any-order-no-collection",
                messageOf(
                        Order.builder().tags(null).build(),
                        isOrder().with("tags", itemsInAnyOrder(equalTo("a")))));
    }

    @Test
    void itemsInAnyOrderWithoutItemsIsDescribedAsNoItems() throws IOException {
        golden.assertMessage(
                "items-in-any-order-empty",
                messageOf(orderWithLines(LINE_1), isOrder().with("lines", itemsInAnyOrder())));
    }

    @Test
    void itemsInAnyOrderReportsAValueThatIsNoCollection() throws IOException {
        golden.assertMessage(
                "items-in-any-order-no-collection-value",
                messageOf(
                        Order.builder().bySku(Map.of("A-1", LINE_1)).build(),
                        isOrder().with("bySku", itemsInAnyOrder(line("A-1", 1)))));
    }

    @Test
    void itemsInAnyOrderMatchesTheItemsInAnyOrder() {
        assertThat(
                orderWithLines(LINE_1, LINE_2),
                isOrder().with("lines", itemsInAnyOrder(line("B-2", 2), line("A-1", 1))));
        assertThat(Set.of("a", "b"), itemsInAnyOrder(List.of(equalTo("b"), equalTo("a"))));
        assertThat(new String[] {"a", "b"}, itemsInAnyOrder(equalTo("b"), equalTo("a")));
    }

    @Test
    void itemsInAnyOrderAssignsItemsSoThatEveryMatcherFindsItsOwnWheneverPossible() {
        // the first matcher fits both items, the second one only the first item
        assertThat(List.of("ab", "ac"), itemsInAnyOrder(startsWith("a"), equalTo("ab")));
        assertThat(
                List.of("ab", "ac", "ad"),
                itemsInAnyOrder(startsWith("a"), startsWith("a"), equalTo("ab")));
    }

    @Test
    void entriesNamesTheKeyAndPathOfDifferingValues() throws IOException {
        golden.assertMessage(
                "entries-differing-value",
                messageOf(
                        orderWithBySku(Map.of("A-1", LINE_1, "B-2", LINE_2)),
                        isOrder()
                                .with(
                                        "bySku",
                                        entries("A-1", line("A-1", 1), "B-2", line("B-2", 3)))));
    }

    @Test
    void entriesNamesMissingAndUnexpectedKeys() throws IOException {
        final Map<String, OrderLine> bySku = new LinkedHashMap<>();
        bySku.put("A-1", LINE_1);
        bySku.put("C-3", LINE_3);
        golden.assertMessage(
                "entries-missing-and-unexpected",
                messageOf(
                        orderWithBySku(bySku),
                        isOrder()
                                .with(
                                        "bySku",
                                        entries("A-1", line("A-1", 1), "D-4", line("D-4", 4)))));
    }

    @Test
    void entriesQuoteStringKeysAndLeaveOtherKeysUnquoted() throws IOException {
        final Map<Color, OrderLine> byColor = new LinkedHashMap<>();
        byColor.put(Color.RED, LINE_1);
        byColor.put(Color.GREEN, LINE_2);
        final Map<Integer, OrderLine> byId = new LinkedHashMap<>();
        byId.put(42, LINE_1);
        byId.put(43, LINE_3);
        golden.assertMessage(
                "entries-key-types",
                messageOf(
                        Order.builder().byColor(byColor).byId(byId).build(),
                        isOrder()
                                .with(
                                        "byColor",
                                        entries(
                                                Color.RED,
                                                line("A-1", 2),
                                                Color.BLUE,
                                                line("B-2", 2)))
                                .with("byId", entries(42, line("A-1", 1), 44, line("D-4", 4)))));
    }

    @Test
    void entriesDescribeKeysOfGeneratedTypesFromTheirProperties() throws IOException {
        isOrderLine();
        final Map<OrderLine, String> byLine = new LinkedHashMap<>();
        byLine.put(LINE_1, "first");
        byLine.put(LINE_2, "second");
        golden.assertMessage(
                "entries-generated-keys",
                messageOf(
                        Order.builder().byLine(byLine).build(),
                        isOrder()
                                .with(
                                        "byLine",
                                        entries(
                                                LINE_1,
                                                equalTo("1st"),
                                                LINE_3,
                                                equalTo("third")))));
    }

    @Test
    void entriesReportAValueItCannotCheck() throws IOException {
        golden.assertMessage(
                "entries-no-map",
                messageOf(
                        orderWithLines(LINE_1),
                        isOrder().with("lines", entries("A-1", line("A-1", 1)))));
    }

    @Test
    void entriesWithoutKeysAreDescribedAsAnEmptyMap() throws IOException {
        golden.assertMessage(
                "entries-empty",
                messageOf(
                        orderWithBySku(Map.of("A-1", LINE_1)),
                        isOrder().with("bySku", entries(Map.<String, Matcher<Object>>of()))));
    }

    @Test
    void entriesOnItsOwnReportTheFailuresWithoutAPrefix() throws IOException {
        golden.assertMessage(
                "entries-top-level",
                messageOf(
                        Map.of("A-1", LINE_1, "B-2", LINE_2),
                        entries("A-1", line("A-1", 1), "C-3", line("C-3", 3))));
    }

    @Test
    void entriesMatchExactlyTheirKeys() {
        final Map<String, OrderLine> bySku = new LinkedHashMap<>();
        bySku.put("A-1", LINE_1);
        bySku.put("B-2", LINE_2);
        bySku.put("C-3", LINE_3);
        bySku.put("D-4", new OrderLine("D-4", 4));
        assertThat(Map.of("A-1", LINE_1), entries("A-1", line("A-1", 1)));
        assertThat(
                bySku,
                entries(
                        "A-1",
                        line("A-1", 1),
                        "B-2",
                        line("B-2", 2),
                        "C-3",
                        line("C-3", 5),
                        "D-4",
                        line("D-4", 4)));
        assertThat(
                bySku,
                entries(
                        Map.of(
                                "A-1", line("A-1", 1),
                                "B-2", line("B-2", 2),
                                "C-3", line("C-3", 5),
                                "D-4", line("D-4", 4))));
    }

    @Test
    void entriesMatchThreeKeys() {
        assertThat(
                Map.of("A-1", LINE_1, "B-2", LINE_2, "C-3", LINE_3),
                entries("A-1", line("A-1", 1), "B-2", line("B-2", 2), "C-3", line("C-3", 5)));
    }

    @Test
    void aKeyGivenTwiceIsRejected() {
        final IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> entries("A-1", equalTo(1), "A-1", equalTo(2)));

        assertEquals("Key given twice: A-1", exception.getMessage());
    }

    @Test
    void collectionMatchersInsideForeignMatchersRenderOnOneLine() throws IOException {
        golden.assertMessage(
                "inside-foreign-matcher",
                messageOf(
                        Order.builder().batches(List.of(List.of(LINE_1, LINE_2, LINE_3))).build(),
                        isOrder()
                                .with(
                                        "batches",
                                        contains(itemsInOrder(line("A-1", 1), line("B-2", 3))))));
    }

    /**
     * A foreign matcher that holds the collection matcher is called by Hamcrest, not by a generated
     * matcher, so the runtime cannot know it is inside a foreign matcher and keeps the block
     * format.
     */
    @Test
    void aCollectionMatcherInsideAForeignMatcherAtTheTopLevelKeepsTheBlockFormat()
            throws IOException {
        golden.assertMessage(
                "inside-foreign-matcher-top-level",
                messageOf(
                        List.of(List.of(LINE_1, LINE_2, LINE_3)),
                        contains(itemsInOrder(line("A-1", 1), line("B-2", 3)))));
    }

    @Test
    void matchersWithoutItemsOrKeysDescribeThemselvesAsEmpty() throws IOException {
        golden.assertMessage("empty-items-in-order", messageOf(List.of(LINE_1), itemsInOrder()));
        golden.assertMessage(
                "empty-items-in-any-order", messageOf(List.of(LINE_1), itemsInAnyOrder()));
        golden.assertMessage(
                "empty-entries",
                messageOf(Map.of("A-1", LINE_1), entries(Map.<String, Matcher<Object>>of())));
    }
}
