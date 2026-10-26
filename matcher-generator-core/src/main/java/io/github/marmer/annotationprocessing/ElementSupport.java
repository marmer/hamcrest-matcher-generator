package io.github.marmer.annotationprocessing;

import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;

/**
 * Small helpers around elements, annotation mirrors and compiler diagnostics shared by the worker and the model
 * factory.
 */
final class ElementSupport {

    private ElementSupport() {
    }

    static boolean isPublic(final Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC);
    }

    static boolean isStatic(final Element element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    /**
     * The value of the named annotation attribute, falling back to the attribute's default.
     */
    static AnnotationValue annotationValue(
        final ProcessingEnvironment processingEnv,
        final AnnotationMirror mirror,
        final String name
    ) {
        return processingEnv.getElementUtils().getElementValuesWithDefaults(mirror).entrySet().stream()
            .filter(entry -> entry.getKey().getSimpleName().contentEquals(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow();
    }

    static AnnotationValue valueAttribute(final ProcessingEnvironment processingEnv, final AnnotationMirror mirror) {
        return annotationValue(processingEnv, mirror, "value");
    }

    static void logNote(
        final ProcessingEnvironment processingEnv,
        final String message,
        final Element element,
        final AnnotationMirror annotationMirror,
        final AnnotationValue annotationValue
    ) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.NOTE, message, element, annotationMirror, annotationValue);
    }
}
