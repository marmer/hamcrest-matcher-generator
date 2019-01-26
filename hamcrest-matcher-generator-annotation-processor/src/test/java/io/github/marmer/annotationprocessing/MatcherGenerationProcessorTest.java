package io.github.marmer.annotationprocessing;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import com.sun.tools.javac.util.List;
import org.junit.Test;

import javax.tools.JavaFileObject;

public class MatcherGenerationProcessorTest {
    @Test
    public void testSpikeTest()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "\n" +
                "@MatcherConfiguration(\"de.bla.SampleClass\")\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");
        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("de.bla.SampleClass", "package de.bla;\n" +
                "\n" +
                "public class SampleClass {\n" +
                "    public String someProperty;\n" +
                "\n" +
                "    public String getSomeProperty() {\n" +
                "        return someProperty;\n" +
                "    }\n" +
                "\n" +
                "    public void setSomeProperty(String someProperty) {\n" +
                "        this.someProperty = someProperty;\n" +
                "    }\n" +
                "}");

        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.output.OutputClass", "package sample.output;\n" +
                "public class OutputClass{\n" +
                "}");

        // Execution
        // Assertion
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(List.of(configuration, javaFileObject))
                .processedWith(new MatcherGenerationProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedOutput)
        ;

    }
}