package io.github.marmer.annotationprocessing

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class MatcherGenerationProcessorIT {
    @Test
    fun `Matcher should have been generated for Pojo from Source file`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
                package some.pck;
                
                import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
                
                @MatcherConfiguration("some.other.pck.SimplePojo")
                public final class SomeConfiguration{
                
                }"""
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
            package some.other.pck;
            
            public class SimplePojo{
                private String nonPropertyField;
                private static String staticNonPropertyField;
                
                public String getSomeStringProperty(){
                    return "someValue";
                }
            
                public boolean isSomePrimitiveBooleanProperty(){
                    return true;
                }
            
                public Boolean getSomeNonePrimitiveBooleanProperty(){
                    return false;
                }
            
                public String someNonPropertyMethod(){
                    return ">o.O<";
                }
            
                public void getPropertyLikeVoidMethod(){
                }
            
                public String getSomePropertyLikeMethodWithParameters(int param){
                    return String.valueOf(param);
                }
                
                public boolean getSomePrimitiveBooleanPropertyLike(){
                    return true;
                }
                
                public Boolean isSomeWrapperBooleanPropertyLike(){
                    return true;
                }
                
                public static String getStaticPropertyLikeReturnValue(){
                    return "nope";
                }
                
                public static class InnerStaticPojo{
                    public String getInnerStaticPojoProperty(){
                        return "an inner pojo property value";
                    }
                    
                    public static class InnerInnerStaticPojo{
                    }
                }
            }""".trimIndent()
        )

        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Boolean;
            import java.lang.Class;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
                private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
                }
            
                public SimplePojoMatcher withSomeStringProperty(final Matcher<? super String> matcher) {
                    beanPropertyMatcher.with("someStringProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withSomePrimitiveBooleanProperty(final Matcher<? super Boolean> matcher) {
                    beanPropertyMatcher.with("somePrimitiveBooleanProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withSomeNonePrimitiveBooleanProperty(final Matcher<? super Boolean> matcher) {
                    beanPropertyMatcher.with("someNonePrimitiveBooleanProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
                
                public SimplePojoMatcher resetSomeStringProperty() {
                    beanPropertyMatcher.reset("someStringProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetSomePrimitiveBooleanProperty() {
                    beanPropertyMatcher.reset("somePrimitiveBooleanProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetSomeNonePrimitiveBooleanProperty() {
                    beanPropertyMatcher.reset("someNonePrimitiveBooleanProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SimplePojoMatcher withSomeStringProperty(final String value) {
                    beanPropertyMatcher.with("someStringProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withSomePrimitiveBooleanProperty(final boolean value) {
                    beanPropertyMatcher.with("somePrimitiveBooleanProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withSomeNonePrimitiveBooleanProperty(final Boolean value) {
                    beanPropertyMatcher.with("someNonePrimitiveBooleanProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            
                @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                public static class InnerStaticPojoMatcher extends TypeSafeMatcher<SimplePojo.InnerStaticPojo> {
                    private final BeanPropertyMatcher<SimplePojo.InnerStaticPojo> beanPropertyMatcher;
            
                    public InnerStaticPojoMatcher() {
                        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerStaticPojo>(SimplePojo.InnerStaticPojo.class);
                    }
            
                    public InnerStaticPojoMatcher withInnerStaticPojoProperty(final Matcher<? super String> matcher) {
                        beanPropertyMatcher.with("innerStaticPojoProperty", matcher);
                        return this;
                    }
            
                    public InnerStaticPojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
                        beanPropertyMatcher.with("class", matcher);
                        return this;
                    }
            
                    public InnerStaticPojoMatcher resetInnerStaticPojoProperty() {
                        beanPropertyMatcher.reset("innerStaticPojoProperty");
                        return this;
                    }
            
                    public InnerStaticPojoMatcher resetClass() {
                        beanPropertyMatcher.reset("class");
                        return this;
                    }
            
                    public InnerStaticPojoMatcher withInnerStaticPojoProperty(final String value) {
                        beanPropertyMatcher.with("innerStaticPojoProperty", Matchers.equalTo(value));
                        return this;
                    }
            
                    public InnerStaticPojoMatcher withClass(final Class<?> value) {
                        beanPropertyMatcher.with("class", Matchers.equalTo(value));
                        return this;
                    }
                    
                    @Override
                    public void describeTo(final Description description) {
                        beanPropertyMatcher.describeTo(description);
                    }
            
                    @Override
                    protected boolean matchesSafely(final SimplePojo.InnerStaticPojo item) {
                        return beanPropertyMatcher.matches(item);
                    }
            
                    @Override
                    protected void describeMismatchSafely(final SimplePojo.InnerStaticPojo item, final Description description) {
                        beanPropertyMatcher.describeMismatch(item, description);
                    }
            
                    public static InnerStaticPojoMatcher isInnerStaticPojo() {
                        return new InnerStaticPojoMatcher();
                    }
                    @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                    public static class InnerInnerStaticPojoMatcher extends TypeSafeMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo> {
                        private final BeanPropertyMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo> beanPropertyMatcher;
            
                        public InnerInnerStaticPojoMatcher() {
                            beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerStaticPojo.InnerInnerStaticPojo>(SimplePojo.InnerStaticPojo.InnerInnerStaticPojo.class);
                        }
            
                        public InnerInnerStaticPojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
                            beanPropertyMatcher.with("class", matcher);
                            return this;
                        }
            
                        public InnerInnerStaticPojoMatcher resetClass() {
                            beanPropertyMatcher.reset("class");
                            return this;
                        }
            
                        public InnerInnerStaticPojoMatcher withClass(final Class<?> value) {
                            beanPropertyMatcher.with("class", Matchers.equalTo(value));
                            return this;
                        }
            
                        @Override
                        public void describeTo(final Description description) {
                            beanPropertyMatcher.describeTo(description);
                        }
            
                        @Override
                        protected boolean matchesSafely(final SimplePojo.InnerStaticPojo.InnerInnerStaticPojo item) {
                            return beanPropertyMatcher.matches(item);
                        }
            
                        @Override
                        protected void describeMismatchSafely(final SimplePojo.InnerStaticPojo.InnerInnerStaticPojo item, final Description description) {
                            beanPropertyMatcher.describeMismatch(item, description);
                        }
            
                        public static InnerInnerStaticPojoMatcher isInnerInnerStaticPojo() {
                            return new InnerInnerStaticPojoMatcher();
                        }
                    }
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Matcher should be generated for Interfaces with property methods`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface", """
            package some.other.pck;
            
            public interface SimplePojoInterface{
                String getSomeStringProperty();
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {
                private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;
            
                public SimplePojoInterfaceMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);
                }
            
                public SimplePojoInterfaceMatcher withSomeStringProperty(final Matcher<? super String> matcher) {
                    beanPropertyMatcher.with("someStringProperty", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetSomeStringProperty() {
                    beanPropertyMatcher.reset("someStringProperty");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeStringProperty(final String value) {
                    beanPropertyMatcher.with("someStringProperty", Matchers.equalTo(value));
                    return this;
                }
                
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojoInterface item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojoInterface item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
                
                public static SimplePojoInterfaceMatcher isSimplePojoInterface() {
                    return new SimplePojoInterfaceMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Naming conflicts should be resolved`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface", """
            package some.other.pck;
            
            public interface SimplePojoInterface{
                String getConflictProperty();
                boolean isConflictProperty();
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {
                private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;
            
                public SimplePojoInterfaceMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);
                }
            
                public SimplePojoInterfaceMatcher withConflictProperty(final Matcher<?> matcher) {
                    beanPropertyMatcher.with("conflictProperty", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetConflictProperty() {
                    beanPropertyMatcher.reset("conflictProperty");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withConflictProperty(final String value) {
                    beanPropertyMatcher.with("conflictProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withConflictProperty(final boolean value) {
                    beanPropertyMatcher.with("conflictProperty", Matchers.equalTo(value));
                    return this;
                }
                
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojoInterface item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojoInterface item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
                
                public static SimplePojoInterfaceMatcher isSimplePojoInterface() {
                    return new SimplePojoInterfaceMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }


    @Test
    fun `Print a warning for not existing configured types`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.NotExistingType")
            @FunctionalInterface
            public interface SomeConfiguration{
                String someMethod(String blub);
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration))
            .processedWith(MatcherGenerationProcessor()) // Assertion
            .compilesWithoutError()
            .withWarningContaining("Neither a type nor a type exists for 'some.other.pck.NotExistingType'")
            .`in`(configuration)
            .onLine(5)
            .atColumn(23)
    }


    @Test
    fun `Additional Annotations are allowed for configuration classes`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            @FunctionalInterface
            public interface SomeConfiguration{
                String someMethod(String blub);
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface", """
            package some.other.pck;
            
            public interface SimplePojoInterface{
                String getSomeStringProperty();
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {
                private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;
            
                public SimplePojoInterfaceMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);
                }
            
                public SimplePojoInterfaceMatcher withSomeStringProperty(final Matcher<? super String> matcher) {
                    beanPropertyMatcher.with("someStringProperty", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetSomeStringProperty() {
                    beanPropertyMatcher.reset("someStringProperty");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeStringProperty(final String value) {
                    beanPropertyMatcher.with("someStringProperty", Matchers.equalTo(value));
                    return this;
                }
                
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojoInterface item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojoInterface item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
                
                public static SimplePojoInterfaceMatcher isSimplePojoInterface() {
                    return new SimplePojoInterfaceMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Generation should work for types generated by others`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            import javax.annotation.processing.Generated;
            
            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            @Generated("blubba")
            public interface SomeConfiguration{
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface", """
            package some.other.pck;
            
            import javax.annotation.processing.Generated;
            
            @Generated("blubba")
            public interface SimplePojoInterface{
                String getSomeStringProperty();
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {
                private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;
            
                public SimplePojoInterfaceMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);
                }
            
                public SimplePojoInterfaceMatcher withSomeStringProperty(final Matcher<? super String> matcher) {
                    beanPropertyMatcher.with("someStringProperty", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetSomeStringProperty() {
                    beanPropertyMatcher.reset("someStringProperty");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeStringProperty(final String value) {
                    beanPropertyMatcher.with("someStringProperty", Matchers.equalTo(value));
                    return this;
                }
                
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojoInterface item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojoInterface item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
                
                public static SimplePojoInterfaceMatcher isSimplePojoInterface() {
                    return new SimplePojoInterfaceMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }


    @Test
    fun `Matcher should be generated for arrays`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface", """
            package some.other.pck;
            
            public interface SimplePojoInterface{
                 String[] getSomeStringArray();
                 String[][] getSomeMultidimensionalStringArray();
                AnotherComplexType.SomeInnerType[] getSomeInnerTypeArray();
                byte[] getSomePrimitiveArray();
                byte[][] getSomeMultidimensionalPrimitiveArray();
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObjectWithComplexInnerTypes = JavaFileObjects.forSourceLines(
            "some.other.pck.AnotherComplexType", """
            package some.other.pck;
            
            public interface AnotherComplexType{
                interface SomeInnerType{
                    String getSomeStringProperty();
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoInterfaceMatcher extends TypeSafeMatcher<SimplePojoInterface> {
                private final BeanPropertyMatcher<SimplePojoInterface> beanPropertyMatcher;
            
                public SimplePojoInterfaceMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoInterface>(SimplePojoInterface.class);
                }
            
                public SimplePojoInterfaceMatcher withSomeStringArray(final Matcher<? super String[]> matcher) {
                    beanPropertyMatcher.with("someStringArray", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeMultidimensionalStringArray(final Matcher<? super String[][]> matcher) {
                    beanPropertyMatcher.with("someMultidimensionalStringArray", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeInnerTypeArray(final Matcher<? super AnotherComplexType.SomeInnerType[]> matcher) {
                    beanPropertyMatcher.with("someInnerTypeArray", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomePrimitiveArray(final Matcher<? super byte[]> matcher) {
                    beanPropertyMatcher.with("somePrimitiveArray", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeMultidimensionalPrimitiveArray(final Matcher<? super byte[][]> matcher) {
                    beanPropertyMatcher.with("someMultidimensionalPrimitiveArray", matcher);
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetSomeStringArray() {
                    beanPropertyMatcher.reset("someStringArray");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetSomeMultidimensionalStringArray() {
                    beanPropertyMatcher.reset("someMultidimensionalStringArray");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetSomeInnerTypeArray() {
                    beanPropertyMatcher.reset("someInnerTypeArray");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetSomePrimitiveArray() {
                    beanPropertyMatcher.reset("somePrimitiveArray");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher resetSomeMultidimensionalPrimitiveArray() {
                    beanPropertyMatcher.reset("someMultidimensionalPrimitiveArray");
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeStringArray(final String[] value) {
                    beanPropertyMatcher.with("someStringArray", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeMultidimensionalStringArray(final String[][] value) {
                    beanPropertyMatcher.with("someMultidimensionalStringArray", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeInnerTypeArray(final AnotherComplexType.SomeInnerType[] value) {
                    beanPropertyMatcher.with("someInnerTypeArray", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomePrimitiveArray(final byte[] value) {
                    beanPropertyMatcher.with("somePrimitiveArray", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoInterfaceMatcher withSomeMultidimensionalPrimitiveArray(final byte[][] value) {
                    beanPropertyMatcher.with("someMultidimensionalPrimitiveArray", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojoInterface item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojoInterface item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoInterfaceMatcher isSimplePojoInterface() {
                    return new SimplePojoInterfaceMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject, javaFileObjectWithComplexInnerTypes))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Matcher should be generated for inner non static classes`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SomeClass")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomeClass", """
            package some.other.pck;
            
            public class SomeClass{
                public class SomeNonStaticInnerClass{
                    
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SomeClassMatcher extends TypeSafeMatcher<SomeClass> {
                private final BeanPropertyMatcher<SomeClass> beanPropertyMatcher;
            
                public SomeClassMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SomeClass>(SomeClass.class);
                }
            
                public SomeClassMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SomeClassMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SomeClassMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SomeClass item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SomeClass item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
                
                public static SomeClassMatcher isSomeClass() {
                    return new SomeClassMatcher();
                }
            
                @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                public static class SomeNonStaticInnerClassMatcher extends TypeSafeMatcher<SomeClass.SomeNonStaticInnerClass> {
                    private final BeanPropertyMatcher<SomeClass.SomeNonStaticInnerClass> beanPropertyMatcher;
            
                    public SomeNonStaticInnerClassMatcher() {
                        beanPropertyMatcher = new BeanPropertyMatcher<SomeClass.SomeNonStaticInnerClass>(SomeClass.SomeNonStaticInnerClass.class);
                    }
            
                    public SomeNonStaticInnerClassMatcher withClass(final Matcher<? super Class<?>> matcher) {
                        beanPropertyMatcher.with("class", matcher);
                        return this;
                    }
            
                    public SomeNonStaticInnerClassMatcher resetClass() {
                        beanPropertyMatcher.reset("class");
                        return this;
                    }
            
                    public SomeNonStaticInnerClassMatcher withClass(final Class<?> value) {
                        beanPropertyMatcher.with("class", Matchers.equalTo(value));
                        return this;
                    }
            
                    @Override
                    public void describeTo(final Description description) {
                        beanPropertyMatcher.describeTo(description);
                    }
            
                    @Override
                    protected boolean matchesSafely(final SomeClass.SomeNonStaticInnerClass item) {
                        return beanPropertyMatcher.matches(item);
                    }
            
                    @Override
                    protected void describeMismatchSafely(final SomeClass.SomeNonStaticInnerClass item, final Description description) {
                        beanPropertyMatcher.describeMismatch(item, description);
                    }
            
                    public static SomeNonStaticInnerClassMatcher isSomeNonStaticInnerClass() {
                        return new SomeNonStaticInnerClassMatcher();
                    }
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Generated Matchers should work for inner interfaces`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SomePojo")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo", """
            package some.other.pck;
            
            public class SomePojo{
                public interface InnerInterface {
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.SomePojoMatcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SomePojoMatcher extends TypeSafeMatcher<SomePojo> {
                private final BeanPropertyMatcher<SomePojo> beanPropertyMatcher;
            
                public SomePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SomePojo>(SomePojo.class);
                }
            
                public SomePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SomePojoMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SomePojoMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SomePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SomePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SomePojoMatcher isSomePojo() {
                    return new SomePojoMatcher();
                }
            
                @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                public static class InnerInterfaceMatcher extends TypeSafeMatcher<SomePojo.InnerInterface> {
                    private final BeanPropertyMatcher<SomePojo.InnerInterface> beanPropertyMatcher;
            
                    public InnerInterfaceMatcher() {
                        beanPropertyMatcher = new BeanPropertyMatcher<SomePojo.InnerInterface>(SomePojo.InnerInterface.class);
                    }
            
                    @Override
                    public void describeTo(final Description description) {
                        beanPropertyMatcher.describeTo(description);
                    }
            
                    @Override
                    protected boolean matchesSafely(final SomePojo.InnerInterface item) {
                        return beanPropertyMatcher.matches(item);
                    }
            
                    @Override
                    protected void describeMismatchSafely(final SomePojo.InnerInterface item, final Description description) {
                        beanPropertyMatcher.describeMismatch(item, description);
                    }
            
                    public static InnerInterfaceMatcher isInnerInterface() {
                        return new InnerInterfaceMatcher();
                    }
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Matcher should be generated for Enums with property methods`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SimplePojoEnum")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoEnum", """
            package some.other.pck;
            
            public enum SimplePojoEnum{
                SOME_ENUM_CONSTANT;
                public String getSomeStringProperty(){
                    return "someValue";
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoEnumMatcher extends TypeSafeMatcher<SimplePojoEnum> {
                private final BeanPropertyMatcher<SimplePojoEnum> beanPropertyMatcher;
            
                public SimplePojoEnumMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoEnum>(SimplePojoEnum.class);
                }
            
                public SimplePojoEnumMatcher withSomeStringProperty(final Matcher<? super String> matcher) {
                    beanPropertyMatcher.with("someStringProperty", matcher);
                    return this;
                }
            
                public SimplePojoEnumMatcher withDeclaringClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("declaringClass", matcher);
                    return this;
                }
            
                public SimplePojoEnumMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SimplePojoEnumMatcher resetSomeStringProperty() {
                    beanPropertyMatcher.reset("someStringProperty");
                    return this;
                }
            
                public SimplePojoEnumMatcher resetDeclaringClass() {
                    beanPropertyMatcher.reset("declaringClass");
                    return this;
                }
            
                public SimplePojoEnumMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SimplePojoEnumMatcher withSomeStringProperty(final String value) {
                    beanPropertyMatcher.with("someStringProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoEnumMatcher withDeclaringClass(final Class<?> value) {
                    beanPropertyMatcher.with("declaringClass", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoEnumMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojoEnum item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojoEnum item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
                
                public static SimplePojoEnumMatcher isSimplePojoEnum() {
                    return new SimplePojoEnumMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Matcher should contain inherited properties`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SimplePojo")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val parentPojo = JavaFileObjects.forSourceLines(
            "some.other.pck.ParentPojo", """
            package some.other.pck;
            
            public class ParentPojo<T>{
                public CharSequence getPropertyOfBothClasses(){
                    return "someFancyValue";
                }
                
                public T getParentPojoProperty(){
                    return null;
                }
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
            package some.other.pck;
            
            public class SimplePojo extends ParentPojo<String>{
                public String getPropertyOfBothClasses(){
                    return "someFancyValue";
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Object;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
                private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
                }
            
                public SimplePojoMatcher withPropertyOfBothClasses(final Matcher<? super String> matcher) {
                    beanPropertyMatcher.with("propertyOfBothClasses", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withParentPojoProperty(final Matcher<? super Object> matcher) {
                    beanPropertyMatcher.with("parentPojoProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SimplePojoMatcher resetPropertyOfBothClasses() {
                    beanPropertyMatcher.reset("propertyOfBothClasses");
                    return this;
                }
            
                public SimplePojoMatcher resetParentPojoProperty() {
                    beanPropertyMatcher.reset("parentPojoProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SimplePojoMatcher withPropertyOfBothClasses(final String value) {
                    beanPropertyMatcher.with("propertyOfBothClasses", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withParentPojoProperty(final Object value) {
                    beanPropertyMatcher.with("parentPojoProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
                
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
                
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject, parentPojo))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    @DisplayName("Matcher and matcher methods should not be generated for non public types and properties")
    fun `Matcher and matcher methods should not be generated for non public types and properties`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SimplePojo")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
            package some.other.pck;
            
            public class SimplePojo{
                private String getPrivatePropertyProperty(){
                    return "piv";
                }
                 String getPackagePrivatePropertyProperty(){
                    return "def";
                }
                
                protected String getProtectedPropertyProperty(){
                    return "pro";
                }
                private class PrivateClass{
                    
                }
                class PackagePrivateClass{
                    
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.OutputClass", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
                private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
                }
            
                public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SimplePojoMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SimplePojoMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
            .withNoteContaining("Matcher generation skipped for non public type: some.other.pck.SimplePojo.PrivateClass")
            .`in`(javaFileObject)
            .onLine(14)
            .atColumn(13)
            .and()
            .withNoteContaining("Matcher generation skipped for non public type: some.other.pck.SimplePojo.PackagePrivateClass")
            .`in`(javaFileObject)
            .onLine(17)
            .atColumn(5)
    }

    @Test
    fun `Matcher should be generated for types of outer dependencies as well`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("org.mockito.ArgumentMatchers")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "org.mockito.ArgumentMatchersMatcher", """
            package org.mockito;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class ArgumentMatchersMatcher extends TypeSafeMatcher<ArgumentMatchers> {
                private final BeanPropertyMatcher<ArgumentMatchers> beanPropertyMatcher;
            
                public ArgumentMatchersMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<ArgumentMatchers>(ArgumentMatchers.class);
                }
            
                public ArgumentMatchersMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public ArgumentMatchersMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public ArgumentMatchersMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final ArgumentMatchers item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final ArgumentMatchers item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static ArgumentMatchersMatcher isArgumentMatchers() {
                    return new ArgumentMatchersMatcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(listOf(configuration))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Matchers should be generated for all classes directly located in a configured package`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject1 = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo1", """
     package some.other.pck;
     
     public class SimplePojo1{
     }
     """.trimIndent()
        )
        @Language("JAVA") val javaFileObject2 = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo2", """
     package some.other.pck;
     
     public class SimplePojo2{
     }
     """.trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput1 = JavaFileObjects.forSourceString(
            "sample.other.pck.SimplePojo11Matcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojo1Matcher extends TypeSafeMatcher<SimplePojo1> {
                private final BeanPropertyMatcher<SimplePojo1> beanPropertyMatcher;
            
                public SimplePojo1Matcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo1>(SimplePojo1.class);
                }
            
                public SimplePojo1Matcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SimplePojo1Matcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SimplePojo1Matcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo1 item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo1 item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojo1Matcher isSimplePojo1() {
                    return new SimplePojo1Matcher();
                }
            }""".trimIndent()
        )
        @Language("JAVA") val expectedOutput2 = JavaFileObjects.forSourceString(
            "sample.other.pck.SimplePojo22Matcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojo2Matcher extends TypeSafeMatcher<SimplePojo2> {
                private final BeanPropertyMatcher<SimplePojo2> beanPropertyMatcher;
            
                public SimplePojo2Matcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo2>(SimplePojo2.class);
                }
            
                public SimplePojo2Matcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SimplePojo2Matcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SimplePojo2Matcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo2 item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo2 item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojo2Matcher isSimplePojo2() {
                    return new SimplePojo2Matcher();
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject1, javaFileObject2))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput1, expectedOutput2)
    }

    @Test
    fun `Warns on not existing packages or types`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration({"not.existing.pck"})
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
             package some.other.pck;
             
             public class SimplePojo{
             }
             """.trimIndent()
        )
        val now = LocalDateTime.now()

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .withWarningContaining("Neither a type nor a type exists for 'not.existing.pck'")
            .`in`(configuration)
            .onLine(5)
            .atColumn(23)
    }

    @Test
    fun `Generation should work for MatcherConfiguration (singular) as well`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
             package some.other.pck;
             
             public class SimplePojo{
             }
             """.trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.SimplePojoMatcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
                private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
                }
            
                public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SimplePojoMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SimplePojoMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            }""".trimIndent()
        )
        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Generation should work for properties of inner enums`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
            package some.other.pck;
            
            public interface SimplePojo {
                interface InnerType {
                    enum InnerEnum {
                        ONE, TWO
                    }
                }
                
                InnerType.InnerEnum getSomeProperty();
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.SimplePojoMatcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Class;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
                private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
                }
            
                public SimplePojoMatcher withSomeProperty(final Matcher<? super SimplePojo.InnerType.InnerEnum> matcher) {
                    beanPropertyMatcher.with("someProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher resetSomeProperty() {
                    beanPropertyMatcher.reset("someProperty");
                    return this;
                }
            
                public SimplePojoMatcher withSomeProperty(final SimplePojo.InnerType.InnerEnum value) {
                    beanPropertyMatcher.with("someProperty", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            
                @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                public static class InnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType> {
                    private final BeanPropertyMatcher<SimplePojo.InnerType> beanPropertyMatcher;
            
                    public InnerTypeMatcher() {
                        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType>(SimplePojo.InnerType.class);
                    }
            
                    @Override
                    public void describeTo(final Description description) {
                        beanPropertyMatcher.describeTo(description);
                    }
            
                    @Override
                    protected boolean matchesSafely(final SimplePojo.InnerType item) {
                        return beanPropertyMatcher.matches(item);
                    }
            
                    @Override
                    protected void describeMismatchSafely(final SimplePojo.InnerType item,
                                                          final Description description) {
                        beanPropertyMatcher.describeMismatch(item, description);
                    }
            
                    public static InnerTypeMatcher isInnerType() {
                        return new InnerTypeMatcher();
                    }
            
                    @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                    public static class InnerEnumMatcher extends TypeSafeMatcher<SimplePojo.InnerType.InnerEnum> {
                        private final BeanPropertyMatcher<SimplePojo.InnerType.InnerEnum> beanPropertyMatcher;
            
                        public InnerEnumMatcher() {
                            beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType.InnerEnum>(SimplePojo.InnerType.InnerEnum.class);
                        }
            
                        public InnerEnumMatcher withDeclaringClass(final Matcher<? super Class<?>> matcher) {
                            beanPropertyMatcher.with("declaringClass", matcher);
                            return this;
                        }
            
                        public InnerEnumMatcher withClass(final Matcher<? super Class<?>> matcher) {
                            beanPropertyMatcher.with("class", matcher);
                            return this;
                        }
            
                        public InnerEnumMatcher resetDeclaringClass() {
                            beanPropertyMatcher.reset("declaringClass");
                            return this;
                        }
            
                        public InnerEnumMatcher resetClass() {
                            beanPropertyMatcher.reset("class");
                            return this;
                        }
            
                        public InnerEnumMatcher withDeclaringClass(final Class<?> value) {
                            beanPropertyMatcher.with("declaringClass", Matchers.equalTo(value));
                            return this;
                        }
            
                        public InnerEnumMatcher withClass(final Class<?> value) {
                            beanPropertyMatcher.with("class", Matchers.equalTo(value));
                            return this;
                        }
            
                        @Override
                        public void describeTo(final Description description) {
                            beanPropertyMatcher.describeTo(description);
                        }
            
                        @Override
                        protected boolean matchesSafely(final SimplePojo.InnerType.InnerEnum item) {
                            return beanPropertyMatcher.matches(item);
                        }
            
                        @Override
                        protected void describeMismatchSafely(final SimplePojo.InnerType.InnerEnum item,
                                                              final Description description) {
                            beanPropertyMatcher.describeMismatch(item, description);
                        }
            
                        public static InnerEnumMatcher isInnerEnum() {
                            return new InnerEnumMatcher();
                        }
                    }
                }
            }""".trimIndent()
        )
        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Configured inner types should also generate all outer matchers`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration({"some.other.pck.SimplePojo.InnerType.InnerInnerType"})
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
            package some.other.pck;
            
            public interface SimplePojo {
                interface InnerType {
                    interface InnerInnerType {
                    }
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.SimplePojoMatcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
                private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            
                @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                public static class InnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType> {
                    private final BeanPropertyMatcher<SimplePojo.InnerType> beanPropertyMatcher;
            
                    public InnerTypeMatcher() {
                        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType>(SimplePojo.InnerType.class);
                    }
            
                    @Override
                    public void describeTo(final Description description) {
                        beanPropertyMatcher.describeTo(description);
                    }
            
                    @Override
                    protected boolean matchesSafely(final SimplePojo.InnerType item) {
                        return beanPropertyMatcher.matches(item);
                    }
            
                    @Override
                    protected void describeMismatchSafely(final SimplePojo.InnerType item,
                                                          final Description description) {
                        beanPropertyMatcher.describeMismatch(item, description);
                    }
            
                    public static InnerTypeMatcher isInnerType() {
                        return new InnerTypeMatcher();
                    }
            
                    @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                    public static class InnerInnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType.InnerInnerType> {
                        private final BeanPropertyMatcher<SimplePojo.InnerType.InnerInnerType> beanPropertyMatcher;
            
                        public InnerInnerTypeMatcher() {
                            beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType.InnerInnerType>(SimplePojo.InnerType.InnerInnerType.class);
                        }
            
                        @Override
                        public void describeTo(final Description description) {
                            beanPropertyMatcher.describeTo(description);
                        }
            
                        @Override
                        protected boolean matchesSafely(final SimplePojo.InnerType.InnerInnerType item) {
                            return beanPropertyMatcher.matches(item);
                        }
            
                        @Override
                        protected void describeMismatchSafely(final SimplePojo.InnerType.InnerInnerType item,
                                                              final Description description) {
                            beanPropertyMatcher.describeMismatch(item, description);
                        }
            
                        public static InnerInnerTypeMatcher isInnerInnerType() {
                            return new InnerInnerTypeMatcher();
                        }
                    }
                }
            }""".trimIndent()
        )
        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Only a single matcher method should be generated for properties of type Matcher`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
            package some.other.pck;
            
            import org.hamcrest.Matcher;
            
            public interface SimplePojo {
                Matcher<String> getProperty();
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.SimplePojoMatcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Override;
            import java.lang.String;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
                private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
                }
            
                public SimplePojoMatcher withProperty(final Matcher<? extends String> value) {
                    beanPropertyMatcher.with("property", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            }""".trimIndent()
        )
        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Generics and wildcard properties should be handled properly`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
            package some.other.pck;
            
            import java.util.Map;
            import java.util.function.Consumer;import java.util.function.Function;import java.util.function.Supplier;
            import org.hamcrest.Matcher;
            
            import java.util.List;
            
            public interface SimplePojo <T,X extends String & Function<? super String, T> & Consumer<String>>{
                T getProperty();
                Map<T, List<Supplier<?>>> getNestedGenericProperty();
                List<? extends Function<? extends T, ? super Consumer<?>> > getWildcardProperty();
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.SimplePojoMatcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Object;
            import java.lang.Override;
            import java.util.List;
            import java.util.Map;
            import java.util.function.Consumer;
            import java.util.function.Function;
            import java.util.function.Supplier;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo<?, ?>> {
                private final BeanPropertyMatcher<SimplePojo<?, ?>> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo<?, ?>>(SimplePojo.class);
                }
            
                public SimplePojoMatcher withProperty(final Matcher<? super Object> matcher) {
                    beanPropertyMatcher.with("property", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withNestedGenericProperty(final Matcher<? super Map<?, ? extends List<? extends Supplier<?>>>> matcher) {
                    beanPropertyMatcher.with("nestedGenericProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withWildcardProperty(final Matcher<? super List<? extends Function<?, ? super Consumer<?>>>> matcher) {
                    beanPropertyMatcher.with("wildcardProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher resetProperty() {
                    beanPropertyMatcher.reset("property");
                    return this;
                }
            
                public SimplePojoMatcher resetNestedGenericProperty() {
                    beanPropertyMatcher.reset("nestedGenericProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetWildcardProperty() {
                    beanPropertyMatcher.reset("wildcardProperty");
                    return this;
                }
            
                public SimplePojoMatcher withProperty(final Object value) {
                    beanPropertyMatcher.with("property", Matchers.equalTo(value));
                    return this;
                }
           
                public SimplePojoMatcher withNestedGenericProperty(final Map<?, ? extends List<? extends Supplier<?>>> value) {
                    beanPropertyMatcher.with("nestedGenericProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withWildcardProperty(final List<? extends Function<?, ? super Consumer<?>>> value) {
                    beanPropertyMatcher.with("wildcardProperty", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo<?, ?> item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo<?, ?> item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            }""".trimIndent()
        )
        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `Generation should work properly for primitives`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo", """
            package some.other.pck;
            
            public class SimplePojo{
                public int getIntProperty(){
                    return 1;
                }
                public short getShortProperty(){
                    return 2;
                }
                public long getLongProperty(){
                    return 3;
                }
                public double getDoubleProperty(){
                    return 4;
                }
                public float getFloatProperty(){
                    return 5;
                }
                public char getCharProperty(){
                    return 6;
                }
                public byte getByteProperty(){
                    return 7;
                }
                public boolean isBooleanProperty(){
                    return true;
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "sample.other.pck.SimplePojoMatcher", """
            package some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Boolean;
            import java.lang.Byte;
            import java.lang.Character;
            import java.lang.Class;
            import java.lang.Double;
            import java.lang.Float;
            import java.lang.Integer;
            import java.lang.Long;
            import java.lang.Override;
            import java.lang.Short;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.Matcher;
            import org.hamcrest.Matchers;
            import org.hamcrest.TypeSafeMatcher;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
                private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;
            
                public SimplePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
                }
            
                public SimplePojoMatcher withIntProperty(final Matcher<? super Integer> matcher) {
                    beanPropertyMatcher.with("intProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withShortProperty(final Matcher<? super Short> matcher) {
                    beanPropertyMatcher.with("shortProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withLongProperty(final Matcher<? super Long> matcher) {
                    beanPropertyMatcher.with("longProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withDoubleProperty(final Matcher<? super Double> matcher) {
                    beanPropertyMatcher.with("doubleProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withFloatProperty(final Matcher<? super Float> matcher) {
                    beanPropertyMatcher.with("floatProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withCharProperty(final Matcher<? super Character> matcher) {
                    beanPropertyMatcher.with("charProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withByteProperty(final Matcher<? super Byte> matcher) {
                    beanPropertyMatcher.with("byteProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withBooleanProperty(final Matcher<? super Boolean> matcher) {
                    beanPropertyMatcher.with("booleanProperty", matcher);
                    return this;
                }
            
                public SimplePojoMatcher withClass(final Matcher<? super Class<?>> matcher) {
                    beanPropertyMatcher.with("class", matcher);
                    return this;
                }
            
                public SimplePojoMatcher resetIntProperty() {
                    beanPropertyMatcher.reset("intProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetShortProperty() {
                    beanPropertyMatcher.reset("shortProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetLongProperty() {
                    beanPropertyMatcher.reset("longProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetDoubleProperty() {
                    beanPropertyMatcher.reset("doubleProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetFloatProperty() {
                    beanPropertyMatcher.reset("floatProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetCharProperty() {
                    beanPropertyMatcher.reset("charProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetByteProperty() {
                    beanPropertyMatcher.reset("byteProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetBooleanProperty() {
                    beanPropertyMatcher.reset("booleanProperty");
                    return this;
                }
            
                public SimplePojoMatcher resetClass() {
                    beanPropertyMatcher.reset("class");
                    return this;
                }
            
                public SimplePojoMatcher withIntProperty(final int value) {
                    beanPropertyMatcher.with("intProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withShortProperty(final short value) {
                    beanPropertyMatcher.with("shortProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withLongProperty(final long value) {
                    beanPropertyMatcher.with("longProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withDoubleProperty(final double value) {
                    beanPropertyMatcher.with("doubleProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withFloatProperty(final float value) {
                    beanPropertyMatcher.with("floatProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withCharProperty(final char value) {
                    beanPropertyMatcher.with("charProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withByteProperty(final byte value) {
                    beanPropertyMatcher.with("byteProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withBooleanProperty(final boolean value) {
                    beanPropertyMatcher.with("booleanProperty", Matchers.equalTo(value));
                    return this;
                }
            
                public SimplePojoMatcher withClass(final Class<?> value) {
                    beanPropertyMatcher.with("class", Matchers.equalTo(value));
                    return this;
                }
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SimplePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SimplePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SimplePojoMatcher isSimplePojo() {
                    return new SimplePojoMatcher();
                }
            }""".trimIndent()
        )
        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }

    @Test
    fun `No error should be thrown if the processor finds a class generated by itself`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            
            @MatcherConfiguration("some.other.pck.SomeGeneratedType")
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomeGeneratedType", """
            package some.other.pck;
            
            import javax.annotation.processing.Generated;
            
            @Generated(
                    value = "${MatcherGenerationProcessor::class.qualifiedName}",
                    date = "2019-04-19"
            )
            public class SomeGeneratedType{
            
            }""".trimIndent()
        )

        val now = LocalDateTime.now()


        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .withNoteContaining("Generation skipped for: 'some.other.pck.SomeGeneratedType' because is is already generated by this processor")
            .`in`(javaFileObject)
            .onLine(6)
            .atColumn(17)
    }

    @Test
    fun `Matchers should be generated in configured base package`() {
        // Preparation
        @Language("JAVA") val configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration", """
            package some.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration.GenerationConfiguration;
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration.GenerationConfiguration.PackageConfiguration;
            
            @MatcherConfiguration(value = {"some.other.pck.SomePojo"}, generation = @GenerationConfiguration(packageConfig= @PackageConfiguration("my.base.pck.")))
            public final class SomeConfiguration{
                
            }""".trimIndent()
        )
        @Language("JAVA") val javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo", """
            package some.other.pck;
            
            public interface SomePojo{
                interface InnerInterface {
                }
            }""".trimIndent()
        )
        val now = LocalDateTime.now()
        @Language("JAVA") val expectedOutput = JavaFileObjects.forSourceString(
            "my.base.pck.sample.other.pck.SomePojoMatcher", """
            package my.base.pck.some.other.pck;
            
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
            import java.lang.Override;
            import javax.annotation.processing.Generated;
            import org.hamcrest.Description;
            import org.hamcrest.TypeSafeMatcher;
            import some.other.pck.SomePojo;
            
            @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
            public class SomePojoMatcher extends TypeSafeMatcher<SomePojo> {
                private final BeanPropertyMatcher<SomePojo> beanPropertyMatcher;
            
                public SomePojoMatcher() {
                    beanPropertyMatcher = new BeanPropertyMatcher<SomePojo>(SomePojo.class);
                }
            
            
                @Override
                public void describeTo(final Description description) {
                    beanPropertyMatcher.describeTo(description);
                }
            
                @Override
                protected boolean matchesSafely(final SomePojo item) {
                    return beanPropertyMatcher.matches(item);
                }
            
                @Override
                protected void describeMismatchSafely(final SomePojo item, final Description description) {
                    beanPropertyMatcher.describeMismatch(item, description);
                }
            
                public static SomePojoMatcher isSomePojo() {
                    return new SomePojoMatcher();
                }
            
                @Generated(value = "${MatcherGenerationProcessor::class.qualifiedName}", date = "$now")
                public static class InnerInterfaceMatcher extends TypeSafeMatcher<SomePojo.InnerInterface> {
                    private final BeanPropertyMatcher<SomePojo.InnerInterface> beanPropertyMatcher;
            
                    public InnerInterfaceMatcher() {
                        beanPropertyMatcher = new BeanPropertyMatcher<SomePojo.InnerInterface>(SomePojo.InnerInterface.class);
                    }
            
                    @Override
                    public void describeTo(final Description description) {
                        beanPropertyMatcher.describeTo(description);
                    }
            
                    @Override
                    protected boolean matchesSafely(final SomePojo.InnerInterface item) {
                        return beanPropertyMatcher.matches(item);
                    }
            
                    @Override
                    protected void describeMismatchSafely(final SomePojo.InnerInterface item, final Description description) {
                        beanPropertyMatcher.describeMismatch(item, description);
                    }
            
                    public static InnerInterfaceMatcher isInnerInterface() {
                        return new InnerInterfaceMatcher();
                    }
                }
            }""".trimIndent()
        )

        // Execution
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(MatcherGenerationProcessor { now }) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(expectedOutput)
    }
}
