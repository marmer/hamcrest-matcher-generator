package io.github.marmer.matchergenerator.core;

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

    /**
     * Emits a note, but only when the verbose option is set. Without an annotation mirror the note is placed at the
     * element alone.
     */
    static void logNote(
        final ProcessingEnvironment processingEnv,
        final String message,
        final Element element,
        final AnnotationMirror annotationMirror,
        final AnnotationValue annotationValue
    ) {
        if (!isVerbose(processingEnv)) {
            return;
        }
        if (annotationMirror == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
        } else {
            processingEnv.getMessager()
                .printMessage(Diagnostic.Kind.NOTE, message, element, annotationMirror, annotationValue);
        }
    }

    static boolean isVerbose(final ProcessingEnvironment processingEnv) {
        final Map<String, String> options = processingEnv.getOptions();
        return options.containsKey(MatcherGenerationProcessorWorker.VERBOSE_OPTION)
            && (options.get(MatcherGenerationProcessorWorker.VERBOSE_OPTION) == null
                || Boolean.parseBoolean(options.get(MatcherGenerationProcessorWorker.VERBOSE_OPTION)));
    }
}
