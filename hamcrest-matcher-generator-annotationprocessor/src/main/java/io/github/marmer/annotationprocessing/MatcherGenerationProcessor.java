package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.github.marmer.annotationprocessing.MatcherConfiguration")
@AutoService(Processor.class)
public class MatcherGenerationProcessor extends AbstractProcessor {

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Line Loc 0: ");
        if (roundEnv.processingOver()) {
            return true;
        }

        processingEnv.getElementUtils().getPackageElement("org.hamcrest")
                .getEnclosedElements()
                .forEach(o -> {
                    final JavaFileManager.Location location = StandardLocation.SOURCE_OUTPUT;
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Line Loc 1: ", o);
                    try {
                        final FileObject resource = processingEnv.getFiler().createResource(location, processingEnv.getElementUtils().getPackageOf(o).getQualifiedName().toString(), o.getSimpleName() + ".txt", o);
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Line Loc 2: ", o);
                        try (final Writer writer = resource.openWriter()) {
                            writer.write("bla " + System.currentTimeMillis());
                        }
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Line Loc 3: ", o);

                    } catch (final IOException e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error on filecreation: " + e.getMessage(), o);
                    }
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, o.toString());
                });


        return false;
    }
}
