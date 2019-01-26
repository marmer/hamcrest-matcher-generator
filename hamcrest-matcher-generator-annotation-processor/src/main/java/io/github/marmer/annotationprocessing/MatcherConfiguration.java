package io.github.marmer.annotationprocessing;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(MatcherConfigurations.class)
public @interface MatcherConfiguration {
    /**
     * Package names and full qualified class names to scan for classes to generate matchers for.
     *
     * @return Package names and full qualified class names
     */
    String[] value();
}