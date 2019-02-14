package io.github.marmer.annotationprocessing;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"some.other.pck.SimplePojo\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "public class SimplePojo{\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "import javax.annotation.Generated;\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);\n" +
                "    }\n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final SimplePojo item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final SimplePojo item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "    public static SimplePojoMatcher isSimplePojo() {\n" +
                "        return new SimplePojoMatcher();\n" +
                "    }\n" +
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