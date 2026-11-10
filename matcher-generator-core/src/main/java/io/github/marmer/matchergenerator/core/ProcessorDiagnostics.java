/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Holds the texts of the messages the worker and the source writer give and places each one through
 * the {@code Messager}, at the element and annotation it names, so the user finds the annotation
 * that caused it. The worker decides which message applies and, for an unresolvable name, its kind.
 * Apart from verbose notes, this is where the worker path calls the {@code Messager}: verbose notes
 * go through {@link ElementSupport#logNote}, which {@link TypeModelFactory} uses as well.
 */
final class ProcessorDiagnostics {

    private final ProcessingEnvironment processingEnv;

    ProcessorDiagnostics(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /** Matching has started. A note that only matters in verbose mode. */
    void matcherGenerationStarted() {
        ElementSupport.logNote(processingEnv, "Matcher generation started", null);
    }

    void bothConfigurationAnnotations(final Element configurationElement) {
        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.ERROR,
                        "Use either @GenerateMatchers or @MatcherConfiguration on a type, not both",
                        configurationElement);
    }

    void unmatchedExclusion(
            final String name,
            final ConfigurationSite configuration,
            final AnnotationValue exclusion) {
        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.WARNING,
                        "Exclusion '" + name + "' matches no configured type",
                        configuration.element(),
                        configuration.mirror(),
                        exclusion);
    }

    /**
     * A configured name that is neither a type nor a package, reported with the kind the worker
     * picks.
     */
    void unresolvableName(
            final Diagnostic.Kind kind,
            final String qualifiedTypeOrPackageName,
            final ConfigurationSite configuration,
            final AnnotationValue configuredValue) {
        processingEnv
                .getMessager()
                .printMessage(
                        kind,
                        "Neither a type nor a package exists for '"
                                + qualifiedTypeOrPackageName
                                + "'",
                        configuration.element(),
                        configuration.mirror(),
                        configuredValue);
    }

    /** A type the configuration names has no properties to match. */
    void typeWithoutProperties(final String qualifiedName, final TypeElement configuredType) {
        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.WARNING,
                        typeNamed(qualifiedName) + " has no properties to match",
                        configuredType);
    }

    /** A type a package entry picks up has no properties to match. A verbose note only. */
    void typePickedUpWithoutProperties(
            final String qualifiedName, final TypeElement configuredType) {
        ElementSupport.logNote(
                processingEnv,
                typeNamed(qualifiedName)
                        + " picked up by a package entry has no properties to match",
                configuredType);
    }

    /**
     * A type that is not public is not generated into another package, which is the same for a
     * nested type and a top level type. A verbose note at the declaration of the type, where the
     * note for a nested type has always been.
     */
    void nonPublicTypeSkipped(final TypeElement type) {
        ElementSupport.logNote(
                processingEnv,
                "Matcher generation skipped for non public type: " + type.getQualifiedName(),
                type);
    }

    /**
     * A configuration names a non public top level type, or one of its nested types, and a package
     * prefix moves it into another package. One error at the configured value that names it.
     */
    void nonPublicTypeNamed(
            final TypeElement type,
            final String targetPackage,
            final ConfigurationSite configuration,
            final AnnotationValue configuredValue) {
        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.ERROR,
                        typeNamed(type.getQualifiedName())
                                + " is not public and cannot be used from the target package '"
                                + targetPackage
                                + "'",
                        configuration.element(),
                        configuration.mirror(),
                        configuredValue);
    }

    /** The message form of a type name: the word Type and the name in single quotes. */
    private static String typeNamed(final CharSequence qualifiedName) {
        return "Type '" + qualifiedName + "'";
    }

    /** The type is already generated by this processor, so it is not generated again. */
    void selfGenerationSkipped(final TypeElement typeElement, final AnnotationMirror mirror) {
        ElementSupport.logNote(
                processingEnv,
                "Generation skipped for: '"
                        + typeElement.getQualifiedName()
                        + "' because it is already generated by this processor",
                typeElement,
                mirror,
                ElementSupport.valueAttribute(processingEnv, mirror));
    }

    /** A backend found a problem in a configured type. */
    void backendError(final ConfigurationSite configuration, final String message) {
        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.ERROR,
                        message,
                        configuration.element(),
                        configuration.mirror());
    }

    /** A backend reports something it left out of the code it generates. */
    void backendWarning(final ConfigurationSite configuration, final String message) {
        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.WARNING,
                        message,
                        configuration.element(),
                        configuration.mirror());
    }

    /** A backend has something to tell that is no problem. It is a note without an element. */
    void backendNote(final String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    /** A backend has something to tell that only matters in verbose mode. */
    void backendVerboseNote(final String message) {
        ElementSupport.logNote(processingEnv, message, null);
    }

    /**
     * A source an earlier round wrote is not written again. It names the types of this round in its
     * package that it does not know.
     */
    void sourceNotWrittenAgain(
            final ConfigurationSite configuration, final String name, final String missingTypes) {
        backendWarning(
                configuration,
                name
                        + " was already written in an earlier processing round and is not written"
                        + " again. It does not know the types generated in this round: "
                        + missingTypes);
    }

    /**
     * A Kotlin source is generated, but the processor runs without KAPT, which has no output for
     * it.
     */
    void kotlinWithoutKaptOption(final GeneratorBackend backend, final TypeElement typeElement) {
        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.ERROR,
                        "Backend "
                                + backend.getClass().getName()
                                + " generated Kotlin for "
                                + typeElement.getQualifiedName()
                                + ", which is only compiled when the annotation processor runs"
                                + " with KAPT (processor option '"
                                + GeneratedSourceWriter.KAPT_KOTLIN_GENERATED
                                + "' is missing)",
                        typeElement);
    }
}
