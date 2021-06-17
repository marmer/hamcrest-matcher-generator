package io.github.marmer.annotationprocessing

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MatcherConfiguration(
    /**
     * Package names and/or full qualified class names to scan for classes to generate matchers for.
     *
     * @return Package names and full qualified class names
     */
    vararg val value: String,
    /**
     * Configuration of how to generate something.
     *
     * @return Configuration of how to generate something.
     */
    val generation: GenerationConfiguration = GenerationConfiguration()
) {
    /**
     * Configuration of how to generate something.
     */
    @Retention(AnnotationRetention.SOURCE)
    annotation class GenerationConfiguration(
        /**
         * Configuration of how packages are created for Matchers.
         *
         * @return Configuration of how packages are created for Matchers.
         */
        val packageConfig: PackageConfiguration = PackageConfiguration("")
    ) {
        /**
         * Configuration of how packages are created for Matchers.
         */
        @Retention(AnnotationRetention.SOURCE)
        annotation class PackageConfiguration(
            /**
             * Which package to use for the generated types. By default this value is just a prefix or base package  of generated classes.
             *
             *
             * Example Inputs and outputs for type some.pck.SomeType
             * "some.praefix_" = "some.praefix_some.pck"
             * "some.praefix." = "some.praefix.some.pck"
             * "" = "some.pck"
             *
             *
             * @return Which package to use for the generated types
             */
            val value: String
        )
    }
}
