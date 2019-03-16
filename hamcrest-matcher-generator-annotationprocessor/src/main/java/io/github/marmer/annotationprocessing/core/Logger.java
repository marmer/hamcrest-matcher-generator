package io.github.marmer.annotationprocessing.core;

import javax.lang.model.element.Element;

public interface Logger {
    void fatal(String message);

    void error(String message);

    void info(String s);

    void info(String s, final Element element);
}
