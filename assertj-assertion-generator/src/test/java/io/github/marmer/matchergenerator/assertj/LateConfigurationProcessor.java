/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * A processor that writes a configuration class in its first round, like any other generator that
 * produces annotated sources. The matcher generator sees the class in the next round.
 */
class LateConfigurationProcessor extends AbstractProcessor {
    private final String qualifiedName;
    private final String configuredType;
    private boolean written;

    /**
     * @param qualifiedName the configuration class to write
     * @param configuredType the type its {@code @GenerateMatchers} names
     */
    LateConfigurationProcessor(final String qualifiedName, final String configuredType) {
        this.qualifiedName = qualifiedName;
        this.configuredType = configuredType;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(
            final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (written || roundEnv.processingOver()) {
            return false;
        }
        written = true;
        final int lastDot = qualifiedName.lastIndexOf('.');
        try (Writer writer =
                processingEnv.getFiler().createSourceFile(qualifiedName).openWriter()) {
            writer.write(
                    "package "
                            + qualifiedName.substring(0, lastDot)
                            + ";\n"
                            + "import io.github.marmer.matchergenerator.annotations.GenerateMatchers;\n"
                            + "@GenerateMatchers(\""
                            + configuredType
                            + "\")\n"
                            + "public final class "
                            + qualifiedName.substring(lastDot + 1)
                            + " {\n}\n");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return false;
    }
}
