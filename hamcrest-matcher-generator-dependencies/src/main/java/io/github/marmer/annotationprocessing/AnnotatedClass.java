package io.github.marmer.annotationprocessing;

@MatcherConfiguration("bla bla bla")
public class AnnotatedClass {
    private final String wurst;

    // TODO: marmer 19.01.2019 remove me

    public AnnotatedClass(final String wurst) {
        this.wurst = wurst;
    }
}
