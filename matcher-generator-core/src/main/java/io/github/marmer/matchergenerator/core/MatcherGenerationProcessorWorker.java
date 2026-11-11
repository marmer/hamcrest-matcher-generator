/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String EXCLUDE_ATTRIBUTE = "exclude";

    /** Kind of the {@code kotlin.Metadata} annotation that marks a file facade. */
    private static final int KOTLIN_FILE_FACADE_KIND = 2;

    /** Generator name of the 5.x processor. Types it generated are skipped as well. */
    private static final String LEGACY_GENERATOR_NAME =
            "io.github.marmer.annotationprocessing.MatcherGenerationProcessor";

    private final Supplier<LocalDateTime> timeProvider;
    private final ProcessingEnvironment processingEnv;
    private final String generatorName;
    private final List<GeneratorBackend> backends;
    private final ProcessorDiagnostics diagnostics;
    private final GeneratedSourceWriter sourceWriter;

    public MatcherGenerationProcessorWorker(
            final Supplier<LocalDateTime> timeProvider,
            final ProcessingEnvironment processingEnv,
            final String generatorName,
            final List<GeneratorBackend> backends) {
        this.timeProvider = timeProvider;
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
        final List<PlannedType> planned = new ArrayList<>();
        configurationElements.forEach(element -> planned.addAll(planMatchers(element)));
        generate(planned);
        return true;
    }

    private List<PlannedType> planMatchers(final Element configurationElement) {
        final List<AnnotationMirror> mirrors = getConfigurationMirrors(configurationElement);
        if (mirrors.size() != 1) {
            diagnostics.bothConfigurationAnnotations(configurationElement);
            return List.of();
        }
        final AnnotationMirror configurationMirror = mirrors.get(0);
        final ConfigurationSite configuration =
                new ConfigurationSite(configurationElement, configurationMirror);
        final Map<String, AnnotationValue> exclusions =
                distinctEntries(configurationMirror, EXCLUDE_ATTRIBUTE);
        final Predicate<TypeElement> excluded = excludedBy(exclusions);
        final Map<TypeElement, Set<TypeElement>> typesByGeneratedType =
                getAllTypeElementsFor(configuration);
        warnAboutUnmatchedExclusions(typesByGeneratedType, configuration, exclusions);
        final Map<String, AnnotationValue> namedEntries =
                namedEntries(distinctEntries(configurationMirror, VALUE_ATTRIBUTE));
        final Set<String> namedTypes = namedEntries.keySet();
        final TypeModelFactory factory =
                new TypeModelFactory(
                        processingEnv, diagnostics, configuration, excluded, namedEntries);
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
                                        namedEntries,
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
            final Map<String, AnnotationValue> namedEntries,
            final ConfigurationSite configuration,
            final TypeModelFactory factory) {
        namedEntries.entrySet().stream()
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

    /**
     * The entries of the {@code value} attribute that name a type, keyed by the qualified name of
     * that type, in the order of the annotation. Names that are no type, packages, are not among
     * them.
     */
    private Map<String, AnnotationValue> namedEntries(
            final Map<String, AnnotationValue> configuredEntries) {
        final Map<String, AnnotationValue> named = new LinkedHashMap<>();
        configuredEntries.forEach(
                (name, entry) -> {
                    final TypeElement type = processingEnv.getElementUtils().getTypeElement(name);
                    if (type != null) {
                        named.putIfAbsent(type.getQualifiedName().toString(), entry);
                    }
                });
        return named;
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
        final LocalDateTime generationTime = timeProvider.get();
        final boolean jspecifyAvailable = isJspecifyAvailable();
        planned.forEach(
                type -> {
                    final GenerationContext context =
                            new GenerationContext(
                                    generatorName,
                                    generationTime,
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
     * The types to generate for, each with the configured types it stands for. A nested configured
     * type is generated with its highest nesting type, which then is not a configured type itself.
     */
    private Map<TypeElement, Set<TypeElement>> getAllTypeElementsFor(
            final ConfigurationSite configuration) {
        final Map<TypeElement, Set<TypeElement>> result = new LinkedHashMap<>();
        distinctEntries(configuration.mirror(), VALUE_ATTRIBUTE)
                .forEach(
                        (name, configuredValue) ->
                                getAllTypeElementsFor(
                                        name, configuration, configuredValue, result));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<AnnotationValue> attributeValues(
            final AnnotationMirror configurationMirror, final String name) {
        return (List<AnnotationValue>)
                ElementSupport.annotationValue(processingEnv, configurationMirror, name).getValue();
    }

    /**
     * The entries of a list attribute, each name once, with the value that named it first, so that
     * a diagnostic can point at it.
     */
    private Map<String, AnnotationValue> distinctEntries(
            final AnnotationMirror configurationMirror, final String name) {
        final Map<String, AnnotationValue> entries = new LinkedHashMap<>();
        attributeValues(configurationMirror, name)
                .forEach(entry -> entries.putIfAbsent((String) entry.getValue(), entry));
        return entries;
    }

    private Predicate<TypeElement> excludedBy(final Map<String, AnnotationValue> exclusions) {
        return type -> exclusions.keySet().stream().anyMatch(entry -> isNamedBy(type, entry));
    }

    /**
     * Every exclusion that names none of the types the processor models, neither a generated type
     * nor one of their public member types, is one warning at the exclusion.
     */
    private void warnAboutUnmatchedExclusions(
            final Map<TypeElement, Set<TypeElement>> typesByGeneratedType,
            final ConfigurationSite configuration,
            final Map<String, AnnotationValue> exclusions) {
        final List<TypeElement> modelledTypes =
                typesByGeneratedType.keySet().stream()
                        .flatMap(generatedType -> withPublicMemberTypes(generatedType).stream())
                        .toList();
        exclusions.forEach(
                (name, exclusion) -> {
                    if (modelledTypes.stream().noneMatch(type -> isNamedBy(type, name))) {
                        diagnostics.unmatchedExclusion(name, configuration, exclusion);
                    }
                });
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
     * Whether a configuration entry names the type, either by its qualified name or by its package.
     */
    private boolean isNamedBy(final TypeElement type, final String entry) {
        return entry.equals(type.getQualifiedName().toString())
                || entry.equals(
                        processingEnv
                                .getElementUtils()
                                .getPackageOf(type)
                                .getQualifiedName()
                                .toString());
    }

    private void getAllTypeElementsFor(
            final String currentQualifiedTypeOrPackageName,
            final ConfigurationSite configuration,
            final AnnotationValue configuredValue,
            final Map<TypeElement, Set<TypeElement>> result) {
        final Map<TypeElement, Set<TypeElement>> found = new LinkedHashMap<>();
        processingEnv
                .getElementUtils()
                .getAllPackageElements(currentQualifiedTypeOrPackageName)
                .stream()
                .flatMap(packageElement -> packageElement.getEnclosedElements().stream())
                .map(TypeElement.class::cast)
                .forEach(
                        type ->
                                found.computeIfAbsent(type, key -> new LinkedHashSet<>())
                                        .add(type));

        final TypeElement configuredType =
                processingEnv.getElementUtils().getTypeElement(currentQualifiedTypeOrPackageName);
        final TypeElement highestNestingType = getHighestNestingType(configuredType);
        if (highestNestingType != null) {
            found.computeIfAbsent(highestNestingType, key -> new LinkedHashSet<>())
                    .add(configuredType);
        }

        if (found.isEmpty()) {
            reportNotExistingTypeConfigured(
                    configuration, configuredValue, currentQualifiedTypeOrPackageName);
        }
        found.forEach(
                (type, configured) ->
                        result.computeIfAbsent(type, key -> new LinkedHashSet<>())
                                .addAll(configured));
    }

    private TypeElement getHighestNestingType(final TypeElement typeElement) {
        if (typeElement != null
                && typeElement.getEnclosingElement() instanceof TypeElement enclosing) {
            return getHighestNestingType(enclosing);
        }
        return typeElement;
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
