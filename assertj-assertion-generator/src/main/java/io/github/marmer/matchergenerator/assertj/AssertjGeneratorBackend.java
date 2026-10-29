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
 * compilation are not in the mixin: the core does not write the mixin again in a later round and
 * warns, and a mixin of another compilation on the class path is reported as a note.
 */
public class AssertjGeneratorBackend implements GeneratorBackend {

    @Override
    public List<GeneratedSource> generate(final TypeModel model, final GenerationContext context) {
        // Two claimants of one class name make the compiler refuse the second class. Only the first
        // model of the package reports, and nothing of the package is written.
        final List<TypeModel> packageModels = packageModels(model, context);
        final List<String> clashes =
                new GeneratedNameClashes(packageModels).messages(model.targetPackage());
        if (!clashes.isEmpty()) {
            if (packageModels.get(0).qualifiedName().equals(model.qualifiedName())) {
                clashes.forEach(context::reportError);
            }
            return List.of();
        }
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
        if (packageModels.get(0).qualifiedName().equals(model.qualifiedName())) {
            final GeneratedSource mixin =
                    sourceOf(
                            model,
                            SoftAssertionsProviderGenerator.NAME,
                            new SoftAssertionsProviderGenerator(packageModels, context).typeSpec());
            noteMixinOnClassPath(mixin, packageModels, context);
            sources.add(mixin);
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

    /**
     * A mixin on the class path was written by another compilation, e.g. the main one when this is
     * the test compilation. The one written now shadows it and does not know its types, which the
     * user is told.
     */
    private static void noteMixinOnClassPath(
            final GeneratedSource mixin,
            final List<TypeModel> packageModels,
            final GenerationContext context) {
        if (context.classExistsOutsideOwnOutput(mixin.qualifiedName())) {
            context.reportNote(
                    mixin.qualifiedName()
                            + " also exists on the class path, written by another compilation."
                            + " The one written now only knows the types generated in this"
                            + " compilation: "
                            + packageModels.stream()
                                    .map(TypeModel::qualifiedName)
                                    .collect(Collectors.joining(", ")));
        }
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
