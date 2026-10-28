/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Builds the {@code PropertyRepresentation} of a target package: an AssertJ {@code
 * StandardRepresentation} that prints an instance of a configured type with properties by its
 * properties, such as {@code Line[sku="A-1", quantity=2]}, and formats everything else as AssertJ
 * does. It overrides the fallback, so the elements of a collection are printed by it too. The user
 * registers it with {@code Assertions.useRepresentation}; it is never registered automatically.
 */
final class RepresentationGenerator {
    static final String NAME = "PropertyRepresentation";
    private static final ClassName STANDARD_REPRESENTATION =
            ClassName.get("org.assertj.core.presentation", "StandardRepresentation");
    private static final String FALLBACK_TO_STRING_OF = "fallbackToStringOf";
    private static final String OBJECT = "object";
    private static final String ACTUAL = "actual";

    private final List<TypeModel> models;
    private final GenerationContext context;

    /**
     * @param models the top level types of the package, sorted by name, the first one writes the
     *     representation
     * @param context information about the current generation run
     */
    RepresentationGenerator(final List<TypeModel> models, final GenerationContext context) {
        this.models = models;
        this.context = context;
    }

    TypeSpec typeSpec() {
        final AssertionGenerator first = new AssertionGenerator(models.get(0), context);
        final MethodSpec.Builder fallback =
                MethodSpec.methodBuilder(FALLBACK_TO_STRING_OF)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PROTECTED)
                        .addParameter(first.nullable(TypeName.OBJECT), OBJECT, Modifier.FINAL)
                        .returns(String.class);
        models.forEach(model -> addBranches(fallback, model));
        fallback.addStatement("return super.$L($L)", FALLBACK_TO_STRING_OF, OBJECT);
        return first.topLevel(
                TypeSpec.classBuilder(NAME)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(first.generatedAnnotation())
                        .superclass(STANDARD_REPRESENTATION)
                        .addMethod(fallback.build()));
    }

    /** One branch per type with properties, nested types included; a type without has none. */
    private void addBranches(final MethodSpec.Builder fallback, final TypeModel model) {
        final AssertionGenerator generator = new AssertionGenerator(model, context);
        if (!generator.properties().isEmpty()) {
            fallback.beginControlFlow(
                            "if ($L instanceof $T $L)", OBJECT, generator.assertedType(), ACTUAL)
                    .addStatement("return $L", expression(model, generator.properties()))
                    .endControlFlow();
        }
        model.nestedTypes().forEach(nested -> addBranches(fallback, nested));
    }

    /**
     * {@code "Line[sku=" + toStringOf(actual.getSku()) + ", quantity=" + ... + "]"}, each value
     * formatted by the representation itself.
     */
    private static CodeBlock expression(
            final TypeModel model, final List<PropertyModel> properties) {
        final CodeBlock.Builder expression = CodeBlock.builder();
        String literal = model.simpleName() + "[";
        for (final PropertyModel property : properties) {
            expression.add(
                    "$S + toStringOf($L.$L()) + ",
                    literal + property.name() + "=",
                    ACTUAL,
                    property.accessor());
            literal = ", ";
        }
        return expression.add("$S", "]").build();
    }
}
