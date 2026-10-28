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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates AssertJ assertions: per configured type an {@code AbstractXAssert} with the generated
 * assertions and a concrete {@code XAssert} to use. Per target package one {@code
 * SoftAssertionsProvider} mixin: the model that sorts first by qualified name among the types of
 * that package in this round writes it, so it is written once. Types of another round or
 * compilation are not in the mixin.
 */
public class AssertjGeneratorBackend implements GeneratorBackend {

    @Override
    public List<GeneratedSource> generate(final TypeModel model, final GenerationContext context) {
        final AssertionGenerator generator = new AssertionGenerator(model, context);
        final List<GeneratedSource> sources =
                new ArrayList<>(
                        List.of(
                                sourceOf(
                                        model,
                                        generator.abstractName(),
                                        generator.abstractTypeSpec()),
                                sourceOf(
                                        model,
                                        generator.concreteName(),
                                        generator.concreteTypeSpec())));
        // Only the first model of the package writes the mixin and the representation. The list
        // holds the types generated in this round, so a type from another round or compilation is
        // in neither.
        final List<TypeModel> packageModels = packageModels(model, context);
        if (packageModels.get(0).qualifiedName().equals(model.qualifiedName())) {
            sources.add(
                    sourceOf(
                            model,
                            SoftAssertionsProviderGenerator.NAME,
                            new SoftAssertionsProviderGenerator(packageModels, context)
                                    .typeSpec()));
            sources.add(
                    sourceOf(
                            model,
                            RepresentationGenerator.NAME,
                            new RepresentationGenerator(packageModels, context).typeSpec()));
        }
        return sources;
    }

    /**
     * The types of the model's target package generated in this round, the model itself included,
     * one per qualified name and sorted by it. The first one writes the mixin and the
     * representation.
     */
    private static List<TypeModel> packageModels(
            final TypeModel model, final GenerationContext context) {
        final Map<String, TypeModel> byName =
                Stream.concat(context.generatedTypes().stream(), Stream.of(model))
                        .filter(other -> other.targetPackage().equals(model.targetPackage()))
                        .collect(
                                Collectors.toMap(
                                        TypeModel::qualifiedName,
                                        other -> other,
                                        (first, second) -> first,
                                        TreeMap::new));
        return List.copyOf(byName.values());
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
