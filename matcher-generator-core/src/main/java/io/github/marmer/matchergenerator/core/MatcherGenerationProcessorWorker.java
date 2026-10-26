package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public class MatcherGenerationProcessorWorker {
    static final String GENERATE_MATCHERS = "io.github.marmer.matchergenerator.annotations.GenerateMatchers";
    /**
     * The deprecated 5.x alias. Referenced by name only, so the core does not depend on a deprecated type.
     */
    static final String MATCHER_CONFIGURATION =
        "io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration";
    private static final Set<String> CONFIGURATION_ANNOTATIONS = Set.of(GENERATE_MATCHERS, MATCHER_CONFIGURATION);
    /**
     * Processor option. When given (as {@code -Amatchergenerator.verbose} or with the value {@code true}), every note
     * is emitted, including one per skipped property with its reason.
     */
    static final String VERBOSE_OPTION = "matchergenerator.verbose";
    /**
     * The only property Object contributes. It is not something a type declares, so it does not count as a property
     * of a type without properties.
     */
    private static final String OBJECT_CLASS_ACCESSOR = "getClass";
    private static final String KOTLIN_METADATA = "kotlin.Metadata";
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

    static AnnotationValue getAnnotationValueForField(final AnnotationMirror mirror, final String fieldName) {
        final Map<? extends ExecutableElement, ? extends AnnotationValue> values = mirror.getElementValues();
        return values.get(values.keySet().stream()
            .filter(key -> key.getSimpleName().contentEquals(fieldName))
            .findFirst()
            .orElseThrow());
    }

    static AnnotationValue getAnnotationValueForValue(final AnnotationMirror mirror) {
        return getAnnotationValueForField(mirror, "value");
    }

    /**
     * Emits a note, but only when the verbose option is set.
     */
    static void logNote(
        final ProcessingEnvironment processingEnv,
        final String message,
        final Element element,
        final AnnotationMirror annotationMirror,
        final AnnotationValue annotationValue
    ) {
        if (!isVerbose(processingEnv)) {
            return;
        }
        if (annotationMirror == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
        } else {
            processingEnv.getMessager()
                .printMessage(Diagnostic.Kind.NOTE, message, element, annotationMirror, annotationValue);
        }
    }

    static boolean isVerbose(final ProcessingEnvironment processingEnv) {
        final Map<String, String> options = processingEnv.getOptions();
        return options.containsKey(VERBOSE_OPTION)
            && (options.get(VERBOSE_OPTION) == null || Boolean.parseBoolean(options.get(VERBOSE_OPTION)));
    }

    static boolean isPublic(final Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC);
    }

    static boolean isStatic(final Element element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    private static boolean isTypeOf(final AnnotationMirror mirror, final Class<?> annotationType) {
        return mirror.getAnnotationType().asElement().toString().equals(annotationType.getCanonicalName());
    }

    private static boolean isConfiguration(final AnnotationMirror mirror) {
        return CONFIGURATION_ANNOTATIONS.contains(mirror.getAnnotationType().asElement().toString());
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        logNote(processingEnv, "Annotation processor for hamcrest matcher generation started", null, null, null);
        final Set<Element> configurationElements = new LinkedHashSet<>();
        annotations.stream()
            .filter(annotation -> CONFIGURATION_ANNOTATIONS.contains(annotation.getQualifiedName().toString()))
            .forEach(annotation -> configurationElements.addAll(roundEnv.getElementsAnnotatedWith(annotation)));
        if (configurationElements.isEmpty()) {
            return false;
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
        getAllTypeElementsFor(configurationElement, mirrors.get(0))
            .forEach(typeElement -> {
                if (isSelfGenerated(typeElement)) {
                    printSkipNoteBecauseOfSelfGenerationFor(typeElement);
                } else {
                    generateFor(typeElement, configurationElement, mirrors.get(0));
                }
            });
    }

    private void generateFor(
        final TypeElement typeElement,
        final Element configurationElement,
        final AnnotationMirror configurationMirror
    ) {
        final TypeModel model = new TypeModelFactory(processingEnv, configurationMirror)
            .create(typeElement);
        if (hasNoProperties(model) && !isKotlinFileFacade(typeElement)) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                "Type '" + typeElement.getQualifiedName() + "' has no properties to match",
                typeElement
            );
        }
        final GenerationContext context = new GenerationContext(generatorName, timeProvider.get());
        backends.forEach(backend -> write(
            backend.generate(model, context),
            typeElement,
            configurationElement
        ));
    }

    private static boolean hasNoProperties(final TypeModel model) {
        return model.properties().stream().allMatch(property -> property.accessor().equals(OBJECT_CLASS_ACCESSOR));
    }

    /**
     * The class Kotlin generates for top level functions. It has no properties by design and is no user type.
     */
    private static boolean isKotlinFileFacade(final TypeElement type) {
        return type.getAnnotationMirrors().stream()
            .filter(mirror -> mirror.getAnnotationType().asElement().toString().equals(KOTLIN_METADATA))
            .flatMap(mirror -> mirror.getElementValues().entrySet().stream())
            .filter(entry -> entry.getKey().getSimpleName().contentEquals("k"))
            .anyMatch(entry -> Integer.valueOf(KOTLIN_FILE_FACADE_KIND).equals(entry.getValue().getValue()));
    }

    private void write(
        final GeneratedSource source,
        final TypeElement typeElement,
        final Element configurationElement
    ) {
        final Filer filer = processingEnv.getFiler();
        final Element[] originatingElements = {typeElement, configurationElement};
        final String name = source.packageName().isEmpty()
            ? source.simpleName()
            : source.packageName() + "." + source.simpleName();
        try {
            final FileObject file = source.language() == GeneratedSource.Language.JAVA
                ? filer.createSourceFile(name, originatingElements)
                : filer.createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    source.packageName(),
                    source.simpleName() + "." + source.language().fileExtension(),
                    originatingElements);
            try (Writer writer = file.openWriter()) {
                writer.write(source.content());
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<AnnotationMirror> getConfigurationMirrors(final Element configurationElement) {
        return configurationElement.getAnnotationMirrors().stream()
            .filter(MatcherGenerationProcessorWorker::isConfiguration)
            .collect(Collectors.toList());
    }

    private List<TypeElement> getAllTypeElementsFor(
        final Element configurationElement,
        final AnnotationMirror configurationMirror
    ) {
        final Set<TypeElement> result = new LinkedHashSet<>();
        configuredNames(configurationMirror).stream()
            .distinct()
            .forEach(name -> result.addAll(getAllTypeElementsFor(name, configurationElement)));
        return new ArrayList<>(result);
    }

    private static List<String> configuredNames(final AnnotationMirror configurationMirror) {
        return ((List<?>) getAnnotationValueForValue(configurationMirror).getValue()).stream()
            .map(value -> (String) ((AnnotationValue) value).getValue())
            .collect(Collectors.toList());
    }

    private List<TypeElement> getAllTypeElementsFor(
        final String currentQualifiedTypeOrPackageName,
        final Element configurationType
    ) {
        final List<TypeElement> typeElementsForName = new ArrayList<>();
        processingEnv.getElementUtils()
            .getAllPackageElements(currentQualifiedTypeOrPackageName)
            .stream()
            .flatMap(packageElement -> packageElement.getEnclosedElements().stream())
            .map(TypeElement.class::cast)
            .forEach(typeElementsForName::add);

        final TypeElement highestNestingType = getHighestNestingType(
            processingEnv.getElementUtils().getTypeElement(currentQualifiedTypeOrPackageName));
        if (highestNestingType != null) {
            typeElementsForName.add(highestNestingType);
        }

        if (typeElementsForName.isEmpty()) {
            printErrorBecauseOfNotExistingTypeConfigured(configurationType, currentQualifiedTypeOrPackageName);
        }
        return typeElementsForName;
    }

    private TypeElement getHighestNestingType(final TypeElement typeElement) {
        if (typeElement != null && typeElement.getEnclosingElement() instanceof TypeElement) {
            return getHighestNestingType((TypeElement) typeElement.getEnclosingElement());
        }
        return typeElement;
    }

    private void printErrorBecauseOfNotExistingTypeConfigured(
        final Element configurationClass,
        final String qualifiedTypeOrPackageName
    ) {
        configurationClass.getAnnotationMirrors().stream()
            .filter(MatcherGenerationProcessorWorker::isConfiguration)
            .forEach(mirror -> processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Neither a type nor a type exists for '" + qualifiedTypeOrPackageName + "'",
                configurationClass,
                mirror,
                getAnnotationValueForValue(mirror)
            ));
    }

    private void printSkipNoteBecauseOfSelfGenerationFor(final TypeElement typeElement) {
        typeElement.getAnnotationMirrors().stream()
            .filter(mirror -> isTypeOf(mirror, Generated.class))
            .forEach(mirror -> logNote(
                processingEnv,
                "Generation skipped for: '" + typeElement.getQualifiedName()
                    + "' because is is already generated by this processor",
                typeElement,
                mirror,
                getAnnotationValueForValue(mirror)
            ));
    }

    private boolean isSelfGenerated(final Element element) {
        final Generated generated = element.getAnnotation(Generated.class);
        return generated != null && Arrays.stream(generated.value())
            .anyMatch(name -> name.equals(generatorName) || name.equals(LEGACY_GENERATOR_NAME));
    }
}
