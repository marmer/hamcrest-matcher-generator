package io.github.marmer.annotationprocessing;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import com.sun.tools.javac.util.List;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

public class MatcherGenerationProcessorIT {
    @Test
    public void testSpikeTest()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "\n" +
                "@MatcherConfiguration(\"some.pck.SimplePojo\")\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");
        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("de.bla.SampleClass", "package some.pck;\n" +
                "\n" +
                "public class SimplePojo {\n" +
                "    public String someProperty;\n" +
                "\n" +
                "    public String getSomeProperty() {\n" +
                "        return someProperty;\n" +
                "    }\n" +
                "}");

        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.output.OutputClass", "package some.pck;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "@Generated({\"By hamcrest matcher generator: https://github.com/marmer/hamcrest-matcher-generator\"})\n" +
                "public class SimplePojoMatcher{\n" +
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