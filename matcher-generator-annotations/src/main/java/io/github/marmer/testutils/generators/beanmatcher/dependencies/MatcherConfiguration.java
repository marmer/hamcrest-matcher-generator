package io.github.marmer.testutils.generators.beanmatcher.dependencies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The 5.x configuration annotation, kept as a full alias of
 * {@link io.github.marmer.matchergenerator.annotations.GenerateMatchers}. It is retained in source only, so it leaves
 * no trace in compiled classes.
 *
 * @deprecated use {@link io.github.marmer.matchergenerator.annotations.GenerateMatchers}
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface MatcherConfiguration {

    /**
     * Package names and/or full qualified class names to scan for classes to generate matchers for.
     *
     * @return Package names and full qualified class names
     */
    String[] value();

    /**
     * Package names and/or full qualified class names to leave out of the types selected by {@code value}. A package
     * excludes every configured type in it, a class name excludes that type. An entry that matches no configured type
     * is reported as a warning.
     *
     * @return Package names and full qualified class names to exclude
     */
    String[] exclude() default {};

    /**
     * Configuration of how to generate something.
     *
     * @return Configuration of how to generate something.
     */
    GenerationConfiguration generation() default @GenerationConfiguration();

    /**
     * Configuration of how to generate something.
     */
    @Retention(RetentionPolicy.SOURCE)
    @interface GenerationConfiguration {

        /**
         * Configuration of how packages are created for Matchers.
         *
         * @return Configuration of how packages are created for Matchers.
         */
        PackageConfiguration packageConfig() default @PackageConfiguration("");

        /**
         * Configuration of how packages are created for Matchers.
         */
        @Retention(RetentionPolicy.SOURCE)
        @interface PackageConfiguration {

            /**
             * Which package to use for the generated types. By default this value is just a prefix or base package  of
             * generated classes.
             * <p>
             * Example Inputs and outputs for type some.pck.SomeType "some.praefix_" = "some.praefix_some.pck"
             * "some.praefix." = "some.praefix.some.pck" "" = "some.pck"
             * </p>
             *
             * @return Which package to use for the generated types
             */
            String value();
        }
    }
}
