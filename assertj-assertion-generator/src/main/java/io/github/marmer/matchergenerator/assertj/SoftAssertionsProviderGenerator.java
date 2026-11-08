/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
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
    private static final ClassName SOFT_ASSERTIONS_PROVIDER =
            ClassName.get("org.assertj.core.api", AssertionNaming.SOFT_ASSERTIONS_PROVIDER);
    private static final String ACTUAL = "actual";
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
        final TypeSpec.Builder builder =
                TypeSpec.interfaceBuilder(AssertionNaming.SOFT_ASSERTIONS_PROVIDER)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(FileShell.generatedAnnotation(context))
                        .addSuperinterface(SOFT_ASSERTIONS_PROVIDER);
        models.forEach(
                model ->
                        addMethods(
                                builder,
                                model,
                                ClassName.get(
                                        model.targetPackage(),
                                        AssertionNaming.concreteName(model.simpleName()))));
        return FileShell.topLevel(builder, context);
    }

    private void addMethods(
            final TypeSpec.Builder builder, final TypeModel model, final ClassName assertClass) {
        builder.addMethod(softAssertionMethod(ASSERT_THAT, model, assertClass))
                .addMethod(softAssertionMethod(THEN, model, assertClass));
        model.nestedTypes()
                .forEach(
                        nested ->
                                addMethods(
                                        builder,
                                        nested,
                                        assertClass.nestedClass(
                                                AssertionNaming.concreteName(
                                                        nested.simpleName()))));
    }

    /**
     * A default method of the soft assertions mixin, such as {@code assertThat(Order)}: a soft
     * proxy of the concrete assertion class for the value.
     */
    private MethodSpec softAssertionMethod(
            final String name, final TypeModel model, final ClassName concreteClass) {
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(
                        FileShell.nullable(AssertionGenerator.assertedType(model), context),
                        ACTUAL,
                        Modifier.FINAL)
                .addStatement(
                        "return proxy($T.class, $T.class, $L)",
                        concreteClass,
                        AssertionGenerator.rawClass(model),
                        ACTUAL)
                .returns(concreteClass)
                .build();
    }
}
