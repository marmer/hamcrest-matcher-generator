package io.github.marmer.matchergenerator.core;

import java.util.Map;
import java.util.Optional;
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

    /**
     * Processor option. When given (as {@code -Amatchergenerator.verbose} or with the value {@code true}), every note
     * is emitted, including one per skipped property with its reason.
     */
    static final String VERBOSE_OPTION = "matchergenerator.verbose";
    private static final String KOTLIN_METADATA = "kotlin.Metadata";

    private ElementSupport() {
    }

    /**
     * Whether the annotation mirror is an instance of the annotation with the given qualified name.
     */
    static boolean isOfType(final AnnotationMirror mirror, final String qualifiedName) {
        return mirror.getAnnotationType().asElement().toString().equals(qualifiedName);
    }

    static boolean hasAnnotation(final Element element, final String qualifiedName) {
        return element.getAnnotationMirrors().stream().anyMatch(mirror -> isOfType(mirror, qualifiedName));
    }

    /**
     * Whether the given type was compiled by Kotlin. Pass the declaring type of a member.
     */
    static boolean isDeclaredInKotlin(final Element type) {
        return hasAnnotation(type, KOTLIN_METADATA);
    }

    /**
     * The mirror of {@code kotlin.Metadata} on the element, if there is one.
     */
    static Optional<AnnotationMirror> kotlinMetadata(final Element type) {
        return type.getAnnotationMirrors().stream()
            .filter(mirror -> isOfType(mirror, KOTLIN_METADATA))
            .map(AnnotationMirror.class::cast)
            .findFirst();
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
     * Emits a note at the element alone, but only when the verbose option is set.
     */
    static void logNote(final ProcessingEnvironment processingEnv, final String message, final Element element) {
        logNote(processingEnv, message, element, null, null);
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
        return options.containsKey(VERBOSE_OPTION)
            && (options.get(VERBOSE_OPTION) == null
                || Boolean.parseBoolean(options.get(VERBOSE_OPTION)));
    }
}
