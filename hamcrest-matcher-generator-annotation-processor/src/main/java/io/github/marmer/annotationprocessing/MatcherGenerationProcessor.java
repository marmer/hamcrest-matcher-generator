package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.lang.model.element.ElementKind.METHOD;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"io.github.marmer.annotationprocessing.MatcherConfiguration", "io.github.marmer.annotationprocessing.MatcherConfigurations", "io.github.marmer.annotationprocessing.MatcherClassConfiguration"})
@AutoService(Processor.class)
public class MatcherGenerationProcessor extends AbstractProcessor {

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        return spike(annotations, roundEnv);
    }

    public boolean spike(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        // TODO: marmer 24.01.2019 find all types of a package recursively

        // TODO: marmer 24.01.2019 find all methods of a type
        // TODO: marmer 24.01.2019 find all parameter types
        // TODO: marmer 25.01.2019 find a way to execute the annotation processor after lombok (and possibly other processors) have been executed

        if (roundEnv.processingOver()) {
            return true;
        }

        final Class<MatcherConfiguration> annotationType = MatcherConfiguration.class;
        final List<MatcherConfiguration> annotation = getAnnotation(roundEnv, annotationType);

        final PackageElement requestedPackage = processingEnv.getElementUtils().getPackageElement("io.github.marmer.annotationprocessingtest.sample");
        final TypeElement requestedType = processingEnv.getElementUtils().getTypeElement("io.github.marmer.annotationprocessingtest.sample.SamplePojo");

        if (requestedType != null) {
            final List<? extends Element> packageElements = requestedType
                    .getEnclosedElements();
            packageElements.stream().forEach(this::printWarning);
        }

        // a way to test whether lombok (or any other lib) is on classpath
        try {
            final Class<?> aClass = getClass().forName("lombok.Getter");

            printWarning(aClass);
            printWarning("Lombok class found:D :D :D");
        } catch (final ClassNotFoundException e) {
            printWarning("Lombok class not found :( :( :(");
            e.printStackTrace();
        }

        try {
            final JavaFileObject outFile = processingEnv.getFiler().createSourceFile("sample.output.OutputClass");
            try (final Writer writer = outFile.openWriter()) {
                writer.write("package sample.output;\n" +
                        "\n" +
                        "public class OutputClass{\n" +
                        "    \n" +
                        "}");
            }
        } catch (final IOException e) {
            printError("unable to create bla: " + e);
        }

        roundEnv.getElementsAnnotatedWith(MatcherConfiguration.class).stream().forEach(this::printWarning);
        printWarning("----");
        // would be a way to find all elements within a package of the current project
        // packages outside ot the project should be found via reflections
        roundEnv.getRootElements().stream().forEach(this::printWarning);

        return false;
    }

    private <T extends Annotation> List<T> getAnnotation(final RoundEnvironment roundEnv, final Class<T> annotationType) {
        return roundEnv.getElementsAnnotatedWith(annotationType).stream()
                .map(element -> ((Element) element).getAnnotation(annotationType))
                .collect(Collectors.toList());
    }

    private PackageElement getPackageOf(final Element element) {
        return processingEnv.getElementUtils().getPackageOf(element);
    }

    private void printWarning(final Object value) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "##### -> " + value);
    }

    private void printError(final Object value) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "##### -> " + value);
    }

    private List<? extends VariableElement> getMethodParametersOf(final ExecutableElement element) {
        return element.getParameters();
    }

    private TypeMirror getMethodReturnTypeOf(final ExecutableElement element) {
        return element.getReturnType();
    }

    private boolean isMethod(final Element element) {
        return METHOD == element.getKind();
    }

}
