/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static io.github.marmer.matchergenerator.hamcrest.GoldenMessages.messageOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.Node;
import io.github.marmer.matchergenerator.hamcrest.BeanFixtures.TestMatcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;

/**
 * The runtime side of {@code isXLike(reference)}: presets from a reference object, deep and safe
 * for cyclic graphs. {@link #isNodeLike} is written like the generated code.
 */
class ReferenceMatchingTest {
    @SuppressWarnings("unchecked")
    private static final Class<TestMatcher<Node>> NODE_MATCHER =
            (Class<TestMatcher<Node>>) (Class<?>) TestMatcher.class;

    private final GoldenMessages golden = new GoldenMessages("ReferenceMatchingTest");

    private static TestMatcher<Node> isNodeLike(final Node reference) {
        return ReferenceMatching.matcherFor(
                reference,
                NODE_MATCHER,
                BeanFixtures::isNode,
                matcher -> {
                    final BeanPropertyMatcher<Node> bean = matcher.beanPropertyMatcher();
                    bean.checkingAllProperties();
                    bean.preset("name", ReferenceMatching.value(reference.getName()));
                    bean.preset(
                            "parent",
                            ReferenceMatching.nested(
                                    reference.getParent(), ReferenceMatchingTest::isNodeLike));
                    bean.preset(
                            "children",
                            ReferenceMatching.list(
                                    reference.getChildren(), ReferenceMatchingTest::isNodeLike));
                    bean.preset(
                            "byName",
                            ReferenceMatching.map(
                                    reference.getByName(), ReferenceMatchingTest::isNodeLike));
                    bean.preset(
                            "peers",
                            ReferenceMatching.collection(
                                    reference.getPeers(), ReferenceMatchingTest::isNodeLike));
                    bean.preset("tags", ReferenceMatching.value(reference.getTags()));
                    bean.preset(
                            "siblings",
                            ReferenceMatching.array(
                                    reference.getSiblings(), ReferenceMatchingTest::isNodeLike));
                });
    }

    private static Node tree(final String rootName, final String childName) {
        return new Node(rootName).withChild(new Node(childName));
    }

    private static String descriptionOf(final TestMatcher<Node> matcher) {
        final Description description = new StringDescription();
        matcher.describeTo(description);
        return description.toString();
    }

    @Test
    void anEqualCyclicGraphPasses() {
        assertTrue(isNodeLike(tree("root", "leaf")).matches(tree("root", "leaf")));
    }

    @Test
    void aNodeBeingItsOwnParentPasses() {
        final Node self = new Node("self");
        self.withParent(self);
        final Node other = new Node("self");
        other.withParent(other);

        assertTrue(isNodeLike(self).matches(other));
    }

    @Test
    void aDifferenceBehindTheBackReferenceIsFoundWithoutRunningInCircles() throws IOException {
        final Node actual = tree("root", "leaf");
        actual.getChildren().get(0).withParent(new Node("imposter"));

        golden.assertMessage(
                "difference-behind-back-reference",
                messageOf(actual, isNodeLike(tree("root", "leaf"))));
    }

    @Test
    void aNestedDifferenceIsReportedWithItsFullPath() throws IOException {
        final Node reference = new Node("root").withChild(new Node("a").withChild(new Node("b")));
        final Node actual = new Node("root").withChild(new Node("a").withChild(new Node("c")));

        golden.assertMessage("nested-difference", messageOf(actual, isNodeLike(reference)));
    }

    @Test
    void aNullPropertyOfTheReferenceMustBeNullInTheActualValue() throws IOException {
        final Node actual = new Node("leaf").withParent(new Node("root"));

        golden.assertMessage(
                "null-reference-property", messageOf(actual, isNodeLike(new Node("leaf"))));
    }

    @Test
    void collectionsMapsAndArraysAreComparedByTheirItems() throws IOException {
        final Node reference =
                new Node("root")
                        .withPeer(new Node("p"))
                        .withSiblings(new Node("s"))
                        .withNamed("k", new Node("c"))
                        .withTag("t");
        final Node actual =
                new Node("root")
                        .withPeer(new Node("q"))
                        .withSiblings(new Node("z"))
                        .withNamed("k", new Node("d"))
                        .withTag("t");

        golden.assertMessage("collections-maps-arrays", messageOf(actual, isNodeLike(reference)));
    }

    @Test
    void aPresetIsReplacedByTheNextCheckOfTheProperty() {
        final TestMatcher<Node> matcher = isNodeLike(new Node("expected"));

        matcher.with("name", equalTo("other"));

        assertTrue(matcher.matches(new Node("other")));
        assertFalse(matcher.matches(new Node("expected")));
    }

    @Test
    void aPresetIsDroppedByReset() {
        final TestMatcher<Node> matcher = isNodeLike(new Node("expected"));

        matcher.beanPropertyMatcher().reset("name");

        assertFalse(matcher.matches(new Node("expected")), "name is neither checked nor ignored");
        matcher.ignoring("name");
        assertTrue(matcher.matches(new Node("other")));
    }

    @Test
    void aPresetIsDroppedByIgnoring() {
        final TestMatcher<Node> matcher = isNodeLike(new Node("expected"));

        matcher.ignoring("name");

        assertTrue(matcher.matches(new Node("other")));
    }

    @Test
    void aReferenceObjectMetTwiceWhileBuildingGetsOneMatcher() {
        final Node reference = new Node("x");
        final List<TestMatcher<Node>> built = new ArrayList<>();

        ReferenceMatching.matcherFor(
                new Node("outer"),
                NODE_MATCHER,
                BeanFixtures::isNode,
                outer -> {
                    built.add(
                            ReferenceMatching.matcherFor(
                                    reference, NODE_MATCHER, BeanFixtures::isNode, inner -> {}));
                    built.add(
                            ReferenceMatching.matcherFor(
                                    reference, NODE_MATCHER, BeanFixtures::isNode, inner -> {}));
                });

        assertSame(built.get(0), built.get(1));
    }

    @Test
    void separateBuildsGetSeparateMatchers() {
        final Node reference = new Node("x");

        assertNotSame(isNodeLike(reference), isNodeLike(reference));
    }

    @Test
    void aCyclicMatcherCanBeDescribed() {
        assertDoesNotThrow(() -> descriptionOf(isNodeLike(tree("root", "leaf"))));
    }

    @Test
    void theBuildStateIsDroppedWhenBuildingFails() {
        final Node reference = new Node("root");
        assertThrows(
                IllegalStateException.class,
                () ->
                        ReferenceMatching.matcherFor(
                                reference,
                                NODE_MATCHER,
                                BeanFixtures::isNode,
                                matcher -> {
                                    throw new IllegalStateException("broken");
                                }));

        assertTrue(isNodeLike(reference).matches(new Node("root")));
    }
}
