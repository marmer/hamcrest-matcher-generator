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
                "    \n" +
                "    public String someNonPropertyMethod(){\n" +
                "        return \">o.O<\";\n" +
                "    }\n" +
                "    \n" +
                "    public void getPropertyLikeVoidMethod(){\n" +
                "    }\n" +
                "    \n" +
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
                "    public String getSomeStringProperty();\n" +
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

    // TODO: marmer 14.02.2019 Handle Lombok @Data
    // TODO: marmer 14.02.2019 Handle Lombok @Value
    // TODO: marmer 14.02.2019 Handle Lombok @Getter
    // TODO: marmer 14.02.2019 Handle Property of type "org.hamcrest.Matcher"
    // TODO: marmer 18.02.2019 NonPublicModifiers
    // TODO: marmer 18.02.2019 handle Naming Conflicts (custom classpostfix)
    // TODO: marmer 18.02.2019 handle Naming Conflicts (warn and do not create)
    // TODO: marmer 14.02.2019 handle non static inner classes
    // TODO: marmer 14.02.2019 handle inner interfaces and enums
    // TODO: marmer 20.02.2019 int properties in interfaces
    // TODO: marmer 14.02.2019 handle inner classes without an outer class configuration
    // TODO: marmer 18.02.2019 handle (override) existing matcher files
    // TODO: marmer 18.02.2019 Make the generation robust (should communicate "errors" but not mandatorily crash... if possible)
    // TODO: marmer 18.02.2019 what if configuration points to inner class only?
    // TODO: marmer 18.02.2019 what if configuration points to class of a library?
    // TODO: marmer 19.02.2019 handle anonymous types (if needed somehow)
}