package io.github.marmer.annotationprocessing.creation

import io.github.marmer.annotationprocessing.core.Logger
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import javax.annotation.processing.Filer
import javax.tools.JavaFileObject

@ExtendWith(MockitoExtension::class)
internal class SourceWriterTest {
    @InjectMocks
    private lateinit var underTest: SourceWriter

    @Mock
    private lateinit var filer: Filer

    @Mock
    private lateinit var logger: Logger

    @Test
    @DisplayName("Given matcher source should be written to file")
    fun testCreate_GivenMatcherSourceShouldBeWrittenToFile() {
        // Preparation
        val sourceDescriptor = "some.Type".toMatcherSourceDescriptor()
            .withSource("some content")
        val javaFileObject = Mockito.mock(JavaFileObject::class.java)
        Mockito.`when`(filer.createSourceFile("some.Type")).thenReturn(javaFileObject)
        val writer: Writer = StringWriter()
        Mockito.`when`(javaFileObject.openWriter()).thenReturn(writer)

        // Execution
        underTest.create(sourceDescriptor)

        // Assertion
        MatcherAssert.assertThat(writer.toString(), Matchers.`is`("some content"))
    }

    @Test
    @DisplayName("Errors on file creation should be logged")
    fun testCreate_ErrorsOnFileCreationShouldBeLogged() {
        // Preparation
        val sourceDescriptor = "some.Type".toMatcherSourceDescriptor()
        Mockito.`when`(filer.createSourceFile("some.Type")).thenThrow(IOException("the error message"))

        // Execution
        underTest.create(sourceDescriptor)

        // Assertion
        verify(logger).error("Cannot write file for: 'some.Type' Message: the error message")
    }

    private fun String.toMatcherSourceDescriptor(): MatcherSourceDescriptor {
        return MatcherSourceDescriptor.builder()
            .type(TypeDescriptor.builder().fullQualifiedName(this).build()).build()
    }
}
