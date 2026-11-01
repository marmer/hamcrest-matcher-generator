/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Builds the {@code SoftAssertionsProvider} mixin of a target package: an interface with a default
 * {@code assertThat} and a default {@code then} per generated type of the package, nested types
 * included. Mixed into a {@code SoftAssertions} or {@code BDDSoftAssertions} subclass, the
 * generated assertions become soft and BDD assertions. AssertJ creates the soft proxies.
 */
final class SoftAssertionsProviderGenerator {
    static final String NAME = "SoftAssertionsProvider";
    // AssertJ's interface has the same simple name as the generated mixin, which extends it.
    private static final ClassName SOFT_ASSERTIONS_PROVIDER =
            ClassName.get("org.assertj.core.api", NAME);
    private static final String ASSERT_THAT = "assertThat";
    private static final String THEN = "then";

    private final List<TypeModel> models;
    private final GenerationContext context;

    /**
     * @param models the top level types of the package, sorted by name, the first one writes the
     *     mixin
     * @param context information about the current generation run
     */
    SoftAssertionsProviderGenerator(final List<TypeModel> models, final GenerationContext context) {
        this.models = models;
        this.context = context;
    }

    TypeSpec typeSpec() {
        final AssertionGenerator first = new AssertionGenerator(models.get(0), context);
        final TypeSpec.Builder builder =
                TypeSpec.interfaceBuilder(NAME)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(first.generatedAnnotation())
                        .addSuperinterface(SOFT_ASSERTIONS_PROVIDER);
        models.forEach(
                model -> {
                    final AssertionGenerator generator = new AssertionGenerator(model, context);
                    addMethods(
                            builder,
                            model,
                            ClassName.get(model.targetPackage(), generator.concreteName()));
                });
        return first.topLevel(builder);
    }

    private void addMethods(
            final TypeSpec.Builder builder, final TypeModel model, final ClassName assertClass) {
        final AssertionGenerator generator = new AssertionGenerator(model, context);
        builder.addMethod(generator.softAssertionMethod(ASSERT_THAT, assertClass))
                .addMethod(generator.softAssertionMethod(THEN, assertClass));
        model.nestedTypes()
                .forEach(
                        nested ->
                                addMethods(
                                        builder,
                                        nested,
                                        assertClass.nestedClass(
                                                new AssertionGenerator(nested, context)
                                                        .concreteName())));
    }
}
