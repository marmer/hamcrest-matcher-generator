package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

public class MatcherGenerationProcessorWorker {
    /**
     * The processor option KAPT passes with the directory Kotlin sources have to be written to.
     */
    static final String KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated";
    static final String GENERATE_MATCHERS = "io.github.marmer.matchergenerator.annotations.GenerateMatchers";
    static final String GENERATED = "javax.annotation.processing.Generated";
    static final String NULL_MARKED = "org.jspecify.annotations.NullMarked";
    private static final Set<String> GENERATED_OUTPUT_ANNOTATIONS = Set.of(GENERATED, NULL_MARKED);
    /**
     * The deprecated 5.x alias. Referenced by name only, so the core does not depend on a deprecated type.
     */
    static final String MATCHER_CONFIGURATION =
        "io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration";
    private static final Set<String> CONFIGURATION_ANNOTATIONS = Set.of(GENERATE_MATCHERS, MATCHER_CONFIGURATION);
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String EXCLUDE_ATTRIBUTE = "exclude";
    /**
     * The only property Object contributes. It is not something a type declares, so it does not count as a property
     * of a type without properties.
     */
    private static final String OBJECT_CLASS_ACCESSOR = "getClass";
    /**
     * Kind of the {@code kotlin.Metadata} annotation that marks a file facade.
     */
    private static final int KOTLIN_FILE_FACADE_KIND = 2;
    /**
     * Generator name of the 5.x processor. Types it generated are skipped as well.
     */
    private static final String LEGACY_GENERATOR_NAME = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor";

    private final Supplier<LocalDateTime> timeProvider;
    private final ProcessingEnvironment processingEnv;
    private final String generatorName;
    private final List<GeneratorBackend> backends;

    public MatcherGenerationProcessorWorker(
        final Supplier<LocalDateTime> timeProvider,
        final ProcessingEnvironment processingEnv,
        final String generatorName,
        final List<GeneratorBackend> backends
    ) {
        this.timeProvider = timeProvider;
        this.processingEnv = processingEnv;
        this.generatorName = generatorName;
        this.backends = List.copyOf(backends);
    }

    private static List<AnnotationMirror> getConfigurationMirrors(final Element element) {
        return element.getAnnotationMirrors().stream()
            .filter(mirror -> CONFIGURATION_ANNOTATIONS.stream().anyMatch(name -> ElementSupport.isOfType(mirror, name)))
            .collect(Collectors.toList());
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        ElementSupport.logNote(processingEnv, "Matcher generation started", null);
        final Set<Element> configurationElements = new LinkedHashSet<>();
        annotations.stream()
            .filter(annotation -> CONFIGURATION_ANNOTATIONS.contains(annotation.getQualifiedName().toString()))
            .forEach(annotation -> configurationElements.addAll(roundEnv.getElementsAnnotatedWith(annotation)));
        if (configurationElements.isEmpty()) {
            // The matchers written in an earlier round come back without configuration, carrying the annotations the
            // generator writes on its output. Claiming them keeps javac's processing lint quiet about annotations nobody
            // claims.
            return annotations.stream()
                .anyMatch(annotation -> GENERATED_OUTPUT_ANNOTATIONS.contains(annotation.getQualifiedName().toString()));
        }
        configurationElements.forEach(this::generateMatchers);
        return true;
    }

