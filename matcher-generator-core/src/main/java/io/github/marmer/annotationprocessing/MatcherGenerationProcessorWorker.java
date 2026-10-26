package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.model.TypeModel;
import io.github.marmer.annotationprocessing.spi.GeneratedSource;
import io.github.marmer.annotationprocessing.spi.GenerationContext;
import io.github.marmer.annotationprocessing.spi.GeneratorBackend;
import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
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

    static void logNote(
        final ProcessingEnvironment processingEnv,
        final String message,
        final Element element,
        final AnnotationMirror annotationMirror,
        final AnnotationValue annotationValue
    ) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.NOTE, message, element, annotationMirror, annotationValue);
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

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        logNote(processingEnv, "Annotation processor for hamcrest matcher generation started", null, null, null);
        if (containsMatcherConfiguration(annotations)) {
            roundEnv.getElementsAnnotatedWith(MatcherConfiguration.class)
                .forEach(this::generateMatchers);
            return true;
        }
        return false;
    }

    private boolean containsMatcherConfiguration(final Set<? extends TypeElement> annotations) {
        return annotations.stream()
            .anyMatch(annotation -> MatcherConfiguration.class.getCanonicalName()
                .equals(annotation.getQualifiedName().toString()));
    }

    private void generateMatchers(final Element configurationElement) {
        final MatcherConfiguration matcherConfiguration =
            configurationElement.getAnnotation(MatcherConfiguration.class);
        getAllTypeElementsFor(configurationElement, matcherConfiguration)
            .forEach(typeElement -> {
                if (isSelfGenerated(typeElement)) {
                    printSkipNoteBecauseOfSelfGenerationFor(typeElement);
                } else {
                    generateFor(typeElement, configurationElement);
                }
            });
    }

    private void generateFor(final TypeElement typeElement, final Element configurationElement) {
        final TypeModel model = new TypeModelFactory(processingEnv, getMatcherConfigurationMirror(configurationElement))
            .create(typeElement);
        final GenerationContext context = new GenerationContext(generatorName, timeProvider.get());
        backends.forEach(backend -> write(
            backend.generate(model, context),
            typeElement,
            configurationElement
        ));
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

    private AnnotationMirror getMatcherConfigurationMirror(final Element configurationElement) {
        final List<? extends AnnotationMirror> mirrors = configurationElement.getAnnotationMirrors().stream()
            .filter(mirror -> isTypeOf(mirror, MatcherConfiguration.class))
            .collect(Collectors.toList());
        if (mirrors.size() != 1) {
            throw new IllegalStateException("Expected exactly one MatcherConfiguration on " + configurationElement);
        }
        return mirrors.get(0);
    }

    private List<TypeElement> getAllTypeElementsFor(
        final Element configurationElement,
        final MatcherConfiguration matcherConfiguration
    ) {
        final Set<TypeElement> result = new LinkedHashSet<>();
        Arrays.stream(matcherConfiguration.value())
            .distinct()
            .forEach(name -> result.addAll(getAllTypeElementsFor(name, configurationElement)));
        return new ArrayList<>(result);
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
            printSkipWarningBecauseOfNotExistingTypeConfigured(configurationType, currentQualifiedTypeOrPackageName);
        }
        return typeElementsForName;
    }

    private TypeElement getHighestNestingType(final TypeElement typeElement) {
        if (typeElement != null && typeElement.getEnclosingElement() instanceof TypeElement) {
            return getHighestNestingType((TypeElement) typeElement.getEnclosingElement());
        }
        return typeElement;
    }

    private void printSkipWarningBecauseOfNotExistingTypeConfigured(
        final Element configurationClass,
        final String qualifiedTypeOrPackageName
    ) {
        configurationClass.getAnnotationMirrors().stream()
            .filter(mirror -> isTypeOf(mirror, MatcherConfiguration.class))
            .forEach(mirror -> processingEnv.getMessager().printMessage(
                Diagnostic.Kind.MANDATORY_WARNING,
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
        return generated != null && Arrays.asList(generated.value()).contains(generatorName);
    }
}
