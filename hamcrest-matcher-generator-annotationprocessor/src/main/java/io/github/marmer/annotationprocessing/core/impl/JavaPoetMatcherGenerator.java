package io.github.marmer.annotationprocessing.core.impl;

import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;

public class JavaPoetMatcherGenerator implements MatcherGenerator {
    @Override
    public MatcherSourceDescriptor generateMatcherFor(final MatcherBaseDescriptor descriptor) {

        // TODO: marmer 31.01.2019 implement me
        return MatcherSourceDescriptor.builder().build();
    }
}
