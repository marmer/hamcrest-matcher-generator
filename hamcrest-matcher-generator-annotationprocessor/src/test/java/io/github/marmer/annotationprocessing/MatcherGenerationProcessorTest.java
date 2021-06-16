package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;
import io.github.marmer.annotationprocessing.creation.SourceWriter;
import io.github.marmer.annotationprocessing.extraction.MatcherBaseDescriptorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatcherGenerationProcessorTest {
    @InjectMocks
    private MatcherGenerationProcessor underTest;

    @Mock
    private MatcherBaseDescriptorFactory matcherBaseDescriptorFactory;
    @Mock
    private MatcherGenerator matcherGenerator;
    @Mock
    private SourceWriter sourceWriter;
    @Mock
    private MessagerLogger logger;

    @Test
    @DisplayName("All types for configured packages of single configuration should be processed and saved")
    void testProcess_AllTypesForConfiguredPackagesOfSingleConfigurationShouldBeProcessedAndSaved()
            throws Exception {
        // Preparation
        final RoundEnvironment roundEnv = Mockito.mock(RoundEnvironment.class);
        final MatcherConfiguration matcherConfiguration = mock(MatcherConfiguration.class);
        final Element matcherConfigurationElement = mock(Element.class);
        final Set matcherConfigurationElements = singleton(matcherConfigurationElement);
        final MatcherBaseDescriptor matcherBaseDescriptor = MatcherBaseDescriptor.builder().build();
        final MatcherSourceDescriptor matcherSourceDescriptor = MatcherSourceDescriptor.builder().build();

        when(roundEnv.processingOver()).thenReturn(false);
        when(matcherConfigurationElement.getAnnotation(MatcherConfiguration.class)).thenReturn(matcherConfiguration);
        doReturn(emptySet()).when(roundEnv).getElementsAnnotatedWith(MatcherConfigurations.class);
        doReturn(matcherConfigurationElements).when(roundEnv).getElementsAnnotatedWith(MatcherConfiguration.class);

        when(matcherBaseDescriptorFactory.create(matcherConfiguration)).thenReturn(Stream.of(matcherBaseDescriptor));
        when(matcherGenerator.generateMatcherFor(matcherBaseDescriptor)).thenReturn(matcherSourceDescriptor);

        // Execution
        final var result = underTest.process(Collections.emptySet(), roundEnv);

        // Assertion
        verify(sourceWriter).create(matcherSourceDescriptor);
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Generation should be successfull for not exceptional parts if some error occurs on sourcecode generation")
    void testProcess_GenerationShouldBeSuccessfullForNotExceptionalPartsIfSomeErrorOccursOnSourcecodeGeneration()
            throws Exception {
        // Preparation
        final RoundEnvironment roundEnv = Mockito.mock(RoundEnvironment.class);
        final MatcherConfiguration matcherConfiguration = mock(MatcherConfiguration.class);
        final Element matcherConfigurationElement = mock(Element.class);
        final Set matcherConfigurationElements = singleton(matcherConfigurationElement);
        final MatcherBaseDescriptor matcherBaseDescriptor = MatcherBaseDescriptor.builder().build();
        final MatcherBaseDescriptor erroneousMatcherBaseDescriptor = newMatcherBaseDescriptorForType("some.Type").build();
        final MatcherSourceDescriptor matcherSourceDescriptor = MatcherSourceDescriptor.builder().build();

        when(roundEnv.processingOver()).thenReturn(false);
        when(matcherConfigurationElement.getAnnotation(MatcherConfiguration.class)).thenReturn(matcherConfiguration);
        doReturn(emptySet()).when(roundEnv).getElementsAnnotatedWith(MatcherConfigurations.class);
        doReturn(matcherConfigurationElements).when(roundEnv).getElementsAnnotatedWith(MatcherConfiguration.class);

        final RuntimeException cause = new RuntimeException("some error message");

        when(matcherBaseDescriptorFactory.create(matcherConfiguration)).thenReturn(Stream.of(matcherBaseDescriptor, erroneousMatcherBaseDescriptor));
        doReturn(matcherSourceDescriptor).when(matcherGenerator).generateMatcherFor(same(matcherBaseDescriptor));
        doThrow(cause).when(matcherGenerator).generateMatcherFor(same(erroneousMatcherBaseDescriptor));

        // Execution
        final var result = underTest.process(Collections.emptySet(), roundEnv);

        // Assertion
        verify(sourceWriter).create(matcherSourceDescriptor);
        verify(logger).error("Hamcrest matcher generation stopped for 'some.Type' because of an unexpected error: some error message");
        assertThat(result, is(false));
    }

    private MatcherBaseDescriptor.MatcherBaseDescriptorBuilder newMatcherBaseDescriptorForType(final String fullQualifiedName) {
        return MatcherBaseDescriptor.builder().base(TypeDescriptor.builder().fullQualifiedName(fullQualifiedName).build());
    }

    @Test
    @DisplayName("Processing is allready over should return immediately")
    void testProcess_ProcessingIsAllreadyOverShouldReturnImmediately()
            throws Exception {
        // Preparation
        final RoundEnvironment roundEnv = mock(RoundEnvironment.class);

        // Execution
        final var result = underTest.process(emptySet(), roundEnv);

        // Assertion
        assertThat(result, is(false));
    }
}
