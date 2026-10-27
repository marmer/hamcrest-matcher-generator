/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.dsl.StructurizrDslParserException;
import com.structurizr.view.View;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps the architecture documentation and the Structurizr workspace in step: every view of the
 * workspace is embedded in the arc42 chapters, every embedded diagram comes from a view, and every
 * embedded diagram was rendered.
 */
class WorkspaceViewsTest {

    private static final Path SOURCES =
            Paths.get(System.getProperty("docs.source.directory", "src/docs/asciidoc"));
    private static final Path OUTPUT =
            Paths.get(System.getProperty("docs.output.directory", "target/generated-docs"));
    private static final Path WORKSPACE = SOURCES.resolve("architecture/workspace.dsl");
    private static final Path CHAPTERS = SOURCES.resolve("architecture/_chapters");
    private static final Pattern STRUCTURIZR_BLOCK =
            Pattern.compile("^\\[structurizr\\b([^\\]]*)]", Pattern.MULTILINE);
    private static final Pattern VIEW_ATTRIBUTE = Pattern.compile("\\bview=\"?([^,\"\\]]+)\"?");
    private static final Pattern TARGET_ATTRIBUTE = Pattern.compile("\\btarget=\"?([^,\"\\]]+)\"?");

    @Test
    @DisplayName("Every workspace view is embedded and every embedded diagram comes from a view")
    void everyWorkspaceViewIsEmbeddedAndEveryEmbeddedDiagramComesFromAView() {
        final List<String> problems = viewProblems(viewKeysOfWorkspace(), embeddedViewKeys());
        assertTrue(
                problems.isEmpty(),
                "workspace and documentation differ:\n" + String.join("\n", problems));
    }

    @Test
    @DisplayName("The view check detects views that are not embedded and diagrams without a view")
    void viewCheckDetectsDifferences() {
        assertTrue(viewProblems(Set.of("A", "B"), Set.of("A", "B")).isEmpty(), "equal sets");
        assertEquals(1, viewProblems(Set.of("A", "B"), Set.of("A")).size(), "view not embedded");
        assertEquals(1, viewProblems(Set.of("A"), Set.of("A", "C")).size(), "diagram without view");
    }

    @Test
    @DisplayName(
            "Every embedded view is rendered to a non-empty SVG with its own opaque background")
    void everyEmbeddedViewIsRenderedToSvgWithOpaqueBackground() throws IOException {
        final List<String> problems = new ArrayList<>();
        final Set<String> targets = embeddedAttributes(TARGET_ATTRIBUTE, "target");
        assertFalse(targets.isEmpty(), "no embedded diagrams found");
        for (final String target : targets) {
            final Path svg = OUTPUT.resolve("architecture/images").resolve(target + ".svg");
            if (!Files.isRegularFile(svg) || Files.size(svg) == 0) {
                problems.add(target + ".svg is missing or empty");
                continue;
            }
            // The site has a dark mode; the diagram must not depend on the page background to be
            // readable.
            final Document document = Jsoup.parse(Files.readString(svg, StandardCharsets.UTF_8));
            final Element svgRoot = document.selectFirst("svg");
            final String style = svgRoot == null ? "" : svgRoot.attr("style").toLowerCase();
            if (!style.contains("background:#ffffff")) {
                problems.add(target + ".svg has no opaque white background: '" + style + "'");
            }
        }
        assertTrue(
                problems.isEmpty(),
                "problems with rendered diagrams:\n" + String.join("\n", problems));
    }

    private static List<String> viewProblems(
            final Set<String> workspaceViews, final Set<String> embeddedViews) {
        final List<String> problems = new ArrayList<>();
        workspaceViews.stream()
                .filter(view -> !embeddedViews.contains(view))
                .forEach(
                        view -> problems.add("view '" + view + "' is not embedded in any chapter"));
        embeddedViews.stream()
                .filter(view -> !workspaceViews.contains(view))
                .forEach(
                        view ->
                                problems.add(
                                        "an embedded diagram uses the unknown view '"
                                                + view
                                                + "'"));
        return problems;
    }

    private static Set<String> viewKeysOfWorkspace() {
        try {
            final StructurizrDslParser parser = new StructurizrDslParser();
            parser.parse(WORKSPACE.toFile());
            return parser.getWorkspace().getViews().getViews().stream()
                    .map(View::getKey)
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (final StructurizrDslParserException e) {
            throw new IllegalStateException("workspace.dsl is invalid", e);
        }
    }

    private static Set<String> embeddedViewKeys() {
        return embeddedAttributes(VIEW_ATTRIBUTE, "view");
    }

    private static Set<String> embeddedAttributes(final Pattern pattern, final String name) {
        final Set<String> values = new TreeSet<>();
        for (final String attributes : embeddedBlocks()) {
            final Matcher matcher = pattern.matcher(attributes);
            if (!matcher.find()) {
                throw new IllegalStateException(
                        "structurizr block without " + name + ": [structurizr" + attributes + "]");
            }
            values.add(matcher.group(1).trim());
        }
        return values;
    }

    private static List<String> embeddedBlocks() {
        try (Stream<Path> files = Files.walk(CHAPTERS)) {
            final List<String> blocks = new ArrayList<>();
            for (final Path chapter :
                    files.filter(file -> file.toString().endsWith(".adoc"))
                            .sorted()
                            .collect(Collectors.toList())) {
                final Matcher matcher =
                        STRUCTURIZR_BLOCK.matcher(
                                Files.readString(chapter, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    blocks.add(matcher.group(1));
                }
            }
            return blocks;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
