package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.model.TypeModel;
import io.github.marmer.annotationprocessing.spi.GeneratedSource;
import io.github.marmer.annotationprocessing.spi.GenerationContext;
import io.github.marmer.annotationprocessing.spi.GeneratorBackend;
import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

public class MatcherGenerationProcessorWorker {
    /**
     * The processor option KAPT passes with the directory Kotlin sources have to be written to.
     */
    static final String KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated";

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

    private static boolean isTypeOf(final AnnotationMirror mirror, final Class<?> annotationType) {
        return mirror.getAnnotationType().asElement().toString().equals(annotationType.getCanonicalName());
    }

    private static List<AnnotationMirror> matcherConfigurationMirrors(final Element element) {
        return element.getAnnotationMirrors().stream()
            .filter(mirror -> isTypeOf(mirror, MatcherConfiguration.class))
            .collect(Collectors.toList());
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        ElementSupport.logNote(processingEnv, "Matcher generation started", null, null, null);
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
            backend,
            backend.generate(model, context),
            typeElement,
            configurationElement
        ));
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

    private AnnotationMirror getMatcherConfigurationMirror(final Element configurationElement) {
        final List<AnnotationMirror> mirrors = matcherConfigurationMirrors(configurationElement);
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
        matcherConfigurationMirrors(configurationClass)
            .forEach(mirror -> processingEnv.getMessager().printMessage(
                Diagnostic.Kind.MANDATORY_WARNING,
                "Neither a type nor a type exists for '" + qualifiedTypeOrPackageName + "'",
                configurationClass,
                mirror,
                ElementSupport.valueAttribute(processingEnv, mirror)
            ));
    }

    private void printSkipNoteBecauseOfSelfGenerationFor(final TypeElement typeElement) {
        typeElement.getAnnotationMirrors().stream()
            .filter(mirror -> isTypeOf(mirror, Generated.class))
            .forEach(mirror -> ElementSupport.logNote(
                processingEnv,
                "Generation skipped for: '" + typeElement.getQualifiedName()
                    + "' because is is already generated by this processor",
                typeElement,
                mirror,
                ElementSupport.valueAttribute(processingEnv, mirror)
            ));
    }

    private boolean isSelfGenerated(final Element element) {
        final Generated generated = element.getAnnotation(Generated.class);
        return generated != null && Arrays.asList(generated.value()).contains(generatorName);
    }
}
