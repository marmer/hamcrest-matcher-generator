package io.github.marmer.annotationprocessing;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(MatcherConfigurations.class)
public @interface MatcherConfiguration {

    /**
     * Package names and/or full qualified class names to scan for classes to generate matchers for.
     *
     * @return Package names and full qualified class names
     */
    String[] value();

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
             * Which package to use for the generated types. By default this value is just a prefix or base package  of generated classes.
             * <p>
             * Example Inputs and outputs for type some.pck.SomeType
             * "some.praefix_" => "some.praefix_some.pck"
             * "some.praefix." => "some.praefix.some.pck"
             * "" => "some.pck"
             * </p>
             *
             * @return Which package to use for the generated types
             */
            String value();
        }
    }
}