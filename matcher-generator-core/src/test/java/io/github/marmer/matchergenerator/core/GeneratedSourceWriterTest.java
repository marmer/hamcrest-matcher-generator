/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The writer without a javac round: the Filer and the Messager are fakes. They do not look at the
 * elements, so the elements are stubs that fail on any call.
 */
class GeneratedSourceWriterTest {

    private static final GeneratorBackend BACKEND = (model, context) -> List.of();
    private static final TypeElement TYPE_ELEMENT = unused(TypeElement.class);
    private static final Element CONFIGURATION_ELEMENT = unused(Element.class);
    private static final AnnotationMirror CONFIGURATION_MIRROR = unused(AnnotationMirror.class);

    @TempDir Path kaptOutput;

    @Test
    @DisplayName("A Java source is written through the Filer with its content")
    void aJavaSourceIsWrittenThroughTheFilerWithItsContent() {
        final FakeProcessingEnvironment environment = new FakeProcessingEnvironment(Map.of());
        final GeneratedSourceWriter writer = writerFor(environment);

        writeSource(writer, javaSource("a.b", "Foo", "first"));

        assertEquals(List.of("a.b.Foo"), environment.filer().createdSourceNames());
        assertEquals("first", environment.filer().contentOf("a.b.Foo"));
        assertEquals(List.of(), environment.messager().diagnostics());
    }

    @Test
    @DisplayName("A source an earlier round wrote is not written again, and a warning says so")
    void aSourceAnEarlierRoundWroteIsNotWrittenAgainAndAWarningSaysSo() {
        final FakeProcessingEnvironment environment = new FakeProcessingEnvironment(Map.of());
        final GeneratedSourceWriter writer = writerFor(environment);
        writeSource(writer, javaSource("a.b", "Foo", "first"));

        writer.startRound();
        writeSource(writer, javaSource("a.b", "Foo", "second"));

        assertEquals(List.of("a.b.Foo"), environment.filer().createdSourceNames());
        assertEquals("first", environment.filer().contentOf("a.b.Foo"));
        assertEquals(1, environment.messager().diagnostics().size());
        final String note = environment.messager().diagnostics().get(0);
        assertTrue(
                note.startsWith(
                        "WARNING a.b.Foo was already written in an earlier processing round"));
        assertTrue(note.contains("is not written again"));
    }

    @Test
    @DisplayName(
            "A Kotlin source is written below the KAPT output directory, not through the Filer")
    void aKotlinSourceIsWrittenBelowTheKaptOutputDirectoryNotThroughTheFiler() throws Exception {
        final FakeProcessingEnvironment environment =
                new FakeProcessingEnvironment(
                        Map.of(GeneratedSourceWriter.KAPT_KOTLIN_GENERATED, kaptOutput.toString()));
        final GeneratedSourceWriter writer = writerFor(environment);

        writeSource(writer, kotlinSource("a.b", "Foo", "kotlin"));

        assertEquals(List.of(), environment.filer().createdSourceNames());
        assertEquals(
                "kotlin",
                Files.readString(kaptOutput.resolve("a/b/Foo.kt"), StandardCharsets.UTF_8));
        assertEquals(List.of(), environment.messager().diagnostics());
    }

    private static GeneratedSourceWriter writerFor(final FakeProcessingEnvironment environment) {
        return new GeneratedSourceWriter(environment, new ProcessorDiagnostics(environment));
    }

    private static void writeSource(
            final GeneratedSourceWriter writer, final GeneratedSource source) {
        writer.write(
                BACKEND,
                source,
                TYPE_ELEMENT,
                CONFIGURATION_ELEMENT,
                CONFIGURATION_MIRROR,
                List.of());
    }

    private static GeneratedSource javaSource(
            final String packageName, final String simpleName, final String content) {
        return new GeneratedSource(packageName, simpleName, GeneratedSource.Language.JAVA, content);
    }

    private static GeneratedSource kotlinSource(
            final String packageName, final String simpleName, final String content) {
        return new GeneratedSource(
                packageName, simpleName, GeneratedSource.Language.KOTLIN, content);
    }

    /**
     * An element the fake never looks at. Any call on it throws, so a test that needs it finds out.
     */
    private static <T> T unused(final Class<T> type) {
        return type.cast(
                Proxy.newProxyInstance(
                        type.getClassLoader(),
                        new Class<?>[] {type},
                        (proxy, method, args) -> {
                            throw new UnsupportedOperationException(method.getName());
                        }));
    }
}
