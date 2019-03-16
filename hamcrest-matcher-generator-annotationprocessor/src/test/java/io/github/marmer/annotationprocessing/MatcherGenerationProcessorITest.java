package io.github.marmer.annotationprocessing;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.time.LocalDate;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

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
                "    private String nonPropertyField;\n" +
                "    private static String staticNonPropertyField;\n" +
                "    \n" +
                "    public String getSomeStringProperty(){\n" +
                "        return \"someValue\";\n" +
                "    }\n" +
                "\n" +
                "    public boolean isSomePrimitiveBooleanProperty(){\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    public Boolean getSomeNonePrimitiveBooleanProperty(){\n" +
                "        return false;\n" +
                "    }\n" +
                "\n" +
                "    public String someNonPropertyMethod(){\n" +
                "        return \">o.O<\";\n" +
                "    }\n" +
                "\n" +
                "    public void getPropertyLikeVoidMethod(){\n" +
                "    }\n" +
                "\n" +
                "    public String getSomePropertyLikeMethodWithParameters(int param){\n" +
                "        return String.valueOf(param);\n" +
                "    }\n" +
                "    \n" +
                "    public boolean getSomePropertyLike(){\n" +
                "        return true;\n" +
                "    }\n" +
                "    \n" +
                "    public static String getStaticPropertyLikeReturnValue(){\n" +
                "        return \"nope\";\n" +
                "    }\n" +
                "    \n" +
                "    public static class InnerStaticPojo{\n" +
                "        public String getInnerStaticPojoProperty(){\n" +
                "            return \"an inner pojo property value\";\n" +
                "        }\n" +
                "        \n" +
                "        public static class InnerInnerStaticPojo{\n" +
                "        }\n" +
                "    }\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "import javax.annotation.Generated;\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withSomeStringProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"someStringProperty\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withSomeStringProperty(final String value) {\n" +
                "        beanPropertyMatcher.with(\"someStringProperty\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withSomePrimitiveBooleanProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"somePrimitiveBooleanProperty\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withSomePrimitiveBooleanProperty(final boolean value) {\n" +
                "        beanPropertyMatcher.with(\"somePrimitiveBooleanProperty\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withSomeNonePrimitiveBooleanProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"someNonePrimitiveBooleanProperty\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withSomeNonePrimitiveBooleanProperty(final Boolean value) {\n" +
                "        beanPropertyMatcher.with(\"someNonePrimitiveBooleanProperty\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
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
                "\n" +
                "    public static SimplePojoMatcher isSimplePojo() {\n" +
                "        return new SimplePojoMatcher();\n" +
                "    }\n" +
                "\n" +
                "    @Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "    public static class InnerStaticPojoMatcher extends TypeSafeMatcher<SimplePojo.InnerStaticPojo> {\n" +
                "        private final BeanPropertyMatcher<SimplePojo.InnerStaticPojo> beanPropertyMatcher;\n" +
                "\n" +
                "        public InnerStaticPojoMatcher() {\n" +
                "            beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerStaticPojo>(SimplePojo.InnerStaticPojo.class);\n" +
                "        }\n" +
                "\n" +
                "        public InnerStaticPojoMatcher withInnerStaticPojoProperty(final Matcher<?> matcher) {\n" +
                "            beanPropertyMatcher.with(\"innerStaticPojoProperty\", matcher);\n" +
                "            return this;\n" +
                "        }\n" +
                "\n" +
                "        public InnerStaticPojoMatcher withInnerStaticPojoProperty(final String value) {\n" +
                "            beanPropertyMatcher.with(\"innerStaticPojoProperty\", Matchers.equalTo(value));\n" +
                "            return this;\n" +
                "        }\n" +
                "\n" +
                "        public InnerStaticPojoMatcher withClass(final Matcher<?> matcher) {\n" +
                "            beanPropertyMatcher.with(\"class\", matcher);\n" +
                "            return this;\n" +
                "        }\n" +
                "\n" +
                "        public InnerStaticPojoMatcher withClass(final Class value) {\n" +
                "            beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "            return this;\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public void describeTo(final Description description) {\n" +
                "            beanPropertyMatcher.describeTo(description);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected boolean matchesSafely(final SimplePojo.InnerStaticPojo item) {\n" +
                "            return beanPropertyMatcher.matches(item);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected void describeMismatchSafely(final SimplePojo.InnerStaticPojo item, final Description description) {\n" +
                "            beanPropertyMatcher.describeMismatch(item, description);\n" +
                "        }\n" +
                "\n" +
                "        public static InnerStaticPojoMatcher isInnerStaticPojo() {\n" +
                "            return new InnerStaticPojoMatcher();\n" +
                "        }\n" +
                "        @Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "        public static class InnerInnerStaticPojoMatcher extends TypeSafeMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo> {\n" +
                "            private final BeanPropertyMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo> beanPropertyMatcher;\n" +
                "\n" +
                "            public InnerInnerStaticPojoMatcher() {\n" +
                "                beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo>(SimplePojo.InnerStaticPojo.InnerInnerStaticPojo.class);\n" +
                "            }\n" +
                "\n" +
                "            public InnerInnerStaticPojoMatcher withClass(final Matcher<?> matcher) {\n" +
                "                beanPropertyMatcher.with(\"class\", matcher);\n" +
                "                return this;\n" +
                "            }\n" +
                "\n" +
                "            public InnerInnerStaticPojoMatcher withClass(final Class value) {\n" +
                "                beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "                return this;\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            public void describeTo(final Description description) {\n" +
                "                beanPropertyMatcher.describeTo(description);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            protected boolean matchesSafely(final SimplePojo.InnerStaticPojo.InnerInnerStaticPojo item) {\n" +
                "                return beanPropertyMatcher.matches(item);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            protected void describeMismatchSafely(final SimplePojo.InnerStaticPojo.InnerInnerStaticPojo item, final Description description) {\n" +
                "                beanPropertyMatcher.describeMismatch(item, description);\n" +
                "            }\n" +
                "\n" +
                "            public static InnerInnerStaticPojoMatcher isInnerInnerStaticPojo() {\n" +
                "                return new InnerInnerStaticPojoMatcher();\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}"
        );

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

    @Test
    @DisplayName("Matcher should be generated for Interfaces with property methods")
    void testGenerate_MatcherShouldBeGeneratedForInterfacesWithPropertyMethods()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"some.other.pck.SimplePojoInterface\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojoInterface", "package some.other.pck;\n" +
                "\n" +
                "public interface SimplePojoInterface{\n" +
                "    String getSomeStringProperty();\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "import javax.annotation.Generated;\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {\n" +
                "    private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher withSomeStringProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"someStringProperty\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher withSomeStringProperty(final String value) {\n" +
                "        beanPropertyMatcher.with(\"someStringProperty\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final SimplePojoInterface item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final SimplePojoInterface item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "    \n" +
                "    public static SimplePojoInterfaceMatcher isSimplePojoInterface() {\n" +
                "        return new SimplePojoInterfaceMatcher();\n" +
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

    @Test
    @DisplayName("Matcher should be generated for arrays")
    void testGenerate_MatcherShouldBeGeneratedForArrays()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"some.other.pck.SimplePojoInterface\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojoInterface", "package some.other.pck;\n" +
                "\n" +
                "public interface SimplePojoInterface{\n" +
                "     String[] getSomeStringArray();\n" +
                "     String[][] getSomeMultidimensionalStringArray();\n" +
                "    AnotherComplexType.SomeInnerType[] getSomeInnerTypeArray();\n" +
                "}");
        final JavaFileObject javaFileObjectWithComplexInnerTypes = JavaFileObjects.forSourceLines("some.other.pck.AnotherComplexType", "package some.other.pck;\n" +
                "\n" +
                "public interface AnotherComplexType{\n" +
                "    interface SomeInnerType{\n" +
                "        String getSomeStringProperty();\n" +
                "    }\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {\n" +
                "    private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher withSomeStringArray(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"someStringArray\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher withSomeStringArray(final String[] value) {\n" +
                "        beanPropertyMatcher.with(\"someStringArray\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher withSomeMultidimensionalStringArray(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"someMultidimensionalStringArray\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher withSomeMultidimensionalStringArray(final String[][] value) {\n" +
                "        beanPropertyMatcher.with(\"someMultidimensionalStringArray\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher withSomeInnerTypeArray(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"someInnerTypeArray\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoInterfaceMatcher withSomeInnerTypeArray(final AnotherComplexType.SomeInnerType[] value) {\n" +
                "        beanPropertyMatcher.with(\"someInnerTypeArray\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final SimplePojoInterface item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final SimplePojoInterface item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "\n" +
                "    public static SimplePojoInterfaceMatcher isSimplePojoInterface() {\n" +
                "        return new SimplePojoInterfaceMatcher();\n" +
                "    }\n" +
                "}");

        // Execution
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(asList(configuration, javaFileObject, javaFileObjectWithComplexInnerTypes))
                .processedWith(new MatcherGenerationProcessor())

                // Assertion
                .compilesWithoutError()
                .and()
                .generatesSources(expectedOutput);
    }

    @Test
    @DisplayName("Matcher should be generated for inner non static classes")
    void testGenerate_MatcherShouldBeGeneratedForInnerNonStaticClasses()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"some.other.pck.SomeClass\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SomeClass", "package some.other.pck;\n" +
                "\n" +
                "public class SomeClass{\n" +
                "    public class SomeNonStaticInnerClass{\n" +
                "        \n" +
                "    }\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "import javax.annotation.Generated;\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SomeClassMatcher extends TypeSafeMatcher<SomeClass> {\n" +
                "    private final BeanPropertyMatcher<SomeClass> beanPropertyMatcher;\n" +
                "\n" +
                "    public SomeClassMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SomeClass>(SomeClass.class);\n" +
                "    }\n" +
                "\n" +
                "    public SomeClassMatcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SomeClassMatcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final SomeClass item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final SomeClass item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "    \n" +
                "    public static SomeClassMatcher isSomeClass() {\n" +
                "        return new SomeClassMatcher();\n" +
                "    }\n" +
                "\n" +
                "    @Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "    public static class SomeNonStaticInnerClassMatcher extends TypeSafeMatcher<SomeClass.SomeNonStaticInnerClass> {\n" +
                "        private final BeanPropertyMatcher<SomeClass.SomeNonStaticInnerClass> beanPropertyMatcher;\n" +
                "\n" +
                "        public SomeNonStaticInnerClassMatcher() {\n" +
                "            beanPropertyMatcher = new BeanPropertyMatcher<SomeClass.SomeNonStaticInnerClass>(SomeClass.SomeNonStaticInnerClass.class);\n" +
                "        }\n" +
                "\n" +
                "        public SomeNonStaticInnerClassMatcher withClass(final Matcher<?> matcher) {\n" +
                "            beanPropertyMatcher.with(\"class\", matcher);\n" +
                "            return this;\n" +
                "        }\n" +
                "\n" +
                "        public SomeNonStaticInnerClassMatcher withClass(final Class value) {\n" +
                "            beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "            return this;\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public void describeTo(final Description description) {\n" +
                "            beanPropertyMatcher.describeTo(description);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected boolean matchesSafely(final SomeClass.SomeNonStaticInnerClass item) {\n" +
                "            return beanPropertyMatcher.matches(item);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected void describeMismatchSafely(final SomeClass.SomeNonStaticInnerClass item, final Description description) {\n" +
                "            beanPropertyMatcher.describeMismatch(item, description);\n" +
                "        }\n" +
                "\n" +
                "        public static SomeNonStaticInnerClassMatcher isSomeNonStaticInnerClass() {\n" +
                "            return new SomeNonStaticInnerClassMatcher();\n" +
                "        }\n" +
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

    @Test
    @DisplayName("Generated Matchers should work for inner interfaces")
    void testGenerate_GeneratedMatchersShouldWorkForInnerInterfaces()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"some.other.pck.SomePojo\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SomePojo", "package some.other.pck;\n" +
                "\n" +
                "public class SomePojo{\n" +
                "    public interface InnerInterface {\n" +
                "    }\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SomePojoMatcher extends TypeSafeMatcher<SomePojo> {\n" +
                "    private final BeanPropertyMatcher<SomePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SomePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SomePojo>(SomePojo.class);\n" +
                "    }\n" +
                "\n" +
                "    public SomePojoMatcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SomePojoMatcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final SomePojo item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final SomePojo item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "\n" +
                "    public static SomePojoMatcher isSomePojo() {\n" +
                "        return new SomePojoMatcher();\n" +
                "    }\n" +
                "\n" +
                "    @Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "    public static class InnerInterfaceMatcher extends TypeSafeMatcher<SomePojo.InnerInterface> {\n" +
                "        private final BeanPropertyMatcher<SomePojo.InnerInterface> beanPropertyMatcher;\n" +
                "\n" +
                "        public InnerInterfaceMatcher() {\n" +
                "            beanPropertyMatcher = new BeanPropertyMatcher<SomePojo.InnerInterface>(SomePojo.InnerInterface.class);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public void describeTo(final Description description) {\n" +
                "            beanPropertyMatcher.describeTo(description);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected boolean matchesSafely(final SomePojo.InnerInterface item) {\n" +
                "            return beanPropertyMatcher.matches(item);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected void describeMismatchSafely(final SomePojo.InnerInterface item, final Description description) {\n" +
                "            beanPropertyMatcher.describeMismatch(item, description);\n" +
                "        }\n" +
                "\n" +
                "        public static InnerInterfaceMatcher isInnerInterface() {\n" +
                "            return new InnerInterfaceMatcher();\n" +
                "        }\n" +
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

    @Test
    @DisplayName("Matcher should be generated for Enums with property methods")
    void testGenerate_MatcherShouldBeGeneratedForEnumsWithPropertyMethods()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"some.other.pck.SimplePojoEnum\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojoEnum", "package some.other.pck;\n" +
                "\n" +
                "public enum SimplePojoEnum{\n" +
                "    SOME_ENUM_CONSTANT;\n" +
                "    public String getSomeStringProperty(){\n" +
                "        return \"someValue\";\n" +
                "    }\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "import javax.annotation.Generated;\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoEnumMatcher extends TypeSafeMatcher<SimplePojoEnum> {\n" +
                "    private final BeanPropertyMatcher<SimplePojoEnum> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoEnumMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoEnum>(SimplePojoEnum.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoEnumMatcher withSomeStringProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"someStringProperty\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoEnumMatcher withSomeStringProperty(final String value) {\n" +
                "        beanPropertyMatcher.with(\"someStringProperty\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final SimplePojoEnum item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final SimplePojoEnum item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "    \n" +
                "    public static SimplePojoEnumMatcher isSimplePojoEnum() {\n" +
                "        return new SimplePojoEnumMatcher();\n" +
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


    @Test
    @DisplayName("Matcher should contain inherited properties")
    void testGenerate_MatcherShouldContainInheritedProperties()
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

        final JavaFileObject parentPojo = JavaFileObjects.forSourceLines("some.other.pck.ParentPojo", "package some.other.pck;\n" +
                "\n" +
                "public class ParentPojo{\n" +
                "    public String getPropertyOfBothClasses(){\n" +
                "        return \"someFancyValue\";\n" +
                "    }\n" +
                "    \n" +
                "    public String getParentPojoProperty(){\n" +
                "        return \"someValue\";\n" +
                "    }\n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "public class SimplePojo extends ParentPojo{\n" +
                "    public String getPropertyOfBothClasses(){\n" +
                "        return \"someFancyValue\";\n" +
                "    }\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "import javax.annotation.Generated;\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withPropertyOfBothClasses(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"propertyOfBothClasses\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withPropertyOfBothClasses(final String value) {\n" +
                "        beanPropertyMatcher.with(\"propertyOfBothClasses\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withParentPojoProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"parentPojoProperty\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withParentPojoProperty(final String value) {\n" +
                "        beanPropertyMatcher.with(\"parentPojoProperty\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "    \n" +
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
                "    \n" +
                "    public static SimplePojoMatcher isSimplePojo() {\n" +
                "        return new SimplePojoMatcher();\n" +
                "    }\n" +
                "}");

        // Execution
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(asList(configuration, javaFileObject, parentPojo))
                .processedWith(new MatcherGenerationProcessor())

                // Assertion
                .compilesWithoutError()
                .and()
                .generatesSources(expectedOutput);
    }

    @Test
    @DisplayName("Matcher and matcher methods should not be generated for non public types and properties")
    void testGenerate_MatcherAndMatcherMethodsShouldNotBeGeneratedForNonPublicTypesAndProperties()
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
                "    private String getPrivatePropertyProperty(){\n" +
                "        return \"piv\";\n" +
                "    }\n" +
                "     String getPackagePrivatePropertyProperty(){\n" +
                "        return \"def\";\n" +
                "    }\n" +
                "    \n" +
                "    protected String getProtectedPropertyProperty(){\n" +
                "        return \"pro\";\n" +
                "    }\n" +
                "    private class PrivateClass{\n" +
                "        \n" +
                "    }\n" +
                "    class PackagePrivateClass{\n" +
                "        \n" +
                "    }\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.OutputClass", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
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
                "\n" +
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
                .generatesSources(expectedOutput)
                .withNoteContaining("Processing skipped for non public type: some.other.pck.SimplePojo.PrivateClass")
                .in(javaFileObject)
                .and()
                .withNoteContaining("Processing skipped for non public type: some.other.pck.SimplePojo.PackagePrivateClass")
                .in(javaFileObject);
    }

    @Test
    @DisplayName("Matcher should be generated for types of outer dependencies as well")
    void testGenerate_MatcherShouldBeGeneratedForTypesOfOuterDependenciesAsWell()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"org.mockito.ArgumentMatchers\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("org.mockito.ArgumentMatchersMatcher", "package org.mockito;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class ArgumentMatchersMatcher extends TypeSafeMatcher<ArgumentMatchers> {\n" +
                "    private final BeanPropertyMatcher<ArgumentMatchers> beanPropertyMatcher;\n" +
                "\n" +
                "    public ArgumentMatchersMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<ArgumentMatchers>(ArgumentMatchers.class);\n" +
                "    }\n" +
                "\n" +
                "    public ArgumentMatchersMatcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public ArgumentMatchersMatcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final ArgumentMatchers item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final ArgumentMatchers item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "\n" +
                "    public static ArgumentMatchersMatcher isArgumentMatchers() {\n" +
                "        return new ArgumentMatchersMatcher();\n" +
                "    }\n" +
                "}");

        // Execution
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(singletonList(configuration))
                .processedWith(new MatcherGenerationProcessor())

                // Assertion
                .compilesWithoutError()
                .and()
                .generatesSources(expectedOutput);
    }


    @Test
    @DisplayName("Matchers should be generated for all classes directly located in a configured package")
    void testGenerate_MatchersShouldBeGeneratedForAllClassesDirectlyLocatedInAConfiguredPackage()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration(\"some.other.pck\"))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject1 = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo1", "package some.other.pck;\n" +
                "\n" +
                "public class SimplePojo1{\n" +
                "}");

        final JavaFileObject javaFileObject2 = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo2", "package some.other.pck;\n" +
                "\n" +
                "public class SimplePojo2{\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput1 = JavaFileObjects.forSourceString("sample.other.pck.SimplePojo11Matcher", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojo1Matcher extends TypeSafeMatcher<SimplePojo1> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo1> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojo1Matcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo1>(SimplePojo1.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojo1Matcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojo1Matcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final SimplePojo1 item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final SimplePojo1 item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "\n" +
                "    public static SimplePojo1Matcher isSimplePojo1() {\n" +
                "        return new SimplePojo1Matcher();\n" +
                "    }\n" +
                "}");
        final JavaFileObject expectedOutput2 = JavaFileObjects.forSourceString("sample.other.pck.SimplePojo22Matcher", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojo2Matcher extends TypeSafeMatcher<SimplePojo2> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo2> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojo2Matcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo2>(SimplePojo2.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojo2Matcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojo2Matcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeTo(final Description description) {\n" +
                "        beanPropertyMatcher.describeTo(description);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(final SimplePojo2 item) {\n" +
                "        return beanPropertyMatcher.matches(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected void describeMismatchSafely(final SimplePojo2 item, final Description description) {\n" +
                "        beanPropertyMatcher.describeMismatch(item, description);\n" +
                "    }\n" +
                "\n" +
                "    public static SimplePojo2Matcher isSimplePojo2() {\n" +
                "        return new SimplePojo2Matcher();\n" +
                "    }\n" +
                "}");

        // Execution
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(asList(configuration, javaFileObject1, javaFileObject2))
                .processedWith(new MatcherGenerationProcessor())

                // Assertion
                .compilesWithoutError()
                .and()
                .generatesSources(expectedOutput1, expectedOutput2);
    }

    @Test
    @DisplayName("Warns on not existing packages or types")
    void testGenerate_WarnsOnNotExistingPackagesOrTypes()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfigurations;\n" +
                "\n" +
                "@MatcherConfigurations(@MatcherConfiguration({\"not.existing.pck\"}))\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "public class SimplePojo{\n" +
                "}");

        // Execution
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(asList(configuration, javaFileObject))
                .processedWith(new MatcherGenerationProcessor())

                // Assertion
                .compilesWithoutError()
                .withWarningContaining("Package or type does not exist: not.existing.pck")
        // TODO: marmer 04.03.2019 more precise location (at least a reference for the configuration annotation. If possible, maybe the exact line and column)
        // .in(javaFileObject)
        // .onLine(XX)
        ;
    }

    @Test
    @DisplayName("Generation should work for MatcherConfiguration (singular) as well")
    void testGenerate_GenerationSholdWorkForMatcherConfigurationAsWell()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "\n" +
                "@MatcherConfiguration({\"some.other.pck.SimplePojo\"})\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "public class SimplePojo{\n" +
                "}");
        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.SimplePojoMatcher", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withClass(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"class\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withClass(final Class value) {\n" +
                "        beanPropertyMatcher.with(\"class\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
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
                "\n" +
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

    @Test
    @DisplayName("Generation should work for properties of inner enums")
    void testGenerate_GenerationShouldWorkForPropertiesOfInnerEnums()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "\n" +
                "@MatcherConfiguration({\"some.other.pck.SimplePojo\"})\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "public interface SimplePojo {\n" +
                "    interface InnerType {\n" +
                "        enum InnerEnum {\n" +
                "            ONE, TWO\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    InnerType.InnerEnum getSomeProperty();\n" +
                "}");
        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.SimplePojoMatcher", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withSomeProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"someProperty\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withSomeProperty(final SimplePojo.InnerType.InnerEnum value) {\n" +
                "        beanPropertyMatcher.with(\"someProperty\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
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
                "\n" +
                "    public static SimplePojoMatcher isSimplePojo() {\n" +
                "        return new SimplePojoMatcher();\n" +
                "    }\n" +
                "\n" +
                "    @Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "    public static class InnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType> {\n" +
                "        private final BeanPropertyMatcher<SimplePojo.InnerType> beanPropertyMatcher;\n" +
                "\n" +
                "        public InnerTypeMatcher() {\n" +
                "            beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType>(SimplePojo.InnerType.class);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public void describeTo(final Description description) {\n" +
                "            beanPropertyMatcher.describeTo(description);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected boolean matchesSafely(final SimplePojo.InnerType item) {\n" +
                "            return beanPropertyMatcher.matches(item);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected void describeMismatchSafely(final SimplePojo.InnerType item,\n" +
                "                                              final Description description) {\n" +
                "            beanPropertyMatcher.describeMismatch(item, description);\n" +
                "        }\n" +
                "\n" +
                "        public static InnerTypeMatcher isInnerType() {\n" +
                "            return new InnerTypeMatcher();\n" +
                "        }\n" +
                "\n" +
                "        @Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "        public static class InnerEnumMatcher extends TypeSafeMatcher<SimplePojo.InnerType.InnerEnum> {\n" +
                "            private final BeanPropertyMatcher<SimplePojo.InnerType.InnerEnum> beanPropertyMatcher;\n" +
                "\n" +
                "            public InnerEnumMatcher() {\n" +
                "                beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType.InnerEnum>(SimplePojo.InnerType.InnerEnum.class);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            public void describeTo(final Description description) {\n" +
                "                beanPropertyMatcher.describeTo(description);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            protected boolean matchesSafely(final SimplePojo.InnerType.InnerEnum item) {\n" +
                "                return beanPropertyMatcher.matches(item);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            protected void describeMismatchSafely(final SimplePojo.InnerType.InnerEnum item,\n" +
                "                                                  final Description description) {\n" +
                "                beanPropertyMatcher.describeMismatch(item, description);\n" +
                "            }\n" +
                "\n" +
                "            public static InnerEnumMatcher isInnerEnum() {\n" +
                "                return new InnerEnumMatcher();\n" +
                "            }\n" +
                "        }\n" +
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

    @Test
    @DisplayName("Configured inner types should also generate all outer matchers")
    void testGenerate_ConfiguredInnerTypesShouldAlsoGenerateAllOuterMatchers()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "\n" +
                "@MatcherConfiguration({\"some.other.pck.SimplePojo.InnerType.InnerInnerType\"})\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "public interface SimplePojo {\n" +
                "    interface InnerType {\n" +
                "        interface InnerInnerType {\n" +
                "        }\n" +
                "    }\n" +
                "}");
        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.SimplePojoMatcher", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
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
                "\n" +
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
                "\n" +
                "    public static SimplePojoMatcher isSimplePojo() {\n" +
                "        return new SimplePojoMatcher();\n" +
                "    }\n" +
                "\n" +
                "    @Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "    public static class InnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType> {\n" +
                "        private final BeanPropertyMatcher<SimplePojo.InnerType> beanPropertyMatcher;\n" +
                "\n" +
                "        public InnerTypeMatcher() {\n" +
                "            beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType>(SimplePojo.InnerType.class);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public void describeTo(final Description description) {\n" +
                "            beanPropertyMatcher.describeTo(description);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected boolean matchesSafely(final SimplePojo.InnerType item) {\n" +
                "            return beanPropertyMatcher.matches(item);\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        protected void describeMismatchSafely(final SimplePojo.InnerType item,\n" +
                "                                              final Description description) {\n" +
                "            beanPropertyMatcher.describeMismatch(item, description);\n" +
                "        }\n" +
                "\n" +
                "        public static InnerTypeMatcher isInnerType() {\n" +
                "            return new InnerTypeMatcher();\n" +
                "        }\n" +
                "\n" +
                "        @Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "        public static class InnerInnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType.InnerInnerType> {\n" +
                "            private final BeanPropertyMatcher<SimplePojo.InnerType.InnerInnerType> beanPropertyMatcher;\n" +
                "\n" +
                "            public InnerInnerTypeMatcher() {\n" +
                "                beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType.InnerInnerType>(SimplePojo.InnerType.InnerInnerType.class);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            public void describeTo(final Description description) {\n" +
                "                beanPropertyMatcher.describeTo(description);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            protected boolean matchesSafely(final SimplePojo.InnerType.InnerInnerType item) {\n" +
                "                return beanPropertyMatcher.matches(item);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            protected void describeMismatchSafely(final SimplePojo.InnerType.InnerInnerType item,\n" +
                "                                                  final Description description) {\n" +
                "                beanPropertyMatcher.describeMismatch(item, description);\n" +
                "            }\n" +
                "\n" +
                "            public static InnerInnerTypeMatcher isInnerInnerType() {\n" +
                "                return new InnerInnerTypeMatcher();\n" +
                "            }\n" +
                "        }\n" +
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

    @Test
    @DisplayName("Only a single matcher method should be generated for properties of type Matcher")
    void testGenerate_OnlyASingleMatcherMethodShouldBeGeneratedForPropertiesOfTypeMatcher()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "\n" +
                "@MatcherConfiguration({\"some.other.pck.SimplePojo\"})\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "import org.hamcrest.Matcher;\n" +
                "\n" +
                "public interface SimplePojo {\n" +
                "    Matcher<String> getProperty();\n" +
                "}");
        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.SimplePojoMatcher", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"property\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
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
                "\n" +
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

    // TODO: marmer 16.03.2019 better Text/name
    @Test
    @DisplayName("Properties with generics should be handled correctly")
    void testGenerate_PropertiesWithGenericsShouldBeHandledCorrectly()
            throws Exception {
        // Preparation
        final JavaFileObject configuration = JavaFileObjects.forSourceLines("some.pck.SomeConfiguration", "package some.pck;\n" +
                "\n" +
                "import io.github.marmer.annotationprocessing.MatcherConfiguration;\n" +
                "\n" +
                "@MatcherConfiguration({\"some.other.pck.SimplePojo\"})\n" +
                "public final class SomeConfiguration{\n" +
                "    \n" +
                "}");

        final JavaFileObject javaFileObject = JavaFileObjects.forSourceLines("some.other.pck.SimplePojo", "package some.other.pck;\n" +
                "\n" +
                "import org.hamcrest.Matcher;\n" +
                "\n" +
                "import java.util.List;\n" +
                "\n" +
                "public interface SimplePojo <T>{\n" +
                "    T getProperty();\n" +
                "    List<? extends String> getWildcardProperty();\n" +
                "}");

        final String today = LocalDate.now().toString();
        final JavaFileObject expectedOutput = JavaFileObjects.forSourceString("sample.other.pck.SimplePojoMatcher", "package some.other.pck;\n" +
                "\n" +
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;\n" +
                "\n" +
                "import java.util.List;\n" +
                "import javax.annotation.Generated;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.Matchers;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "\n" +
                "@Generated(value = \"io.github.marmer.annotationprocessing.core.impl.JavaPoetMatcherGenerator\", date = \"" + today + "\")\n" +
                "public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {\n" +
                "    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;\n" +
                "\n" +
                "    public SimplePojoMatcher() {\n" +
                "        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"property\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withProperty(final Object value) {\n" +
                "        beanPropertyMatcher.with(\"property\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withWildcardProperty(final Matcher<?> matcher) {\n" +
                "        beanPropertyMatcher.with(\"wildcardProperty\", matcher);\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    public SimplePojoMatcher withWildcardProperty(final List value) {\n" +
                "        beanPropertyMatcher.with(\"wildcardProperty\", Matchers.equalTo(value));\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
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
                "\n" +
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

    // TODO: marmer 18.02.2019 handle Naming Conflicts (custom classpostfix)
    // TODO: marmer 18.02.2019 handle Naming Conflicts (info and do not create)
    // TODO: marmer 04.03.2019 Better output messages for not matching results (description and missmatchdescription)
    // TODO: marmer 04.03.2019 how to handle resources in package
    // TODO: marmer 04.03.2019 how to handle non classes (like package-info.java) in package
}