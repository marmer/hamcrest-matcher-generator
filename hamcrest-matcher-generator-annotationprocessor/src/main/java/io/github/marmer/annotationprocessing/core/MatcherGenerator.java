package io.github.marmer.annotationprocessing.core;

import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;

public interface MatcherGenerator {
    MatcherSourceDescriptor generateMatcherFor(MatcherBaseDescriptor descriptor);
}
