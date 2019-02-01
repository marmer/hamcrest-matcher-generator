package io.github.marmer.annotationprocessing.extraction;

import io.github.marmer.annotationprocessing.MatcherConfigurations;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;

import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * Factory to create some matcher descriptions.
 */
public class MatcherBaseDescriptorfFactory {
    /**
     * Creates Matcher descriptions.
     *
     * @param configurations Configuration for what to create {@link MatcherBaseDescriptor}s for.
     * @param processingEnv  environment used to find details based on the configurations.
     * @return Resulting {@link MatcherBaseDescriptor}s based on the configurations.
     */
    public Set<MatcherBaseDescriptor> create(final MatcherConfigurations configurations, final ProcessingEnvironment processingEnv) {
        
        // TODO: marmer 31.01.2019 implement me and replace this dummy
        return Collections.singleton(MatcherBaseDescriptor.builder()
                .base(TypeDescriptor.builder()
                        .packageName("some.other.pck")
                        .typeName("SimplePojo")
                        .build())
                .build());
    }
}
