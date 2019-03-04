package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.core.Logger;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

public class MessagerLogger implements Logger {
    private final Messager messager;

    public MessagerLogger(final Messager messager) {
        this.messager = messager;
    }

    @Override
    public void fatal(final String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

    @Override
    public void error(final String message) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, message);

    }

    @Override
    public void warn(final String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, message);

    }

    @Override
    public void info(final String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);

    }
}
