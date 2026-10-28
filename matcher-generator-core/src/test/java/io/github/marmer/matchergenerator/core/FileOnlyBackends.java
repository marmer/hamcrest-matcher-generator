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
}
