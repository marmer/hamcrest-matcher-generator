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
import javax.tools.Diagnostic;
import java.util.Set;

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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Annotation processor for hamcrest matcher generation started");

        roundEnv.getElementsAnnotatedWith(MatcherConfigurations.class).stream()
                .map(this::toAnnotationConfiguration)
                .flatMap(matcherBaseDescriptorfFactory::create)
                .map(matcherGenerator::generateMatcherFor)
                .forEach(sourceWriter::create);

        return true;
    }

    private MatcherConfigurations toAnnotationConfiguration(final Element element) {
        return element.getAnnotation(MatcherConfigurations.class);
    }
}
