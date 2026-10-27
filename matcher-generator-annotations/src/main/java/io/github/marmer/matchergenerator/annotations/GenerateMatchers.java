package io.github.marmer.matchergenerator.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures the types to generate matchers for. Every backend on the processor path generates for every configured
 * type.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface GenerateMatchers {

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
    @Retention(RetentionPolicy.CLASS)
    @interface GenerationConfiguration {

        /**
         * Configuration of how packages are created for generated types.
         *
         * @return Configuration of how packages are created for generated types.
         */
        PackageConfiguration packageConfig() default @PackageConfiguration("");

        /**
         * Configuration of how packages are created for generated types.
         */
        @Retention(RetentionPolicy.CLASS)
        @interface PackageConfiguration {

            /**
             * Which package to use for the generated types. By default this value is just a prefix or base package of
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
