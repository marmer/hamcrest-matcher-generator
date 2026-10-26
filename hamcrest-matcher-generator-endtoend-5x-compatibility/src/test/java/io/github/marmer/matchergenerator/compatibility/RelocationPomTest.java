package io.github.marmer.matchergenerator.compatibility;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A reactor build does not follow relocations, so this checks what a resolver of a repository follows: the 5.x
 * artifacts of the old coordinates are POMs that relocate to the Hamcrest backend, which carries the runtime library
 * and, transitively, the configuration annotations.
 */
class RelocationPomTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "hamcrest-matcher-generator-dependencies",
        "hamcrest-matcher-generator-annotationprocessor"
    })
    void anOldArtifactIsARelocationPomToTheHamcrestBackend(final String oldArtifact)
        throws IOException, SAXException, ParserConfigurationException {
        final Document pom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(Path.of("..", oldArtifact, "pom.xml").toFile());
        final Element project = pom.getDocumentElement();

        assertEquals(oldArtifact, childText(project, "artifactId"));
        assertEquals("pom", childText(project, "packaging"));
        final Element relocation = (Element) pom.getElementsByTagName("relocation").item(0);
        assertEquals("io.github.marmer.testutils", childText(relocation, "groupId"));
        assertEquals("hamcrest-matcher-generator", childText(relocation, "artifactId"));
        assertEquals("${project.version}", childText(relocation, "version"));
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
