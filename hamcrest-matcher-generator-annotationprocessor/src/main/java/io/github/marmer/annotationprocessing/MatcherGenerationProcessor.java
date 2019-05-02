package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;
import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.creation.SourceWriter;
import io.github.marmer.annotationprocessing.extraction.MatcherBaseDescriptorFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.stream.Stream;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
        "io.github.marmer.annotationprocessing.MatcherConfigurations",
        "io.github.marmer.annotationprocessing.MatcherConfiguration"})
@AutoService(Processor.class)
public class MatcherGenerationProcessor extends AbstractProcessor {
    private MatcherBaseDescriptorFactory matcherBaseDescriptorFactory;
    private MatcherGenerator matcherGenerator;
    private SourceWriter sourceWriter;
    private MessagerLogger logger;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        matcherGenerator = new JavaPoetMatcherGenerator();
        logger = new MessagerLogger("Hamcrest-Matcher-Generator", processingEnv.getMessager());
        matcherBaseDescriptorFactory = new MatcherBaseDescriptorFactory(processingEnv, logger);
        sourceWriter = new SourceWriter(processingEnv.getFiler(), logger);
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            logger.info("Annotation processor for hamcrest matcher generation started");

            processPluralConfigurationElements(roundEnv);
            processSingularConfigurationElement(roundEnv);
        }
        return false;
    }


    private void processSingularConfigurationElement(final RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(MatcherConfiguration.class).stream()
                .forEachOrdered(this::processSingularConfigurationElement);
    }

    private void processSingularConfigurationElement(final Element element) {
        logger.setCurrentElement(element);
        performMatcherGeneration(element.getAnnotation(MatcherConfiguration.class));
    }

    private void processPluralConfigurationElements(final RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(MatcherConfigurations.class).stream()
                .forEachOrdered(this::processPluralConfigurationElements);
    }

    private void processPluralConfigurationElements(final Element element) {
        logger.setCurrentElement(element);
        final MatcherConfigurations matcherConfigurations = toAnnotationConfigurations(element);
        Stream.of(matcherConfigurations.value())
                .flatMap(Stream::of)
                .forEach(this::performMatcherGeneration);
    }

    private Stream<MatcherSourceDescriptor> toSourcecode(final MatcherBaseDescriptor descriptor) {
        try {
            return Stream.of(matcherGenerator.generateMatcherFor(descriptor));
        } catch (final RuntimeException e) {
            logger.error("Hamcrest matcher generation stopped for '" + descriptor.getBase().getFullQualifiedName() + "' because of an unexpected error: " + e.getMessage());
        }
        return Stream.empty();
    }

    private MatcherConfigurations toAnnotationConfigurations(final Element element) {
        return element.getAnnotation(MatcherConfigurations.class);
    }

    private void performMatcherGeneration(final MatcherConfiguration matcherConfiguration) {
        matcherBaseDescriptorFactory.create(matcherConfiguration)
                .flatMap(this::toSourcecode)
                .forEach(sourceWriter::create);
    }
}
