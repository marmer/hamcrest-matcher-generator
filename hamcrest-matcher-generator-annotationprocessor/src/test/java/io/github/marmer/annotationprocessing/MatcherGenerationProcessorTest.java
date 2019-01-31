package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.core.model.Testdatagenerator;
import io.github.marmer.annotationprocessing.creation.SourceWriter;
import io.github.marmer.annotationprocessing.extraction.MatcherBaseDescriptorfFactory;
import lombok.var;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatcherGenerationProcessorTest {
    @RegisterExtension
    private final Testdatagenerator testdatagenerator = new Testdatagenerator();

    @InjectMocks
    private MatcherGenerationProcessor underTest;
    @Mock
    private ProcessingEnvironment processingEnv;
    @Mock
    private MatcherBaseDescriptorfFactory matcherBaseDescriptorfFactory;
    @Mock
    private MatcherGenerator matcherGenerator;
    @Mock
    private SourceWriter sourceWriter;

    @BeforeEach
    void setUp() {
        underTest.init(processingEnv);
    }

    @Test
    @DisplayName("Generation should happen for source code class configuration")
    void testProcess_GenerationShouldHappenForSourceCodeClassConfiguration()
            throws Exception {
        // Preparation
        final RoundEnvironment roundEnv = mock(RoundEnvironment.class);
        final Element configurationClassElement = mock(Element.class);
        final MatcherConfigurations configurations = mock(MatcherConfigurations.class);
        when(configurationClassElement.getAnnotation(MatcherConfigurations.class)).thenReturn(configurations);

        doReturn(singleton(configurationClassElement)).when(roundEnv).getElementsAnnotatedWith(MatcherConfigurations.class);
        final MatcherBaseDescriptor descriptor = testdatagenerator.newMatcherBaseDescriptor().build();
        when(matcherBaseDescriptorfFactory.create(configurations, processingEnv)).thenReturn(singleton(descriptor));

        final MatcherSourceDescriptor matcherSourceDescriptor = testdatagenerator.newMatcherSourceDescriptor().build();
        when(matcherGenerator.generateMatcherFor(descriptor)).thenReturn(matcherSourceDescriptor);
        final Filer filer = mock(Filer.class);
        when(processingEnv.getFiler()).thenReturn(filer);

        // Execution
        final var result = underTest.process(Collections.emptySet(), roundEnv);

        // Assertion
        Assert.assertThat(result, is(true));
        verify(sourceWriter).create(filer, matcherSourceDescriptor);
    }

}