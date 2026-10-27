/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.bridge.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The 5.x artifact {@code hamcrest-matcher-generator-dependencies} is a bridge: a jar that depends
 * on the Hamcrest backend, not a relocation POM, because older compiler plugins do not follow a
 * relocation in annotationProcessorPaths.
 */
class BridgeJarIT {

    private static final String PACKAGE_ROOT =
            "io.github.marmer.matchergenerator.bridge.dependencies";

    @Test
    @DisplayName(
            "The POM packages a jar, carries no relocation and depends on hamcrest-matcher-generator of the same version")
    void thePomPackagesAJarAndDependsOnTheHamcrestBackend()
            throws IOException, SAXException, ParserConfigurationException {
        final Document pom =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(Path.of(System.getProperty("module.pom")).toFile());
        final Element project = pom.getDocumentElement();

        assertEquals("hamcrest-matcher-generator-dependencies", childText(project, "artifactId"));
        assertEquals("jar", packaging(project));
        assertEquals(0, pom.getElementsByTagName("relocation").getLength(), "relocation");
        final Element backend = bridgeDependency(pom);
        assertEquals("io.github.marmer.testutils", childText(backend, "groupId"));
        assertEquals("${project.version}", childText(backend, "version"));
        assertFalse(
                backend.getElementsByTagName("scope").getLength() > 0,
                "compile scope, so it is transitive");
    }

    @Test
    @DisplayName(
            "The jar has an Automatic-Module-Name equal to the package root, its package, the license and the NOTICE")
    void theJarHasAModuleNameTheLicenseAndTheNotice() throws IOException {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("module.jar")).toFile())) {
            assertEquals(
                    PACKAGE_ROOT,
                    jar.getManifest().getMainAttributes().getValue("Automatic-Module-Name"));
            assertNotNull(
                    jar.getEntry(PACKAGE_ROOT.replace('.', '/') + "/Bridge.class"), "Bridge class");
            assertNotNull(jar.getEntry("META-INF/LICENSE"), "META-INF/LICENSE");
            assertNotNull(jar.getEntry("META-INF/NOTICE"), "META-INF/NOTICE");
            assertNotNull(jar.getEntry("THIRD-PARTY.txt"), "THIRD-PARTY.txt");
        }
    }

    private static String packaging(final Element project) {
        final NodeList children = project.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child
                    && child.getTagName().equals("packaging")) {
                return child.getTextContent().trim();
            }
        }
        return "jar";
    }

    private static Element bridgeDependency(final Document pom) {
        final NodeList dependencies = pom.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            final Element dependency = (Element) dependencies.item(i);
            if (childText(dependency, "artifactId").equals("hamcrest-matcher-generator")) {
                return dependency;
            }
        }
        throw new AssertionError("No dependency on hamcrest-matcher-generator");
    }

    private static String childText(final Element parent, final String name) {
        final NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && child.getTagName().equals(name)) {
                return child.getTextContent().trim();
            }
        }
        throw new AssertionError("No <" + name + "> in " + parent.getTagName());
    }
}
