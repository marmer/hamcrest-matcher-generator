/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * A processor that writes one type with a {@code getName()} getter in its first round only, like
 * any other generator that produces sources. It is not registered as a service: a test passes it to
 * the compiler next to the matcher generator.
 */
class FirstRoundTypeProcessor extends AbstractProcessor {
    private final String qualifiedName;
    private boolean written;

    /**
     * @param qualifiedName the type to write, with its package
     */
    FirstRoundTypeProcessor(final String qualifiedName) {
        this.qualifiedName = qualifiedName;
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
                            + "public final class "
                            + qualifiedName.substring(lastDot + 1)
                            + " {\n"
                            + "    public String getName() { return \"\"; }\n"
                            + "}\n");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return false;
    }
}
