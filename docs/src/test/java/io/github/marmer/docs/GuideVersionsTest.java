/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The guide names the Hamcrest and AssertJ versions in prose through the attributes of
 * {@code docs/pom.xml}. Those attributes must match the versions the root pom manages, so the
 * guide never names a version the project does not use.
 */
class GuideVersionsTest {

    private static final File DOCS_POM = new File("pom.xml");
    private static final File ROOT_POM = new File("..", "pom.xml");

    @Test
    @DisplayName("The guide's Hamcrest and AssertJ versions match the root dependency management")
    void theGuidesVersionsMatchTheRootDependencyManagement() {
        final Document docs = parseXml(DOCS_POM);
        final Document root = parseXml(ROOT_POM);
        assertEquals(
                managedVersion(root, "hamcrest"),
                docs.select("hamcrest-version").text(),
                "docs/pom.xml hamcrest-version differs from the root pom");
        assertEquals(
                managedVersion(root, "assertj-core"),
                docs.select("assertj-version").text(),
                "docs/pom.xml assertj-version differs from the root pom");
    }

    private static String managedVersion(final Document root, final String artifactId) {
        for (final Element dependency : root.select("dependencyManagement dependency")) {
            if (artifactId.equals(dependency.select("artifactId").text())) {
                return dependency.select("version").text();
            }
        }
        throw new AssertionError(artifactId + " is not in the root dependencyManagement");
    }

    private static Document parseXml(final File file) {
        try {
            return Jsoup.parse(file, "UTF-8", "", Parser.xmlParser());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
