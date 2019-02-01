package io.github.marmer.annotationprocessing.extraction;

import com.google.auto.common.AnnotationValues;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.MatcherConfigurations;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Factory to create some matcher descriptions.
 */
public class MatcherBaseDescriptorfFactory {
    /**
     * Creates Matcher descriptions.
     *
     * @param configurationsWrapper Configuration for what to create {@link MatcherBaseDescriptor}s for.
     * @param processingEnv         environment used to find details based on the configurations.
     * @return Resulting {@link MatcherBaseDescriptor}s based on the configurations.
     */
    public Set<MatcherBaseDescriptor> create(final MatcherConfigurations configurationsWrapper, final ProcessingEnvironment processingEnv) {
        return Stream.of(configurationsWrapper.value())
                .map(matcherConfiguration -> create(matcherConfiguration, processingEnv))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }


    /**
     * Creates Matcher descriptions.
     *
     * @param configurations Configuration for what to create {@link MatcherBaseDescriptor}s for.
     * @param processingEnv  environment used to find details based on the configurations.
     * @return Resulting {@link MatcherBaseDescriptor}s based on the configurations.
     */
    public Set<MatcherBaseDescriptor> create(final MatcherConfiguration configurations, final ProcessingEnvironment processingEnv) {
        final String[] value = configurations.value();
        final Set<MatcherBaseDescriptor> result = new HashSet<>();
        final TypeElement type = processingEnv.getElementUtils().getTypeElement(value[0]);
        // TODO: marmer 01.02.2019 Type does not exist -> warn
        AnnotationValues.


        return result;
    }


}
