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
     * @return The matchers package name
     */
    Optional<String> packageFor(Class<?> type);

    /**
     * Generates a name for a matcher to the given type.
     *
     * @param type Type to generate the name for the Matcher for.
     * @return Tha matcher name.
     */
    Optional<String> typeNameFor(Class<?> type);

    /**
     * Strategy of how to name generated matchers.
     */
    enum Name {
        /**
         * The matcher is named exactly as the class it is generate for with exactly the package it lies in.
         * <p>
         * This strategy can be used if no matchers are generated for inner classes or if there is always only one
         * inner class in a package with a name.
         * If you have two classes with each an inner class an both inner classes are named the same, only one wins.
         * </p>
         * <p>
         * E.g. This constellation
         * some.package.OuterClass1.InnerClass
         * some.package.OuterClass2.InnerClass
         * </p>
         * <p>
         * ...would produce the following Matchers:
         * some.package.OuterClass1Matcher
         * some.package.OuterClass2Matcher
         * some.package.InnerClassMatcher
         *
         * You don't know what the InnerClassMatcher is for.
         * </p>
         */
        PLAIN,
        /**
         * The matcher is named exactly as the class it is generated for but added to a package which looks pretty much
         * like the full qualified classname it was generated for.
         * <p>
         * E.g. This constellation
         * some.package.OuterClass1.InnerClass
         * some.package.OuterClass2.InnerClass
         * </p>
         * <p>
         * ...would produce the following machers
         * </p>
         * <p>
         * some.package.OuterClass1Matcher
         * some.package.outerclass1.InnerClassMatcher
         * some.package.OuterClass2Matcher
         * some.package.outerclass2.InnerClassMatcher
         * </p>
         */
        PACKAGE,
        /**
         * The generated matcher contains the typename of the enclosing type(s) and lies in their package.
         * <p>
         * E.g. This constellation
         * some.package.OuterClass1.InnerClass
         * some.package.OuterClass2.InnerClass.InnerInnerClass
         * </p>
         * <p>
         * ...would produce the following machers
         * </p>
         * <p>
         * some.package.OuterClass1Matcher
         * some.package.OuterClass1$InnerClassMatcher
         * some.package.OuterClass2Matcher
         * some.package.OuterClass2$InnerClassMatcher
         * some.package.OuterClass2$InnerClass$InnerInnerClassMatcher
         * </p>
         */
        // TODO: marmer 04.11.2018 comming soon
        PARENT
    }
}
