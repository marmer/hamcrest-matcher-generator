package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;
import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.creation.SourceWriter;
import io.github.marmer.annotationprocessing.extraction.MatcherBaseDescriptorfFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"io.github.marmer.annotationprocessing.MatcherConfigurations"})
@AutoService(Processor.class)
public class MatcherGenerationProcessor extends AbstractProcessor {
    private final MatcherBaseDescriptorfFactory matcherBaseDescriptorfFactory;
    private final MatcherGenerator matcherGenerator;
    private final SourceWriter sourceWriter;

    public MatcherGenerationProcessor(final MatcherGenerator matcherGenerator, final MatcherBaseDescriptorfFactory matcherBaseDescriptorfFactory, final SourceWriter sourceWriter) {
        this.matcherGenerator = matcherGenerator;
        this.matcherBaseDescriptorfFactory = matcherBaseDescriptorfFactory;
        this.sourceWriter = sourceWriter;
    }

    public MatcherGenerationProcessor() {
        this(new JavaPoetMatcherGenerator(), new MatcherBaseDescriptorfFactory(), new SourceWriter());
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
                .forEach(createSources());

        return true;
    }

    private Consumer<MatcherSourceDescriptor> createSources() {
        return matcherSourceDescriptor -> sourceWriter.create(processingEnv.getFiler(), matcherSourceDescriptor);
    }

    private Function<MatcherConfigurations, Stream<? extends MatcherBaseDescriptor>> toMatcherBaseDescriptorStream() {
        return configurations -> matcherBaseDescriptorfFactory.create(configurations, processingEnv).stream();
    }

    private Function<Element, MatcherConfigurations> toAnnotationConfiguration() {
        return element -> element.getAnnotation(MatcherConfigurations.class);
    }
}
