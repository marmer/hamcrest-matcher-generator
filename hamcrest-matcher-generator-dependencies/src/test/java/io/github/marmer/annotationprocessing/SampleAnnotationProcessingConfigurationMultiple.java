package io.github.marmer.annotationprocessing;

@MatcherConfigurations({
        @MatcherConfiguration("io.github.marmer.annotationprocessing.samples"),
        @MatcherConfiguration("io.github.marmer.annotationprocessing.samples2.SamplePojo2")})
public class SampleAnnotationProcessingConfigurationMultiple {
    private final String wurst;

    // TODO: marmer 19.01.2019 remove me
    public SampleAnnotationProcessingConfigurationMultiple(final String wurst) {
        this.wurst = wurst;
    }
}
