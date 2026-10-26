package io.github.marmer.annotationprocessing;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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

public class MatcherGenerationProcessorWorker {
    private final Supplier<LocalDateTime> timeProvider;
    private final ProcessingEnvironment processingEnv;
    private final String generatorName;

    public MatcherGenerationProcessorWorker(
        final Supplier<LocalDateTime> timeProvider,
        final ProcessingEnvironment processingEnv,
        final String generatorName
    ) {
        this.timeProvider = timeProvider;
        this.processingEnv = processingEnv;
        this.generatorName = generatorName;
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
                    new MatcherGenerator(
                        processingEnv,
                        typeElement,
                        timeProvider,
                        generatorName,
                        List.of(configurationElement),
                        getMatcherConfigurationMirror(configurationElement)
                    ).generate();
                }
            });
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
