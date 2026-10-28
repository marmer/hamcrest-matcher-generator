/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.processing.AbstractProcessor;
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
        final AtomicReference<@Nullable TypeMirror> string = new AtomicReference<>();
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
                                                string.compareAndSet(
                                                        null,
                                                        processingEnv
                                                                .getElementUtils()
                                                                .getTypeElement("java.lang.String")
                                                                .asType());
                                                return false;
                                            }
                                        })
                                .compile(
                                        JavaFileObjects.forSourceLines(
                                                "some.Empty", "package some;", "class Empty {}")))
                .succeeded();
        return Objects.requireNonNull(string.get());
    }
}
