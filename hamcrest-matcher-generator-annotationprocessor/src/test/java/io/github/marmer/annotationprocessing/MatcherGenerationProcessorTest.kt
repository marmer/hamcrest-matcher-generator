package io.github.marmer.annotationprocessing

import io.github.marmer.annotationprocessing.core.MatcherGenerator
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor
import io.github.marmer.annotationprocessing.creation.SourceWriter
import io.github.marmer.annotationprocessing.extraction.MatcherBaseDescriptorFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.stream.Stream
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element

@ExtendWith(MockitoExtension::class)
internal class MatcherGenerationProcessorTest {
    @InjectMocks
    private lateinit var underTest: MatcherGenerationProcessor

    @Mock
    private lateinit var matcherBaseDescriptorFactory: MatcherBaseDescriptorFactory

    @Mock
    private lateinit var matcherGenerator: MatcherGenerator

    @Mock
    private lateinit var sourceWriter: SourceWriter

    @Mock
    private lateinit var logger: MessagerLogger

    @Test
    @DisplayName("All types for configured packages of single configuration should be processed and saved")
    fun testProcess_AllTypesForConfiguredPackagesOfSingleConfigurationShouldBeProcessedAndSaved() {
        // Preparation
        val roundEnv = mock(RoundEnvironment::class.java)
        val matcherConfiguration = mock(MatcherConfiguration::class.java)
        val matcherConfigurationElement = mock(
            Element::class.java
        )
        val matcherConfigurationElements = setOf(matcherConfigurationElement)
        val matcherBaseDescriptor = MatcherBaseDescriptor.builder().build()
        val matcherSourceDescriptor = MatcherSourceDescriptor.builder().build()
        `when`(roundEnv.processingOver()).thenReturn(false)
        `when`(matcherConfigurationElement.getAnnotation(MatcherConfiguration::class.java))
            .thenReturn(matcherConfiguration)
        doReturn(emptySet<Any>()).`when`(roundEnv).getElementsAnnotatedWith(
            MatcherConfigurations::class.java
        )
        doReturn(matcherConfigurationElements).`when`(roundEnv).getElementsAnnotatedWith(
            MatcherConfiguration::class.java
        )
        `when`(matcherBaseDescriptorFactory.create(matcherConfiguration))
            .thenReturn(Stream.of(matcherBaseDescriptor))
        `when`(matcherGenerator.generateMatcherFor(matcherBaseDescriptor)).thenReturn(matcherSourceDescriptor)

        // Execution
        val result = underTest.process(emptySet(), roundEnv)

        // Assertion
        verify(sourceWriter).create(matcherSourceDescriptor)
        assertThat(result, `is`(false))
    }

    @Test
    @DisplayName("Generation should be successfull for not exceptional parts if some error occurs on sourcecode generation")
    fun testProcess_GenerationShouldBeSuccessfullForNotExceptionalPartsIfSomeErrorOccursOnSourcecodeGeneration() {
        // Preparation
        val roundEnv = mock(RoundEnvironment::class.java)
        val matcherConfiguration = mock(MatcherConfiguration::class.java)
        val matcherConfigurationElement = mock(
            Element::class.java
        )
        val matcherConfigurationElements: Set<*> = setOf(matcherConfigurationElement)
        val matcherBaseDescriptor = MatcherBaseDescriptor.builder().build()
        val erroneousMatcherBaseDescriptor = "some.Type".newMatcherBaseDescriptorForType()
            .build()
        val matcherSourceDescriptor = MatcherSourceDescriptor.builder().build()
        `when`(roundEnv.processingOver()).thenReturn(false)
        `when`(matcherConfigurationElement.getAnnotation(MatcherConfiguration::class.java))
            .thenReturn(matcherConfiguration)
        doReturn(emptySet<Any>()).`when`(roundEnv).getElementsAnnotatedWith(
            MatcherConfigurations::class.java
        )
        doReturn(matcherConfigurationElements).`when`(roundEnv).getElementsAnnotatedWith(
            MatcherConfiguration::class.java
        )
        val cause = RuntimeException("some error message")
        `when`(matcherBaseDescriptorFactory.create(matcherConfiguration))
            .thenReturn(Stream.of(matcherBaseDescriptor, erroneousMatcherBaseDescriptor))
        doReturn(matcherSourceDescriptor).`when`(matcherGenerator)
            .generateMatcherFor(ArgumentMatchers.same(matcherBaseDescriptor))
        doThrow(cause).`when`(matcherGenerator)
            .generateMatcherFor(ArgumentMatchers.same(erroneousMatcherBaseDescriptor))

        // Execution
        val result = underTest.process(emptySet(), roundEnv)

        // Assertion
        verify(sourceWriter).create(matcherSourceDescriptor)
        verify(logger).error(
            "Hamcrest matcher generation stopped for 'some.Type' because of an unexpected error: some error message"
        )
        assertThat(result, `is`(false))
    }

    private fun String.newMatcherBaseDescriptorForType(): MatcherBaseDescriptor.MatcherBaseDescriptorBuilder {
        return MatcherBaseDescriptor.builder()
            .base(TypeDescriptor.builder().fullQualifiedName(this).build())
    }

    @Test
    @DisplayName("Processing is allready over should return immediately")
    fun testProcess_ProcessingIsAllreadyOverShouldReturnImmediately() {
        // Preparation
        val roundEnv = mock(RoundEnvironment::class.java)

        // Execution
        val result = underTest.process(emptySet(), roundEnv)

        // Assertion
        assertThat(result, `is`(false))
    }
}
