package io.github.marmer.annotationprocessing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface MatcherConfiguration {
    /**
     * Package names and full qualified class names to scan for classes to generate matchers for.
     *
     * @return Package names and full qualified class names
     */
    String[] value();
}