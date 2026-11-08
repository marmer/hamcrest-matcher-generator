/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * A processing environment without a javac round. It offers a Filer that remembers the sources
 * written and refuses a second source of a name, as javac does, and a Messager that remembers the
 * diagnostics. Nothing else is supported.
 */
final class FakeProcessingEnvironment implements ProcessingEnvironment {

    private final Map<String, String> options;
    private final RecordingFiler filer = new RecordingFiler();
    private final RecordingMessager messager = new RecordingMessager();

    FakeProcessingEnvironment(final Map<String, String> options) {
        this.options = options;
    }

    RecordingFiler filer() {
        return filer;
    }

    RecordingMessager messager() {
        return messager;
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public Messager getMessager() {
        return messager;
    }

    @Override
    public Filer getFiler() {
        return filer;
    }

    @Override
    public Elements getElementUtils() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Types getTypeUtils() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SourceVersion getSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Locale getLocale() {
        return Locale.ROOT;
    }

    /** Remembers each source by its name; a second source of a name is refused like javac does. */
    static final class RecordingFiler implements Filer {

        private final Map<String, ByteArrayOutputStream> sources = new LinkedHashMap<>();

        List<String> createdSourceNames() {
            return new ArrayList<>(sources.keySet());
        }

        String contentOf(final String name) {
            return Objects.requireNonNull(sources.get(name)).toString(StandardCharsets.UTF_8);
        }

        @Override
        public JavaFileObject createSourceFile(
                final CharSequence name, final Element... originatingElements) throws IOException {
            final String qualifiedName = name.toString();
            if (sources.containsKey(qualifiedName)) {
                throw new FilerException("Attempt to recreate a file for type " + qualifiedName);
            }
            final ByteArrayOutputStream content = new ByteArrayOutputStream();
            sources.put(qualifiedName, content);
            return new SimpleJavaFileObject(
                    URI.create("string:///" + qualifiedName.replace('.', '/') + ".java"),
                    JavaFileObject.Kind.SOURCE) {
                @Override
                public OutputStream openOutputStream() {
                    return content;
                }
            };
        }

        @Override
        public JavaFileObject createClassFile(
                final CharSequence name, final Element... originatingElements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject createResource(
                final JavaFileManager.Location location,
                final CharSequence moduleAndPkg,
                final CharSequence relativeName,
                final Element... originatingElements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject getResource(
                final JavaFileManager.Location location,
                final CharSequence moduleAndPkg,
                final CharSequence relativeName)
                throws IOException {
            throw new IOException("No resource " + relativeName);
        }
    }

    /** Remembers the diagnostics as kind and message. */
    static final class RecordingMessager implements Messager {

        private final List<String> diagnostics = new ArrayList<>();

        /** Each diagnostic as its kind, a space and its message. */
        List<String> diagnostics() {
            return diagnostics;
        }

        @Override
        public void printMessage(final Kind kind, final CharSequence msg) {
            diagnostics.add(kind + " " + msg);
        }

        @Override
        public void printMessage(final Kind kind, final CharSequence msg, final Element e) {
            printMessage(kind, msg);
        }

        @Override
        public void printMessage(
                final Kind kind,
                final CharSequence msg,
                final Element e,
                final AnnotationMirror a) {
            printMessage(kind, msg);
        }

        @Override
        public void printMessage(
                final Kind kind,
                final CharSequence msg,
                final Element e,
                final AnnotationMirror a,
                final AnnotationValue v) {
            printMessage(kind, msg);
        }
    }
}
