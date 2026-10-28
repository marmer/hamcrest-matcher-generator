/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest.kotlin;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.util.List;

/**
 * Generates the Kotlin DSL on top of the generated Hamcrest matchers, one {@code XMatcherDsl.kt}
 * per configured type. Kotlin sources are compiled only when the annotation processor runs with
 * KAPT, so under plain javac this backend generates nothing and says so once.
 */
public class KotlinDslGeneratorBackend implements GeneratorBackend {

    static final String NOT_UNDER_KAPT_NOTE =
            "hamcrest-matcher-generator-kotlin generates the Kotlin DSL only when the annotation"
                    + " processor runs with KAPT; nothing is generated for this compilation. The"
                    + " Java matchers are not affected.";

    private boolean notedMissingKapt;

    @Override
    public List<GeneratedSource> generate(final TypeModel model, final GenerationContext context) {
        if (!context.kotlinOutputAvailable()) {
            noteMissingKaptOnce(context);
            return List.of();
        }
        if (!KotlinDslGenerator.hasAnythingToGenerate(model)) {
            return List.of();
        }
        return List.of(
                new GeneratedSource(
                        model.targetPackage(),
                        KotlinDslGenerator.fileName(model),
                        GeneratedSource.Language.KOTLIN,
                        new KotlinDslGenerator(model, context).generate()));
    }

    private void noteMissingKaptOnce(final GenerationContext context) {
        if (!notedMissingKapt) {
            notedMissingKapt = true;
            context.reportNote(NOT_UNDER_KAPT_NOTE);
        }
    }
}
