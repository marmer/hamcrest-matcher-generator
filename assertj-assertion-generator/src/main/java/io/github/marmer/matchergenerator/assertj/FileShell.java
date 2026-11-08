/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import javax.annotation.processing.Generated;

/**
 * The shell every generated AssertJ type shares: the {@code @Generated} annotation, the null
 * marking of a top level type and the {@code @Nullable} of a type use. Nothing here depends on the
 * type the shell wraps, so every generator of the backend uses the same one.
 */
final class FileShell {
    private static final String VALUE = "value";
    private static final AnnotationSpec NULL_MARKED =
            AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "NullMarked")).build();
    private static final AnnotationSpec NULLABLE =
            AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "Nullable")).build();

    private FileShell() {}

    /** The {@code @Generated} annotation naming the generator and the time of the generation. */
    static AnnotationSpec generatedAnnotation(final GenerationContext context) {
        return AnnotationSpec.builder(Generated.class)
                .addMember(VALUE, "$S", context.generatorName())
                .addMember("date", "$S", context.generationTime())
                .build();
    }

    /** The builder's type as a top level type, null marked when JSpecify is available. */
    static TypeSpec topLevel(final TypeSpec.Builder builder, final GenerationContext context) {
        if (context.jspecifyAvailable()) {
            builder.addAnnotation(NULL_MARKED);
        }
        return builder.build();
    }

    /** Marks the type as nullable when JSpecify is available. Primitives cannot be annotated. */
    static TypeName nullable(final TypeName type, final GenerationContext context) {
        return context.jspecifyAvailable() && !type.isPrimitive() ? type.annotated(NULLABLE) : type;
    }
}
