package io.github.marmer.annotationprocessing.extraction;

import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.MatcherConfigurations;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        // TODO: marmer 01.02.2019 Type does not exist -> warn
        return Stream.of(configurations.value())
                .map(t -> processingEnv.getElementUtils().getTypeElement(t))
                .map(typeElement -> typeDescriptorFor(processingEnv, typeElement))
                .collect(Collectors.toSet());
    }

    private MatcherBaseDescriptor typeDescriptorFor(final ProcessingEnvironment processingEnv, final TypeElement type) {
        return MatcherBaseDescriptor.builder()
                .base(TypeDescriptor.builder()
                        .packageName(processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString())
                        .typeName(type.getSimpleName().toString()).build()).build();
    }


}
