package io.github.marmer.annotationprocessing.core.model;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Getter;

public class Testdatagenerator {
    @Getter
    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(getClass().getName().hashCode())
            .build();

    public MatcherBaseDescriptor.MatcherBaseDescriptorBuilder newMatcherBaseDescriptor() {
        return random.nextObject(MatcherBaseDescriptor.MatcherBaseDescriptorBuilder.class);
    }

    public MatcherSourceDescriptor.MatcherSourceDescriptorBuilder newMatcherSourceDescriptor() {
        return random.nextObject(MatcherSourceDescriptor.MatcherSourceDescriptorBuilder.class);
    }
}
