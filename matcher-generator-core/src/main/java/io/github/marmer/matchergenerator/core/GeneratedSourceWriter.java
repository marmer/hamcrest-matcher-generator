/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Writes the sources the backends generate and owns the {@code Filer}. Java sources go through the
 * Filer; Kotlin sources go into the directory KAPT compiles. A source an earlier round wrote is not
 * written again, since the Filer refuses a second source of a name.
 */
final class GeneratedSourceWriter {
    /** The processor option KAPT passes with the directory Kotlin sources have to be written to. */
    static final String KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated";

    private final ProcessingEnvironment processingEnv;
    private final ProcessorDiagnostics diagnostics;

    /**
     * The names of the sources written so far, per round. The Filer refuses a second source of the
     * same name, which for a file a backend writes once per package happens when a later round adds
     * a type to that package.
     */
    private final Set<String> writtenInEarlierRounds = new HashSet<>();

    private final Set<String> writtenInThisRound = new HashSet<>();

    GeneratedSourceWriter(
            final ProcessingEnvironment processingEnv, final ProcessorDiagnostics diagnostics) {
        this.processingEnv = processingEnv;
        this.diagnostics = diagnostics;
    }

    /**
     * Starts a round. The sources the previous round wrote count as written in an earlier round.
     */
    void startRound() {
        writtenInEarlierRounds.addAll(writtenInThisRound);
        writtenInThisRound.clear();
    }

    /**
     * Writes one source of a backend for a type. The configuration names the element and annotation
     * a diagnostic points at; the round models are the types this round generates.
     */
    void write(
            final GeneratorBackend backend,
            final GeneratedSource source,
            final TypeElement typeElement,
            final Element configurationElement,
            final AnnotationMirror configurationMirror,
            final List<TypeModel> roundModels) {
        final String name = source.qualifiedName();
        if (writtenInEarlierRounds.contains(name)) {
            printNotWrittenAgain(
                    configurationElement,
                    configurationMirror,
                    name,
                    source.packageName(),
                    roundModels);
            return;
        }
        writtenInThisRound.add(name);
        final Element[] originatingElements = {typeElement, configurationElement};
        try {
            if (source.language() == GeneratedSource.Language.JAVA) {
                writeJava(name, source, originatingElements);
            } else {
                writeKotlin(backend, source, typeElement);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Whether a class of that name is on the compile class path or source path, without being part
     * of the output directory of this compilation. What a previous, non clean build of this module
     * left in its own output is not counted, since it is replaced by this build.
     */
    boolean classExistsOutsideOwnOutput(final String qualifiedClassName) {
        if (processingEnv.getElementUtils().getTypeElement(qualifiedClassName) == null) {
            return false;
        }
        try (InputStream ignored =
                processingEnv
                        .getFiler()
                        .getResource(
                                StandardLocation.CLASS_OUTPUT,
                                "",
                                qualifiedClassName.replace('.', '/') + ".class")
                        .openInputStream()) {
            return false;
        } catch (final IOException e) {
            return true;
        }
    }

    /**
     * A source an earlier round wrote is not written again, which the Filer would refuse. Whatever
     * this round adds to the package is not part of it, which the user has to know.
     */
    private void printNotWrittenAgain(
            final Element configurationElement,
            final AnnotationMirror configurationMirror,
            final String name,
            final String packageName,
            final List<TypeModel> roundModels) {
        final String missing =
                roundModels.stream()
                        .filter(model -> model.targetPackage().equals(packageName))
                        .map(TypeModel::qualifiedName)
                        .sorted()
                        .collect(Collectors.joining(", "));
        diagnostics.sourceNotWrittenAgain(configurationElement, configurationMirror, name, missing);
    }

    private void writeJava(
            final String name, final GeneratedSource source, final Element[] originatingElements)
            throws IOException {
        final FileObject file =
                processingEnv.getFiler().createSourceFile(name, originatingElements);
        try (Writer writer = file.openWriter()) {
            writer.write(source.content());
        }
    }

    private void writeKotlin(
            final GeneratorBackend backend,
            final GeneratedSource source,
            final TypeElement typeElement)
            throws IOException {
        final String outputDirectory = processingEnv.getOptions().get(KAPT_KOTLIN_GENERATED);
        if (outputDirectory == null) {
            diagnostics.kotlinWithoutKaptOption(backend, typeElement);
            return;
        }
        final Path directory = Path.of(outputDirectory, source.packageName().split("\\."));
        Files.createDirectories(directory);
        Files.writeString(
                directory.resolve(source.simpleName() + "." + source.language().fileExtension()),
                source.content());
    }
}
