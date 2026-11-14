/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class MatcherGenerationProcessorWorker {
    static final String GENERATE_MATCHERS =
            "io.github.marmer.matchergenerator.annotations.GenerateMatchers";
    static final String GENERATED = "javax.annotation.processing.Generated";
    static final String NULL_MARKED = "org.jspecify.annotations.NullMarked";
    private static final Set<String> GENERATED_OUTPUT_ANNOTATIONS = Set.of(GENERATED, NULL_MARKED);

    /**
     * The deprecated 5.x alias. Referenced by name only, so the core does not depend on a
     * deprecated type.
     */
    static final String MATCHER_CONFIGURATION =
            "io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration";

    private static final Set<String> CONFIGURATION_ANNOTATIONS =
            Set.of(GENERATE_MATCHERS, MATCHER_CONFIGURATION);

    /** Kind of the {@code kotlin.Metadata} annotation that marks a file facade. */
    private static final int KOTLIN_FILE_FACADE_KIND = 2;

    /** Generator name of the 5.x processor. Types it generated are skipped as well. */
    private static final String LEGACY_GENERATOR_NAME =
            "io.github.marmer.annotationprocessing.MatcherGenerationProcessor";

    private final ProcessingEnvironment processingEnv;
    private final String generatorName;
    private final List<GeneratorBackend> backends;
    private final ProcessorDiagnostics diagnostics;
    private final GeneratedSourceWriter sourceWriter;

    /** The configured names that matched nothing when their configuration was seen. */
    private final ConfiguredNames.Memory memory = new ConfiguredNames.Memory();

    public MatcherGenerationProcessorWorker(
            final ProcessingEnvironment processingEnv,
            final String generatorName,
            final List<GeneratorBackend> backends) {
        this.processingEnv = processingEnv;
        this.generatorName = generatorName;
        this.backends = List.copyOf(backends);
        this.diagnostics = new ProcessorDiagnostics(processingEnv);
        this.sourceWriter = new GeneratedSourceWriter(processingEnv, diagnostics);
    }

    private static List<AnnotationMirror> getConfigurationMirrors(final Element element) {
        return element.getAnnotationMirrors().stream()
                .filter(
                        mirror ->
                                CONFIGURATION_ANNOTATIONS.stream()
                                        .anyMatch(name -> ElementSupport.isOfType(mirror, name)))
                .collect(Collectors.toList());
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public boolean process(
            final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            // No type can appear after this round, and a file created now is not compiled. So the
            // names that still match nothing are reported here, once, and nothing is generated.
            reportUnresolvedNames();
            return false;
        }
        diagnostics.matcherGenerationStarted();
        final Set<Element> configurationElements = new LinkedHashSet<>();
        annotations.stream()
                .filter(
                        annotation ->
                                CONFIGURATION_ANNOTATIONS.contains(
                                        annotation.getQualifiedName().toString()))
                .forEach(
                        annotation ->
                                configurationElements.addAll(
                                        roundEnv.getElementsAnnotatedWith(annotation)));
        final List<PlannedType> planned = new ArrayList<>(planRememberedNames());
        configurationElements.forEach(element -> planned.addAll(planConfiguration(element)));
        generate(planned);
        if (configurationElements.isEmpty()) {
            // The matchers written in an earlier round come back without configuration, carrying
            // the annotations the
            // generator writes on its output (javax.annotation.processing.Generated and
            // org.jspecify.annotations.NullMarked).
            // Claiming them keeps javac's -Xlint:all -Werror builds clean: javac would otherwise
            // warn that nobody claims
            // them. The side effect is that javac does not call a later processor in this round
            // when that processor
            // supports exactly those annotation types and nothing else.
            return annotations.stream()
                    .anyMatch(
                            annotation ->
                                    GENERATED_OUTPUT_ANNOTATIONS.contains(
                                            annotation.getQualifiedName().toString()));
        }
        return true;
    }

    /**
     * Plans a configuration of this round. The names that match a type or a package are planned
     * now, as before. A name that matches neither is remembered, with the qualified name of the
     * configuration, and planned in the first later round in which it resolves. The exclusion check
     * of the configuration runs once: here when no name is remembered, otherwise where its last
     * remembered name is planned, or when processing is over.
     */
    private List<PlannedType> planConfiguration(final Element configurationElement) {
        final List<AnnotationMirror> mirrors = getConfigurationMirrors(configurationElement);
        if (mirrors.size() != 1) {
            diagnostics.bothConfigurationAnnotations(configurationElement);
            return List.of();
        }
        final ConfigurationSite configuration =
                new ConfigurationSite(configurationElement, mirrors.get(0));
        final ConfiguredNames names = configuredNamesOf(configuration);
        final List<String> unresolved = names.namesMatchingNothing();
        if (unresolved.isEmpty()) {
            warnAboutUnmatchedExclusions(configuration, names);
        } else {
            memory.remember(qualifiedNameOf(configurationElement), unresolved);
        }
        return planNames(configuration, names, names.names());
    }

    private ConfiguredNames configuredNamesOf(final ConfigurationSite configuration) {
        return ConfiguredNames.of(processingEnv.getElementUtils(), configuration.mirror());
    }

    /**
     * Plans the remembered names that resolve by now, each once, and forgets them. Each remembered
     * configuration is looked up again by its qualified name. When its last remembered name
     * resolves, its exclusion check runs, over all of its names.
     */
    private List<PlannedType> planRememberedNames() {
        final List<PlannedType> planned = new ArrayList<>();
        memory.remembered()
                .forEach(
                        (qualifiedName, remembered) -> {
                            final ConfigurationSite configuration = siteOf(qualifiedName);
                            final ConfiguredNames names = configuredNamesOf(configuration);
                            final List<String> stillUnresolved =
                                    names.namesMatchingNothing(remembered);
                            planned.addAll(planNames(configuration, names, remembered));
                            if (stillUnresolved.isEmpty()) {
                                memory.forget(qualifiedName);
                                warnAboutUnmatchedExclusions(configuration, names);
                            } else {
                                memory.remember(qualifiedName, stillUnresolved);
                            }
                        });
        return planned;
    }

    /**
     * Processing is over, so nothing is generated. Each name that is still remembered matches
     * nothing and gets its diagnostic once, at its value. The exclusion check of such a
     * configuration has not run yet, so it runs here, once, over all of its names.
     */
    private void reportUnresolvedNames() {
        memory.remembered()
                .forEach(
                        (qualifiedName, rememberedNames) -> {
                            final ConfigurationSite configuration = siteOf(qualifiedName);
                            final ConfiguredNames names = configuredNamesOf(configuration);
                            names.entriesOf(rememberedNames)
                                    .forEach(
                                            (name, configuredValue) ->
                                                    reportNotExistingTypeConfigured(
                                                            configuration, configuredValue, name));
                            warnAboutUnmatchedExclusions(configuration, names);
                        });
    }

    /** The configuration class with its annotation, looked up again by its qualified name. */
    private ConfigurationSite siteOf(final String qualifiedName) {
        final TypeElement configurationType =
                processingEnv.getElementUtils().getTypeElement(qualifiedName);
        return new ConfigurationSite(
                configurationType, getConfigurationMirrors(configurationType).get(0));
    }

    /**
     * The qualified name of a configuration class, which is all a later round keeps of it. The
     * annotation targets types only, so the element is a type.
     */
    private static String qualifiedNameOf(final Element configurationElement) {
        return ((TypeElement) configurationElement).getQualifiedName().toString();
    }

    /**
     * Plans those of the given configured names of a configuration that match a type or a package.
     */
    private List<PlannedType> planNames(
            final ConfigurationSite configuration,
            final ConfiguredNames configuredNames,
            final Collection<String> candidates) {
        final Predicate<TypeElement> excluded = configuredNames.exclusions();
        final Map<String, AnnotationValue> entries =
                configuredNames.entriesMatchingTypeOrPackage(candidates);
        final Map<TypeElement, Set<TypeElement>> typesByGeneratedType =
                getAllTypeElementsFor(configuredNames, entries.keySet());
        final Map<String, AnnotationValue> typeEntries =
                configuredNames.entriesNamingTypes(entries.keySet());
        final Set<String> namedTypes = typeEntries.keySet();
        final TypeModelFactory factory =
                new TypeModelFactory(
                        processingEnv, diagnostics, configuration, excluded, typeEntries);
        final List<PlannedType> planned = new ArrayList<>();
        withoutExcludedTypes(typesByGeneratedType, excluded)
                .forEach(
                        (typeElement, configuredTypes) -> {
                            if (isSelfGenerated(typeElement)) {
                                printSkipNoteBecauseOfSelfGenerationFor(typeElement);
                            } else if (factory.isNonPublicOutsideItsPackage(typeElement)) {
                                reportNonPublicType(
                                        typeElement,
                                        configuredTypes,
                                        typeEntries,
                                        configuration,
                                        factory);
                            } else {
                                planned.add(
                                        planFor(
                                                typeElement,
                                                configuredTypes,
                                                namedTypes,
                                                configuration,
                                                factory));
                            }
                        });
        return planned;
    }

    /**
     * A non public top level type is not generated into another package, since the generated code
     * could not use it there. When the configuration names the type, or one of its nested types,
     * that is one error for the type. It sits at the first value of the {@code value} attribute, in
     * the order of the annotation, that names the type or one of its nested types. So with {@code
     * {"p.Outer", "p.Outer.Inner"}} it sits at {@code "p.Outer"}, and with the values in the other
     * order at {@code "p.Outer.Inner"}. A type only a package entry picked up is a verbose note.
     */
    private void reportNonPublicType(
            final TypeElement typeElement,
            final Set<TypeElement> configuredTypes,
            final Map<String, AnnotationValue> typeEntries,
            final ConfigurationSite configuration,
            final TypeModelFactory factory) {
        typeEntries.entrySet().stream()
                .filter(
                        entry ->
                                configuredTypes.stream()
                                        .anyMatch(
                                                configuredType ->
                                                        configuredType
                                                                .getQualifiedName()
                                                                .contentEquals(entry.getKey())))
                .findFirst()
                .ifPresentOrElse(
                        entry ->
                                diagnostics.nonPublicTypeNamed(
                                        typeElement,
                                        factory.targetPackageOf(typeElement),
                                        configuration,
                                        entry.getValue()),
                        () -> diagnostics.nonPublicTypeSkipped(typeElement));
    }

    private PlannedType planFor(
            final TypeElement typeElement,
            final Set<TypeElement> configuredTypes,
            final Set<String> namedTypes,
            final ConfigurationSite configuration,
            final TypeModelFactory factory) {
        final TypeModel model = factory.create(typeElement);
        configuredTypes.forEach(
                configuredType -> reportWithoutProperties(model, configuredType, namedTypes));
        return new PlannedType(typeElement, model, configuration);
    }

    /**
     * Writes the planned types. Every backend learns all of them first, so that generated code can
     * refer to the code generated for another type, whichever configuration asked for it.
     */
    private void generate(final List<PlannedType> planned) {
        final List<TypeModel> models = planned.stream().map(PlannedType::model).toList();
        sourceWriter.startRound(typesByPackage(models));
        final boolean jspecifyAvailable = isJspecifyAvailable();
        planned.forEach(
                type -> {
                    final GenerationContext context =
                            new GenerationContext(
                                    generatorName,
                                    jspecifyAvailable,
                                    models,
                                    this::inheritedMethodNames,
                                    message ->
                                            diagnostics.backendError(type.configuration(), message),
                                    processingEnv
                                            .getOptions()
                                            .containsKey(
                                                    GeneratedSourceWriter.KAPT_KOTLIN_GENERATED),
                                    diagnostics::backendNote,
                                    message ->
                                            diagnostics.backendWarning(
                                                    type.configuration(), message),
                                    diagnostics::backendVerboseNote,
                                    sourceWriter::classExistsOutsideOwnOutput);
                    backends.forEach(
                            backend ->
                                    backend.generate(type.model(), context)
                                            .forEach(
                                                    source ->
                                                            sourceWriter.write(
                                                                    backend,
                                                                    source,
                                                                    type.typeElement(),
                                                                    type.configuration())));
                });
    }

    /** The qualified names of the types the models generate, by their target package. */
    private static Map<String, List<String>> typesByPackage(final List<TypeModel> models) {
        return models.stream()
                .collect(
                        Collectors.groupingBy(
                                TypeModel::targetPackage,
                                Collectors.mapping(TypeModel::qualifiedName, Collectors.toList())));
    }

    /**
     * The names of the methods of a class on the compile class path, inherited ones included. A
     * class that is not on the class path has none.
     */
    private Set<String> inheritedMethodNames(final String qualifiedClassName) {
        final TypeElement type = processingEnv.getElementUtils().getTypeElement(qualifiedClassName);
        if (type == null) {
            return Set.of();
        }
        return ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(type)).stream()
                .filter(method -> !method.getModifiers().contains(Modifier.PRIVATE))
                .map(method -> method.getSimpleName().toString())
                .collect(Collectors.toSet());
    }

    /**
     * Reports the configured type itself, which is the generated type or one of its nested types,
     * when it has no properties. A type the configuration names is warned about. A type that only a
     * package entry picks up is a verbose note instead, since a package holds types that are no
     * match target. A type that is only the container of a configured nested type is never
     * reported.
     */
    private void reportWithoutProperties(
            final TypeModel generatedModel,
            final TypeElement configuredType,
            final Set<String> namedTypes) {
        final String qualifiedName = configuredType.getQualifiedName().toString();
        findModel(generatedModel, qualifiedName)
                .filter(MatcherGenerationProcessorWorker::hasNoProperties)
                .filter(model -> !isKotlinFileFacade(configuredType))
                .ifPresent(
                        model -> {
                            if (namedTypes.contains(qualifiedName)) {
                                diagnostics.typeWithoutProperties(qualifiedName, configuredType);
                            } else {
                                diagnostics.typePickedUpWithoutProperties(
                                        qualifiedName, configuredType);
                            }
                        });
    }

    private static Optional<TypeModel> findModel(
            final TypeModel model, final String qualifiedName) {
        if (model.qualifiedName().equals(qualifiedName)) {
            return Optional.of(model);
        }
        return model.nestedTypes().stream()
                .map(nested -> findModel(nested, qualifiedName))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static boolean hasNoProperties(final TypeModel model) {
        return model.accessorProperties().isEmpty();
    }

    /**
     * The class Kotlin generates for top level functions. It has no properties by design and is no
     * user type.
     */
    private static boolean isKotlinFileFacade(final TypeElement type) {
        return ElementSupport.kotlinMetadata(type).stream()
                .flatMap(mirror -> mirror.getElementValues().entrySet().stream())
                .filter(entry -> entry.getKey().getSimpleName().contentEquals("k"))
                .anyMatch(
                        entry ->
                                Integer.valueOf(KOTLIN_FILE_FACADE_KIND)
                                        .equals(entry.getValue().getValue()));
    }

    /**
     * The types to generate for the given configured names, each with the configured types it
     * stands for. A nested configured type is generated with its highest nesting type, which then
     * is not a configured type itself.
     */
    private static Map<TypeElement, Set<TypeElement>> getAllTypeElementsFor(
            final ConfiguredNames configuredNames, final Collection<String> names) {
        final Map<TypeElement, Set<TypeElement>> result = new LinkedHashMap<>();
        names.forEach(
                name ->
                        configuredNames
                                .typesNamed(name)
                                .forEach(
                                        (type, configured) ->
                                                result.computeIfAbsent(
                                                                type, key -> new LinkedHashSet<>())
                                                        .addAll(configured)));
        return result;
    }

    /**
     * Every exclusion that names none of the types the processor models for the configured names,
     * neither a generated type nor one of their public member types, is one warning at the
     * exclusion. The caller runs it once per configuration, when all of its names are final.
     */
    private void warnAboutUnmatchedExclusions(
            final ConfigurationSite configuration, final ConfiguredNames configuredNames) {
        final List<TypeElement> modelledTypes =
                getAllTypeElementsFor(configuredNames, configuredNames.names()).keySet().stream()
                        .flatMap(generatedType -> withPublicMemberTypes(generatedType).stream())
                        .toList();
        configuredNames
                .exclusionsNamingNone(modelledTypes)
                .forEach(
                        (name, exclusion) ->
                                diagnostics.unmatchedExclusion(name, configuration, exclusion));
    }

    /**
     * The type followed by its public member types, recursively, which is the set of types the type
     * model holds.
     */
    private static List<TypeElement> withPublicMemberTypes(final TypeElement type) {
        final List<TypeElement> result = new ArrayList<>();
        result.add(type);
        type.getEnclosedElements().stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(ElementSupport::isPublic)
                .forEach(member -> result.addAll(withPublicMemberTypes(member)));
        return result;
    }

    /**
     * Drops excluded generated types with all their configured types, and the excluded configured
     * types of the generated types that stay. A generated type without configured types left is not
     * generated.
     */
    private static Map<TypeElement, Set<TypeElement>> withoutExcludedTypes(
            final Map<TypeElement, Set<TypeElement>> typesByGeneratedType,
            final Predicate<TypeElement> excluded) {
        final Map<TypeElement, Set<TypeElement>> result = new LinkedHashMap<>();
        typesByGeneratedType.forEach(
                (generatedType, configuredTypes) -> {
                    final Set<TypeElement> kept =
                            configuredTypes.stream()
                                    .filter(excluded.negate())
                                    .collect(Collectors.toCollection(LinkedHashSet::new));
                    if (!excluded.test(generatedType) && !kept.isEmpty()) {
                        result.put(generatedType, kept);
                    }
                });
        return result;
    }

    /**
     * An unresolvable name is an error for {@code @GenerateMatchers}. The deprecated alias only
     * warns, as it did in 5.x, so existing 5.x configurations keep compiling. The diagnostic points
     * at the failing name.
     */
    private void reportNotExistingTypeConfigured(
            final ConfigurationSite configuration,
            final AnnotationValue configuredValue,
            final String qualifiedTypeOrPackageName) {
        diagnostics.unresolvableName(
                ElementSupport.isOfType(configuration.mirror(), GENERATE_MATCHERS)
                        ? Diagnostic.Kind.ERROR
                        : Diagnostic.Kind.WARNING,
                qualifiedTypeOrPackageName,
                configuration,
                configuredValue);
    }

    private void printSkipNoteBecauseOfSelfGenerationFor(final TypeElement typeElement) {
        typeElement.getAnnotationMirrors().stream()
                .filter(
                        mirror ->
                                ElementSupport.isOfType(mirror, Generated.class.getCanonicalName()))
                .forEach(mirror -> diagnostics.selfGenerationSkipped(typeElement, mirror));
    }

    private boolean isJspecifyAvailable() {
        return processingEnv.getElementUtils().getTypeElement(NULL_MARKED) != null;
    }

    private boolean isSelfGenerated(final Element element) {
        final Generated generated = element.getAnnotation(Generated.class);
        return generated != null
                && Arrays.stream(generated.value())
                        .anyMatch(
                                name ->
                                        name.equals(generatorName)
                                                || name.equals(LEGACY_GENERATOR_NAME));
    }
}