    private void generateMatchers(final Element configurationElement) {
        final List<AnnotationMirror> mirrors = getConfigurationMirrors(configurationElement);
        if (mirrors.size() != 1) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Use either @GenerateMatchers or @MatcherConfiguration on a type, not both",
                configurationElement
            );
            return;
        }
        final AnnotationMirror configurationMirror = mirrors.get(0);
        final Map<String, AnnotationValue> exclusions = distinctEntries(configurationMirror, EXCLUDE_ATTRIBUTE);
        final Predicate<TypeElement> excluded = excludedBy(exclusions);
        final Map<TypeElement, Set<TypeElement>> typesByGeneratedType =
            getAllTypeElementsFor(configurationElement, configurationMirror);
        warnAboutUnmatchedExclusions(typesByGeneratedType, configurationElement, configurationMirror, exclusions);
        final Set<String> namedTypes = namedTypes(configurationMirror);
        withoutExcludedTypes(typesByGeneratedType, excluded).forEach((typeElement, configuredTypes) -> {
            if (isSelfGenerated(typeElement)) {
                printSkipNoteBecauseOfSelfGenerationFor(typeElement);
            } else {
                generateFor(typeElement, configuredTypes, namedTypes, configurationElement, configurationMirror,
                    excluded);
            }
        });
    }

    /**
     * The qualified names of the types the configuration names explicitly. Names that are no type, packages, are not
     * among them.
     */
    private Set<String> namedTypes(final AnnotationMirror configurationMirror) {
        return distinctEntries(configurationMirror, VALUE_ATTRIBUTE).keySet().stream()
            .map(name -> processingEnv.getElementUtils().getTypeElement(name))
            .filter(Objects::nonNull)
            .map(type -> type.getQualifiedName().toString())
            .collect(Collectors.toSet());
    }

    private void generateFor(
        final TypeElement typeElement,
        final Set<TypeElement> configuredTypes,
        final Set<String> namedTypes,
        final Element configurationElement,
        final AnnotationMirror configurationMirror,
        final Predicate<TypeElement> excluded
    ) {
        final TypeModel model = new TypeModelFactory(processingEnv, configurationMirror, excluded)
            .create(typeElement);
        configuredTypes.forEach(configuredType -> reportWithoutProperties(model, configuredType, namedTypes));
        final GenerationContext context = new GenerationContext(generatorName, timeProvider.get(), isJspecifyAvailable());
        backends.forEach(backend -> write(
            backend,
            backend.generate(model, context),
            typeElement,
            configurationElement
        ));
    }

    /**
     * Reports the configured type itself, which is the generated type or one of its nested types, when it has no
     * properties. A type the configuration names is warned about. A type that only a package entry picks up is a
     * verbose note instead, since a package holds types that are no match target. A type that is only the container
     * of a configured nested type is never reported.
     */
    private void reportWithoutProperties(
        final TypeModel generatedModel,
        final TypeElement configuredType,
        final Set<String> namedTypes
    ) {
        final String qualifiedName = configuredType.getQualifiedName().toString();
        findModel(generatedModel, qualifiedName)
            .filter(MatcherGenerationProcessorWorker::hasNoProperties)
            .filter(model -> !isKotlinFileFacade(configuredType))
            .ifPresent(model -> {
                if (namedTypes.contains(qualifiedName)) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "Type '" + qualifiedName + "' has no properties to match",
                        configuredType
                    );
                } else {
                    ElementSupport.logNote(
                        processingEnv,
                        "Type '" + qualifiedName + "' picked up by a package entry has no properties to match",
                        configuredType
                    );
                }
            });
    }

    private static Optional<TypeModel> findModel(final TypeModel model, final String qualifiedName) {
        if (model.qualifiedName().equals(qualifiedName)) {
            return Optional.of(model);
        }
        return model.nestedTypes().stream()
            .map(nested -> findModel(nested, qualifiedName))
            .flatMap(Optional::stream)
            .findFirst();
    }

    private static boolean hasNoProperties(final TypeModel model) {
        return model.properties().stream().allMatch(property -> property.accessor().equals(OBJECT_CLASS_ACCESSOR));
    }

    /**
     * The class Kotlin generates for top level functions. It has no properties by design and is no user type.
     */
    private static boolean isKotlinFileFacade(final TypeElement type) {
        return ElementSupport.kotlinMetadata(type).stream()
            .flatMap(mirror -> mirror.getElementValues().entrySet().stream())
            .filter(entry -> entry.getKey().getSimpleName().contentEquals("k"))
            .anyMatch(entry -> Integer.valueOf(KOTLIN_FILE_FACADE_KIND).equals(entry.getValue().getValue()));
    }

    private void write(
        final GeneratorBackend backend,
        final GeneratedSource source,
        final TypeElement typeElement,
        final Element configurationElement
    ) {
        final Element[] originatingElements = {typeElement, configurationElement};
        try {
            if (source.language() == GeneratedSource.Language.JAVA) {
                writeJava(source, originatingElements);
            } else {
                writeKotlin(backend, source, typeElement);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeJava(final GeneratedSource source, final Element[] originatingElements) throws IOException {
        final String name = source.packageName().isEmpty()
            ? source.simpleName()
            : source.packageName() + "." + source.simpleName();
        final FileObject file = processingEnv.getFiler().createSourceFile(name, originatingElements);
        try (Writer writer = file.openWriter()) {
            writer.write(source.content());
        }
    }

    private void writeKotlin(
        final GeneratorBackend backend,
        final GeneratedSource source,
        final TypeElement typeElement
    ) throws IOException {
        final String outputDirectory = processingEnv.getOptions().get(KAPT_KOTLIN_GENERATED);
        if (outputDirectory == null) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Backend " + backend.getClass().getName() + " generated Kotlin for "
                    + typeElement.getQualifiedName() + ", which is only compiled when the annotation processor runs"
                    + " with KAPT (processor option '" + KAPT_KOTLIN_GENERATED + "' is missing)",
                typeElement);
            return;
        }
        final Path directory = Path.of(outputDirectory, source.packageName().split("\\."));
        Files.createDirectories(directory);
        Files.writeString(
            directory.resolve(source.simpleName() + "." + source.language().fileExtension()),
            source.content());
    }

    /**
     * The types to generate for, each with the configured types it stands for. A nested configured type is generated
     * with its highest nesting type, which then is not a configured type itself.
     */
    private Map<TypeElement, Set<TypeElement>> getAllTypeElementsFor(
        final Element configurationElement,
        final AnnotationMirror configurationMirror
    ) {
        final Map<TypeElement, Set<TypeElement>> result = new LinkedHashMap<>();
        distinctEntries(configurationMirror, VALUE_ATTRIBUTE).forEach((name, configuredValue) ->
            getAllTypeElementsFor(name, configurationElement, configurationMirror, configuredValue, result));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<AnnotationValue> attributeValues(final AnnotationMirror configurationMirror, final String name) {
        return (List<AnnotationValue>) ElementSupport.annotationValue(processingEnv, configurationMirror, name)
            .getValue();
    }

    /**
     * The entries of a list attribute, each name once, with the value that named it first, so that a diagnostic can
     * point at it.
     */
    private Map<String, AnnotationValue> distinctEntries(final AnnotationMirror configurationMirror, final String name) {
        final Map<String, AnnotationValue> entries = new LinkedHashMap<>();
        attributeValues(configurationMirror, name)
            .forEach(entry -> entries.putIfAbsent((String) entry.getValue(), entry));
        return entries;
    }

    private Predicate<TypeElement> excludedBy(final Map<String, AnnotationValue> exclusions) {
        return type -> exclusions.keySet().stream().anyMatch(entry -> isNamedBy(type, entry));
    }

    /**
     * Every exclusion that names none of the types the processor models, neither a generated type nor one of their
     * public member types, is one warning at the exclusion.
     */
    private void warnAboutUnmatchedExclusions(
        final Map<TypeElement, Set<TypeElement>> typesByGeneratedType,
        final Element configurationElement,
        final AnnotationMirror configurationMirror,
        final Map<String, AnnotationValue> exclusions
    ) {
        final List<TypeElement> modelledTypes = typesByGeneratedType.keySet().stream()
            .flatMap(generatedType -> withPublicMemberTypes(generatedType).stream())
            .collect(Collectors.toList());
        exclusions.forEach((name, exclusion) -> {
            if (modelledTypes.stream().noneMatch(type -> isNamedBy(type, name))) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Exclusion '" + name + "' matches no configured type",
                    configurationElement,
                    configurationMirror,
                    exclusion
                );
            }
        });
    }

    /**
     * The type followed by its public member types, recursively, which is the set of types the type model holds.
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
     * Drops excluded generated types with all their configured types, and the excluded configured types of the
     * generated types that stay. A generated type without configured types left is not generated.
     */
    private static Map<TypeElement, Set<TypeElement>> withoutExcludedTypes(
        final Map<TypeElement, Set<TypeElement>> typesByGeneratedType,
        final Predicate<TypeElement> excluded
    ) {
        final Map<TypeElement, Set<TypeElement>> result = new LinkedHashMap<>();
        typesByGeneratedType.forEach((generatedType, configuredTypes) -> {
            final Set<TypeElement> kept = configuredTypes.stream()
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
            || entry.equals(processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString());
    }

    private void getAllTypeElementsFor(
        final String currentQualifiedTypeOrPackageName,
        final Element configurationType,
        final AnnotationMirror configurationMirror,
        final AnnotationValue configuredValue,
        final Map<TypeElement, Set<TypeElement>> result
    ) {
        final Map<TypeElement, Set<TypeElement>> found = new LinkedHashMap<>();
        processingEnv.getElementUtils()
            .getAllPackageElements(currentQualifiedTypeOrPackageName)
            .stream()
            .flatMap(packageElement -> packageElement.getEnclosedElements().stream())
            .map(TypeElement.class::cast)
            .forEach(type -> found.computeIfAbsent(type, key -> new LinkedHashSet<>()).add(type));

        final TypeElement configuredType =
            processingEnv.getElementUtils().getTypeElement(currentQualifiedTypeOrPackageName);
        final TypeElement highestNestingType = getHighestNestingType(configuredType);
        if (highestNestingType != null) {
            found.computeIfAbsent(highestNestingType, key -> new LinkedHashSet<>()).add(configuredType);
        }

        if (found.isEmpty()) {
            printErrorBecauseOfNotExistingTypeConfigured(
                configurationType, configurationMirror, configuredValue, currentQualifiedTypeOrPackageName);
        }
        found.forEach((type, configured) ->
            result.computeIfAbsent(type, key -> new LinkedHashSet<>()).addAll(configured));
    }

    private TypeElement getHighestNestingType(final TypeElement typeElement) {
        if (typeElement != null && typeElement.getEnclosingElement() instanceof TypeElement) {
            return getHighestNestingType((TypeElement) typeElement.getEnclosingElement());
        }
        return typeElement;
    }

    /**
     * An unresolvable name is an error for {@code @GenerateMatchers}. The deprecated alias only warns, as it did in
     * 5.x, so existing 5.x configurations keep compiling. The diagnostic points at the failing name.
     */
    private void printErrorBecauseOfNotExistingTypeConfigured(
        final Element configurationClass,
        final AnnotationMirror mirror,
        final AnnotationValue configuredValue,
        final String qualifiedTypeOrPackageName
    ) {
        processingEnv.getMessager().printMessage(
            ElementSupport.isOfType(mirror, GENERATE_MATCHERS) ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING,
            "Neither a type nor a package exists for '" + qualifiedTypeOrPackageName + "'",
            configurationClass,
            mirror,
            configuredValue
        );
    }

    private void printSkipNoteBecauseOfSelfGenerationFor(final TypeElement typeElement) {
        typeElement.getAnnotationMirrors().stream()
            .filter(mirror -> ElementSupport.isOfType(mirror, Generated.class.getCanonicalName()))
            .forEach(mirror -> ElementSupport.logNote(
                processingEnv,
                "Generation skipped for: '" + typeElement.getQualifiedName()
                    + "' because it is already generated by this processor",
                typeElement,
                mirror,
                ElementSupport.valueAttribute(processingEnv, mirror)
            ));
    }

    private boolean isJspecifyAvailable() {
        return processingEnv.getElementUtils().getTypeElement("org.jspecify.annotations.NullMarked") != null;
    }

    private boolean isSelfGenerated(final Element element) {
        final Generated generated = element.getAnnotation(Generated.class);
        return generated != null && Arrays.stream(generated.value())
            .anyMatch(name -> name.equals(generatorName) || name.equals(LEGACY_GENERATOR_NAME));
    }
}
