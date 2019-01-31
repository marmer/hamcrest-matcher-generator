package io.github.marmer.annotationprocessing;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static java.util.Arrays.asList;

class MatcherGenerationProcessorITest {

    @Test
    @DisplayName("Matcher should have been generated for Pojo from Source file")
    void testGenerate_MatcherShouldHaveBeenGeneratedForPojoFromSourceFile()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"some.other.pck.SimplePojo\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "public class SimplePojo{\n" +
                "}");

        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "public class SimplePojoMatcher{\n" +
                "}");

        // Execution
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(asList(configuration, javaFileObject))
                .processedWith(new MatcherGenerationProcessor())

                // Assertion
                .compilesWithoutError()
                .and()
                .generatesSources(expectedOutput);

    }
}