package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.core.Logger;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class MessagerLogger implements Logger {
    private final Messager messager;

    MessagerLogger(final Messager messager) {
        this.messager = messager;
    }

    @Override
    public void error(final String message) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, message);

    }

    @Override
    public void info(final String message, final Element element) {
        messager.printMessage(Diagnostic.Kind.NOTE, message, element);
    }

    @Override
    public void info(final String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }
}
