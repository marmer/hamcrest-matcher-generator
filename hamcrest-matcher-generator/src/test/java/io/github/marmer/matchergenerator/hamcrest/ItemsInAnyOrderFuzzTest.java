/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.code_intelligence.jazzer.api.CannedFuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks {@code itemsInAnyOrder} against a brute-force oracle: it matches exactly when every
 * matcher can get an item of its own and no item is left over, whatever the order of the inputs.
 * Runs as an ordinary test over the seeds; start the fuzzer with {@code JAZZER_FUZZ=1} (see
 * CONTRIBUTING.md).
 */
class ItemsInAnyOrderFuzzTest {
    private static final int MAX_SIZE = 6;
    private static final int MAX_VALUE = 3;

    @FuzzTest
    void itemsInAnyOrderAgreesWithTheBruteForceOracle(final FuzzedDataProvider data) {
        final List<Integer> items = new ArrayList<>();
        for (var i = data.consumeInt(0, MAX_SIZE); i > 0; i--) {
            items.add(data.consumeInt(0, MAX_VALUE));
        }
        final List<Matcher<Integer>> matchers = new ArrayList<>();
        for (var i = data.consumeInt(0, MAX_SIZE); i > 0; i--) {
            matchers.add(
                    matcherOfKind(
                            data.consumeInt(0, 3),
                            data.consumeInt(0, MAX_VALUE),
                            data.consumeInt(0, MAX_VALUE)));
        }
        final Random random = new Random(data.consumeLong());

        final Verdict verdict = verdictOf(items, matchers);
        final int assigned = largestAssignment(matchers, items, 0, new boolean[items.size()]);
        assertEquals(
                items.size() == matchers.size() && assigned == items.size(),
                verdict.matches(),
                () -> "verdict for " + items + " against " + matchers);
        assertEquals(
                matchers.size() - assigned, verdict.missing(), () -> "missing: " + verdict.text());
        assertEquals(
                items.size() - assigned,
                verdict.unexpected(),
                () -> "unexpected: " + verdict.text());

        Collections.shuffle(items, random);
        Collections.shuffle(matchers, random);
        assertEquals(verdict, verdictOf(items, matchers).withText(verdict.text()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seeds")
    void seedsAgreeWithTheBruteForceOracle(final String name, final List<Object> seed) {
        itemsInAnyOrderAgreesWithTheBruteForceOracle(CannedFuzzedDataProvider.create(seed));
    }

    /**
     * Each seed lists the provider calls in order: items, then matchers as kind, a, b, then the
     * shuffle seed.
     */
    static Stream<Arguments> seeds() {
        return Stream.of(
                Arguments.of("empty lists", List.<Object>of(0, 0, 1L)),
                Arguments.of(
                        "equal sizes, one possible assignment",
                        List.<Object>of(2, 1, 2, 2, 0, 1, 0, 0, 2, 0, 2L)),
                // anything() claims the item 1 first, equalTo(1) must take it back
                Arguments.of(
                        "greedy claiming fails, augmenting path needed",
                        List.<Object>of(2, 1, 2, 2, 1, 0, 0, 0, 1, 0, 3L)),
                Arguments.of("surplus items", List.<Object>of(3, 0, 1, 2, 1, 0, 1, 0, 4L)),
                Arguments.of(
                        "surplus matchers",
                        List.<Object>of(1, 1, 3, 0, 1, 0, 1, 0, 0, 0, 2, 0, 5L)),
                Arguments.of(
                        "no assignment although the sizes are equal",
                        List.<Object>of(2, 0, 1, 2, 0, 0, 0, 0, 0, 0, 6L)));
    }

    private static Matcher<Integer> matcherOfKind(final int kind, final int a, final int b) {
        return switch (kind) {
            case 0 -> Matchers.equalTo(a);
            case 1 -> Matchers.any(Integer.class);
            case 2 ->
                    Matchers.allOf(
                            Matchers.greaterThanOrEqualTo(Math.min(a, b)),
                            Matchers.lessThanOrEqualTo(Math.max(a, b)));
            default -> Matchers.greaterThanOrEqualTo(a);
        };
    }

    /** The size of the largest one-to-one assignment, tried through all permutations. */
    private static int largestAssignment(
            final List<Matcher<Integer>> matchers,
            final List<Integer> items,
            final int matcher,
            final boolean[] taken) {
        if (matcher == matchers.size()) {
            return 0;
        }
        var best = largestAssignment(matchers, items, matcher + 1, taken);
        for (var item = 0; item < items.size(); item++) {
            if (!taken[item] && matchers.get(matcher).matches(items.get(item))) {
                taken[item] = true;
                best = Math.max(best, 1 + largestAssignment(matchers, items, matcher + 1, taken));
                taken[item] = false;
            }
        }
        return best;
    }

    private static Verdict verdictOf(
            final List<Integer> items, final List<Matcher<Integer>> matchers) {
        final Matcher<Object> matcher = itemsInAnyOrder(matchers);
        final boolean matches = matcher.matches(items);
        final StringDescription description = new StringDescription();
        assertDoesNotThrow(() -> matcher.describeMismatch(items, description));
        final String text = description.toString();
        return new Verdict(
                matches, countLines(text, "missing "), countLines(text, "unexpected "), text);
    }

    private static int countLines(final String text, final String prefix) {
        return (int)
                text.lines().map(String::strip).filter(line -> line.startsWith(prefix)).count();
    }

    /** The text only explains a failure; it is not part of the comparison across orders. */
    private record Verdict(boolean matches, int missing, int unexpected, String text) {
        Verdict withText(final String other) {
            return new Verdict(matches, missing, unexpected, other);
        }
    }
}
