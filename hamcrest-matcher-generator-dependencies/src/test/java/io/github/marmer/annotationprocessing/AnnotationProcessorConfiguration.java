package io.github.marmer.annotationprocessing;

@MatcherConfiguration("bla bla bla")
public class AnnotationProcessorConfiguration {
    private final String wurst;

    // TODO: marmer 19.01.2019 remove me

    public AnnotationProcessorConfiguration(final String wurst) {
        this.wurst = wurst;
    }
}
