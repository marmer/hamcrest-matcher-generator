package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.core.Logger;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class MessagerLogger implements Logger {
    private final String praefix;
    private final Messager messager;
    private Element currentElement;

    MessagerLogger(final String praefix, final Messager messager) {
        this.praefix = praefix;
        this.messager = messager;
    }

    @Override
    public void error(final String message) {
        log(Diagnostic.Kind.MANDATORY_WARNING, message, currentElement);
    }

    @Override
    public void info(final String message, final Element element) {
        log(Diagnostic.Kind.NOTE, message, element);
    }

    @Override
    public void info(final String message) {
        log(Diagnostic.Kind.NOTE, message, currentElement);
    }

    private void log(final Diagnostic.Kind level, final String message, final Element element) {
        if (element == null) {
            messager.printMessage(level, praefix + ": " + message);
        } else {
            messager.printMessage(level, praefix + ": " + message, element);
        }
    }

    public void setCurrentElement(final Element currentElement) {
        this.currentElement = currentElement;
    }

    public void reset() {
        currentElement = null;
    }
}
