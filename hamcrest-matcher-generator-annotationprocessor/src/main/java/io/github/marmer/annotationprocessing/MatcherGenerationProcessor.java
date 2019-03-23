package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;
import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator;
import io.github.marmer.annotationprocessing.creation.SourceWriter;
import io.github.marmer.annotationprocessing.extraction.MatcherBaseDescriptorfFactory;

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
    private MatcherBaseDescriptorfFactory matcherBaseDescriptorfFactory;
    private MatcherGenerator matcherGenerator;
    private SourceWriter sourceWriter;
    private MessagerLogger logger;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        matcherGenerator = new JavaPoetMatcherGenerator();
        logger = new MessagerLogger(processingEnv.getMessager());
        matcherBaseDescriptorfFactory = new MatcherBaseDescriptorfFactory(processingEnv, logger);
        sourceWriter = new SourceWriter(processingEnv.getFiler(), logger);
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            logger.info("Annotation processor for hamcrest matcher generation started");

            Stream.concat(
                    roundEnv.getElementsAnnotatedWith(MatcherConfigurations.class).stream()
                            .map(this::toAnnotationConfigurations)
                            .map(MatcherConfigurations::value)
                            .flatMap(Stream::of),
                    roundEnv.getElementsAnnotatedWith(MatcherConfiguration.class).stream()
                            .map(this::toAnnotationConfiguration))
                    .flatMap(matcherBaseDescriptorfFactory::create)
                    .map(matcherGenerator::generateMatcherFor)
                    .forEach(sourceWriter::create);
        }
        return false;
    }

    private MatcherConfiguration toAnnotationConfiguration(final Element element) {
        return element.getAnnotation(MatcherConfiguration.class);
    }

    private MatcherConfigurations toAnnotationConfigurations(final Element element) {
        return element.getAnnotation(MatcherConfigurations.class);
    }
}
