package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;
import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.creation.SourceWriter;
import io.github.marmer.annotationprocessing.extraction.MatcherBaseDescriptorfFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"io.github.marmer.annotationprocessing.MatcherConfigurations"})
@AutoService(Processor.class)
public class MatcherGenerationProcessor extends AbstractProcessor {
    private MatcherBaseDescriptorfFactory matcherBaseDescriptorfFactory;
    private MatcherGenerator matcherGenerator;
    private SourceWriter sourceWriter;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        matcherGenerator = new JavaPoetMatcherGenerator();
        matcherBaseDescriptorfFactory = new MatcherBaseDescriptorfFactory(processingEnv);
        sourceWriter = new SourceWriter(processingEnv.getFiler());
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            // TODO: marmer 31.01.2019 don't forget to test
            return true;
        }

        roundEnv.getElementsAnnotatedWith(MatcherConfigurations.class).stream()
                .map(toAnnotationConfiguration())
                .flatMap(toMatcherBaseDescriptorStream())
                .map(matcherGenerator::generateMatcherFor)
                .forEach(sourceWriter::create);

        return true;
    }

    private Function<MatcherConfigurations, Stream<? extends MatcherBaseDescriptor>> toMatcherBaseDescriptorStream() {
        return configurations -> matcherBaseDescriptorfFactory.create(configurations).stream();
    }

    private Function<Element, MatcherConfigurations> toAnnotationConfiguration() {
        return element -> element.getAnnotation(MatcherConfigurations.class);
    }
}
