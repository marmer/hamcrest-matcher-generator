package io.github.marmer.annotationprocessing.creation;

import io.github.marmer.annotationprocessing.core.Logger;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceWriterTest {
    @InjectMocks
    private SourceWriter underTest;

    @Mock
    private Filer filer;
    @Mock
    private Logger logger;

    @Test
    @DisplayName("Given matcher source should be written to file")
    void testCreate_GivenMatcherSourceShouldBeWrittenToFile()
            throws Exception {
        // Preparation
        final MatcherSourceDescriptor sourceDescriptor = newMatcherSourceDescriptorForType("some.Type")
                .withSource("some content");
        final JavaFileObject javaFileObject = mock(JavaFileObject.class);
        when(filer.createSourceFile("some.Type")).thenReturn(javaFileObject);
        final Writer writer = new StringWriter();
        when(javaFileObject.openWriter()).thenReturn(writer);

        // Execution
        underTest.create(sourceDescriptor);

        // Assertion
        assertThat(writer.toString(), is("some content"));
    }

    @Test
    @DisplayName("Errors on file creation should be logged")
    void testCreate_ErrorsOnFileCreationShouldBeLogged()
            throws Exception {
        // Preparation
        final MatcherSourceDescriptor sourceDescriptor = newMatcherSourceDescriptorForType("some.Type");
        when(filer.createSourceFile("some.Type")).thenThrow(new IOException("the error message"));

        // Execution
        underTest.create(sourceDescriptor);

        // Assertion
        verify(logger).error("Cannot write file for: 'some.Type' Message: the error message");
    }

    private MatcherSourceDescriptor newMatcherSourceDescriptorForType(final String fullQualifiedName) {
        return MatcherSourceDescriptor.builder().type(TypeDescriptor.builder().fullQualifiedName(fullQualifiedName).build()).build();
    }
}