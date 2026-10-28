/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.util.List;

/**
 * Generates AssertJ assertions: per configured type an {@code AbstractXAssert} with the generated
 * assertions and a concrete {@code XAssert} to use.
 */
public class AssertjGeneratorBackend implements GeneratorBackend {

    @Override
    public List<GeneratedSource> generate(final TypeModel model, final GenerationContext context) {
        final AssertionGenerator generator = new AssertionGenerator(model, context);
        return List.of(
                sourceOf(model, generator.abstractName(), generator.abstractTypeSpec()),
                sourceOf(model, generator.concreteName(), generator.concreteTypeSpec()));
    }

    private static GeneratedSource sourceOf(
            final TypeModel model, final String simpleName, final TypeSpec typeSpec) {
        return new GeneratedSource(
                model.targetPackage(),
                simpleName,
                GeneratedSource.Language.JAVA,
                withoutSpaceAfterQualifier(
                        JavaFile.builder(model.targetPackage(), typeSpec).build().toString()));
    }

    /**
     * JavaPoet writes a type use annotation on a nested type as {@code Map. @Nullable Entry}. Both
     * spellings are the same Java, the one without the space is how the annotation is usually
     * written.
     */
    private static String withoutSpaceAfterQualifier(final String source) {
        return source.replaceAll("(\\w)\\. (@Nullable )", "$1.$2");
    }
}
