package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.github.marmer.annotationprocessing.MatcherConfiguration")
@AutoService(Processor.class)
public class MatcherGenerationProcessor extends AbstractProcessor {

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        processingEnv.getElementUtils().getPackageElement("org.hamcrest")
                .getEnclosedElements()
                .forEach(o -> {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, o.toString());
                });


        return false;
    }
}
