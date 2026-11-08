/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.jspecify.annotations.Nullable;

/** Real type mirrors for tests that build a model by hand. */
final class TypeMirrors {

    private TypeMirrors() {}

    /**
     * A real type mirror, since the generator inspects the kind and the element of the types it is
     * given.
     */
    static TypeMirror stringType() {
        return typeNamed("java.lang.String");
    }

    /** A real declared type mirror of the named class, which has to be on the class path. */
    static TypeMirror typeNamed(final String qualifiedName) {
        return inCompilation(environment -> typeElement(environment, qualifiedName).asType());
    }

    /** A real {@code java.util.List} of the named class, which has to be on the class path. */
    static TypeMirror listOf(final String elementName) {
        return declared(
                "java.util.List", environment -> List.of(elementType(environment, elementName)));
    }

    /** A real {@code java.util.List} of an unknown subtype of the named class. */
    static TypeMirror listOfExtending(final String upperBoundName) {
        return declared(
                "java.util.List",
                environment ->
                        List.of(
                                environment
                                        .getTypeUtils()
                                        .getWildcardType(
                                                elementType(environment, upperBoundName), null)));
    }

    /**
     * A real {@code java.util.Collection} of the named class, which has to be on the class path.
     */
    static TypeMirror collectionOf(final String elementName) {
        return declared(
                "java.util.Collection",
                environment -> List.of(elementType(environment, elementName)));
    }

    /** A real {@code java.util.Map} of the named classes, which have to be on the class path. */
    static TypeMirror mapOf(final String keyName, final String valueName) {
        return declared(
                "java.util.Map",
                environment ->
                        List.of(
                                elementType(environment, keyName),
                                elementType(environment, valueName)));
    }

    /** A real array type of the named class, which has to be on the class path. */
    static TypeMirror arrayOf(final String elementName) {
        return inCompilation(
                environment ->
                        environment
                                .getTypeUtils()
                                .getArrayType(elementType(environment, elementName)));
    }

    /**
     * A real declared type of the named generic class with the given type arguments, made inside
     * the same compilation as the arguments.
     */
    private static TypeMirror declared(
            final String rawName,
            final Function<ProcessingEnvironment, List<TypeMirror>> arguments) {
        return inCompilation(
                environment ->
                        environment
                                .getTypeUtils()
                                .getDeclaredType(
                                        typeElement(environment, rawName),
                                        arguments.apply(environment).toArray(TypeMirror[]::new)));
    }

    private static TypeMirror elementType(
            final ProcessingEnvironment environment, final String qualifiedName) {
        return typeElement(environment, qualifiedName).asType();
    }

    /**
     * Builds a type mirror inside a compilation, since only the processing environment of one can
     * make parameterized, array or wildcard types.
     */
    private static TypeMirror inCompilation(
            final Function<ProcessingEnvironment, TypeMirror> build) {
        final AtomicReference<@Nullable TypeMirror> mirror = new AtomicReference<>();
        CompilationSubject.assertThat(
                        Compiler.javac()
                                .withProcessors(
                                        new AbstractProcessor() {
                                            @Override
                                            public Set<String> getSupportedAnnotationTypes() {
                                                return Set.of("*");
                                            }

                                            @Override
                                            public SourceVersion getSupportedSourceVersion() {
                                                return SourceVersion.latestSupported();
                                            }

                                            @Override
                                            public boolean process(
                                                    final Set<? extends TypeElement> annotations,
                                                    final RoundEnvironment round) {
                                                mirror.compareAndSet(
                                                        null, build.apply(processingEnv));
                                                return false;
                                            }
                                        })
                                .compile(
                                        JavaFileObjects.forSourceLines(
                                                "some.Empty", "package some;", "class Empty {}")))
                .succeeded();
        return Objects.requireNonNull(mirror.get());
    }

    private static TypeElement typeElement(
            final ProcessingEnvironment environment, final String qualifiedName) {
        return Objects.requireNonNull(environment.getElementUtils().getTypeElement(qualifiedName));
    }
}
