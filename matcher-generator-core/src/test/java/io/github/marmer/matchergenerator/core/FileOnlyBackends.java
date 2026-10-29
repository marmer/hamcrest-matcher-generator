/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.util.List;
import java.util.stream.Stream;

/**
 * Additional test backends proving that every backend runs for every type and that a backend may
 * emit Kotlin.
 */
final class FileOnlyBackends {

    private FileOnlyBackends() {}

    /** Generates a second Java file per type. */
    public static class SecondJavaBackend implements GeneratorBackend {
        @Override
        public List<GeneratedSource> generate(
                final TypeModel model, final GenerationContext context) {
            return List.of(
                    StubSources.emptyClass(model, "Second"),
                    StubSources.emptyClass(model, "SecondExtra"));
        }
    }

    /** Generates a Kotlin file per type. */
    public static class KotlinBackend implements GeneratorBackend {
        @Override
        public List<GeneratedSource> generate(
                final TypeModel model, final GenerationContext context) {
            return List.of(
                    new GeneratedSource(
                            model.targetPackage(),
                            model.simpleName() + "Dsl",
                            GeneratedSource.Language.KOTLIN,
                            "package "
                                    + model.targetPackage()
                                    + "\n\nclass "
                                    + model.simpleName()
                                    + "Dsl\n"));
        }
    }

    /**
     * Generates one file per package of the types in {@code some.summarized}, like a backend that
     * writes an overview of all types of a package: the model that sorts first by qualified name
     * among the types of this round writes it.
     */
    public static class PackageSummaryBackend implements GeneratorBackend {
        static final String PACKAGE = "some.summarized";

        @Override
        public List<GeneratedSource> generate(
                final TypeModel model, final GenerationContext context) {
            if (!model.targetPackage().equals(PACKAGE)) {
                return List.of();
            }
            final String first =
                    Stream.concat(
                                    context.generatedTypes().stream(),
                                    Stream.of(model))
                            .filter(other -> other.targetPackage().equals(PACKAGE))
                            .map(TypeModel::qualifiedName)
                            .min(String::compareTo)
                            .orElseThrow();
            if (!first.equals(model.qualifiedName())) {
                return List.of();
            }
            return List.of(
                    new GeneratedSource(
                            PACKAGE,
                            "PackageSummary",
                            GeneratedSource.Language.JAVA,
                            "package "
                                    + PACKAGE
                                    + ";\n\npublic class PackageSummary {\n    // "
                                    + context.generatedTypes().stream()
                                            .filter(other -> other.targetPackage().equals(PACKAGE))
                                            .map(TypeModel::qualifiedName)
                                            .sorted()
                                            .toList()
                                    + "\n}\n"));
        }
    }
}
