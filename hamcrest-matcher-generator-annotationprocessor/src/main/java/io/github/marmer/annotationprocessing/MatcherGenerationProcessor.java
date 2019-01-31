package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;
import io.github.marmer.annotationprocessing.generation.JavaPoetMatcherGenerator;
import io.github.marmer.annotationprocessing.generation.MatcherGenerator;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"io.github.marmer.annotationprocessing.MatcherConfiguration", "io.github.marmer.annotationprocessing.MatcherConfigurations"})
@AutoService(Processor.class)
public class MatcherGenerationProcessor extends AbstractProcessor {
    private final MatcherGenerator matcherGenerator;

    public MatcherGenerationProcessor(final MatcherGenerator matcherGenerator) {
        this.matcherGenerator = matcherGenerator;
    }

    public MatcherGenerationProcessor() {
        this.matcherGenerator = new JavaPoetMatcherGenerator();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }

        return true;
    }
}
