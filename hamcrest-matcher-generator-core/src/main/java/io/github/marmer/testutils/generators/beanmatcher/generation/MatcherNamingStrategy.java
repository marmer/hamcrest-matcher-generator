package io.github.marmer.testutils.generators.beanmatcher.generation;

import java.util.Optional;

/**
 * Naming strategy for Matchers.
 */
public interface MatcherNamingStrategy {
    /**
     * Generates the package name for a matcher to the given type.
     *
     * @param type Type to generate the package for the Matcher for.
     * @return The package name
     */
    Optional<String> packageFor(Class<?> type);

    /**
     * Strategy of how to name generated matchers.
     */
    enum Name {
        /**
         * The matcher is named exactly as the class it is generate for with exactly the package it lies in.
         * <p>
         * This strategy can be used if no matchers are generated for inner classes or if there is always only one inner class in a package with a name.
         * If you have two classes with each an inner class an both inner classes are named the same, only one wins.
         * <p>
         * E.g. This constellation
         * OuterClass1.InnerClass
         * OuterClass2.InnerClass
         * <p>
         * ...would produce OuterClass1Matcher, OuterClass2Matcher and InnerClassMatcher but you never know what the InnerClassMatcher is for.
         */
        PLAIN
    }
}
