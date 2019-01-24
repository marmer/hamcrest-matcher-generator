package io.github.marmer.annotationprocessing;

@MatcherConfiguration("io.github.marmer.annotationprocessing.samples3.SamplePojo3")
public class SampleAnnotationProcessingConfigurationSingle {
    private final String wurst;

    // TODO: marmer 19.01.2019 remove me
    public SampleAnnotationProcessingConfigurationSingle(final String wurst) {
        this.wurst = wurst;
    }
}
