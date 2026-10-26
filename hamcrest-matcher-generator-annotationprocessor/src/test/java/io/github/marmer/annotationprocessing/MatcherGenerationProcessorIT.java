package io.github.marmer.annotationprocessing;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatcherGenerationProcessorIT {

    private static final LocalDateTime NOW = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Test
    @DisplayName("Matcher should have been generated for Pojo from Source file")
    void matcherShouldHaveBeenGeneratedForPojoFromSourceFile() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """

                            package some.pck;

                            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

                            @MatcherConfiguration("some.other.pck.SimplePojo")
                            public final class SomeConfiguration{

                            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
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
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-have-been-generated-for-pojo-from-source-file", "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for Interfaces with property methods")
    void matcherShouldBeGeneratedForInterfacesWithPropertyMethods() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface",
            """
            package some.other.pck;

            public interface SimplePojoInterface{
                String getSomeStringProperty();
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-be-generated-for-interfaces-with-property-methods", "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Naming conflicts should be resolved")
    void namingConflictsShouldBeResolved() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface",
            """
            package some.other.pck;

            public interface SimplePojoInterface{
                String getConflictProperty();
                boolean isConflictProperty();
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "naming-conflicts-should-be-resolved", "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Print a warning for not existing configured types")
    void printAWarningForNotExistingConfiguredTypes() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.NotExistingType")
            @FunctionalInterface
            public interface SomeConfiguration{
                String someMethod(String blub);
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor())
            .compile(Arrays.asList(configuration));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadWarningContaining("Neither a type nor a type exists for 'some.other.pck.NotExistingType'").inFile(configuration).onLine(5).atColumn(23);
    }

    @Test
    @DisplayName("Additional Annotations are allowed for configuration classes")
    void additionalAnnotationsAreAllowedForConfigurationClasses() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            @FunctionalInterface
            public interface SomeConfiguration{
                String someMethod(String blub);
            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface",
            """
            package some.other.pck;

            public interface SimplePojoInterface{
                String getSomeStringProperty();
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "additional-annotations-are-allowed-for-configuration-classes", "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Generation should work for types generated by others")
    void generationShouldWorkForTypesGeneratedByOthers() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            import javax.annotation.processing.Generated;

            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            @Generated("blubba")
            public interface SomeConfiguration{
            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface",
            """
            package some.other.pck;

            import javax.annotation.processing.Generated;

            @Generated("blubba")
            public interface SimplePojoInterface{
                String getSomeStringProperty();
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "generation-should-work-for-types-generated-by-others", "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for arrays")
    void matcherShouldBeGeneratedForArrays() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoInterface",
            """
            package some.other.pck;

            public interface SimplePojoInterface{
                 String[] getSomeStringArray();
                 String[][] getSomeMultidimensionalStringArray();
                AnotherComplexType.SomeInnerType[] getSomeInnerTypeArray();
                byte[] getSomePrimitiveArray();
                byte[][] getSomeMultidimensionalPrimitiveArray();
            }
            """);
        JavaFileObject javaFileObjectWithComplexInnerTypes = JavaFileObjects.forSourceLines(
            "some.other.pck.AnotherComplexType",
            """
            package some.other.pck;

            public interface AnotherComplexType{
                interface SomeInnerType{
                    String getSomeStringProperty();
                }
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject, javaFileObjectWithComplexInnerTypes));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-be-generated-for-arrays", "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for inner non static classes")
    void matcherShouldBeGeneratedForInnerNonStaticClasses() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SomeClass")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomeClass",
            """
            package some.other.pck;

            public class SomeClass{
                public class SomeNonStaticInnerClass{

                }
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-be-generated-for-inner-non-static-classes", "some.other.pck.SomeClassMatcher");
    }

    @Test
    @DisplayName("Generated Matchers should work for inner interfaces")
    void generatedMatchersShouldWorkForInnerInterfaces() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SomePojo")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo",
            """
            package some.other.pck;

            public class SomePojo{
                public interface InnerInterface {
                }
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "generated-matchers-should-work-for-inner-interfaces", "some.other.pck.SomePojoMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for Enums with property methods")
    void matcherShouldBeGeneratedForEnumsWithPropertyMethods() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoEnum")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojoEnum",
            """
            package some.other.pck;

            public enum SimplePojoEnum{
                SOME_ENUM_CONSTANT;
                public String getSomeStringProperty(){
                    return "someValue";
                }
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-be-generated-for-enums-with-property-methods", "some.other.pck.SimplePojoEnumMatcher");
    }

    @Test
    @DisplayName("Matcher should contain inherited properties")
    void matcherShouldContainInheritedProperties() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojo")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject parentPojo = JavaFileObjects.forSourceLines(
            "some.other.pck.ParentPojo",
            """
            package some.other.pck;

            public class ParentPojo<T>{
                public CharSequence getPropertyOfBothClasses(){
                    return "someFancyValue";
                }

                public T getParentPojoProperty(){
                    return null;
                }
            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
            package some.other.pck;

            public class SimplePojo extends ParentPojo<String>{
                public String getPropertyOfBothClasses(){
                    return "someFancyValue";
                }
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject, parentPojo));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-contain-inherited-properties", "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Matcher and matcher methods should not be generated for non public types and properties")
    void matcherAndMatcherMethodsShouldNotBeGeneratedForNonPublicTypesAndProperties() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojo")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
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
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-and-matcher-methods-should-not-be-generated-for-non-public-types-and-properties", "some.other.pck.SimplePojoMatcher");
        CompilationSubject.assertThat(compilation).hadNoteContaining("Matcher generation skipped for non public type: some.other.pck.SimplePojo.PrivateClass").inFile(javaFileObject).onLine(14).atColumn(13);
        CompilationSubject.assertThat(compilation).hadNoteContaining("Matcher generation skipped for non public type: some.other.pck.SimplePojo.PackagePrivateClass").inFile(javaFileObject).onLine(17).atColumn(5);
    }

    @Test
    @DisplayName("Matcher should be generated for types of outer dependencies as well")
    void matcherShouldBeGeneratedForTypesOfOuterDependenciesAsWell() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("org.mockito.ArgumentMatchers")
            public final class SomeConfiguration{

            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-be-generated-for-types-of-outer-dependencies-as-well", "org.mockito.ArgumentMatchersMatcher");
    }

    @Test
    @DisplayName("Matchers should be generated for all classes directly located in a configured package")
    void matchersShouldBeGeneratedForAllClassesDirectlyLocatedInAConfiguredPackage() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject1 = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo1",
            """
            package some.other.pck;

            public class SimplePojo1{
            }
            """);
        JavaFileObject javaFileObject2 = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo2",
            """
            package some.other.pck;

            public class SimplePojo2{
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject1, javaFileObject2));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matchers-should-be-generated-for-all-classes-directly-located-in-a-configured-package", "some.other.pck.SimplePojo1Matcher");
        assertGeneratedSource(compilation, "matchers-should-be-generated-for-all-classes-directly-located-in-a-configured-package", "some.other.pck.SimplePojo2Matcher");
    }

    @Test
    @DisplayName("Warns on not existing packages or types")
    void warnsOnNotExistingPackagesOrTypes() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"not.existing.pck"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
            package some.other.pck;

            public class SimplePojo{
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadWarningContaining("Neither a type nor a type exists for 'not.existing.pck'").inFile(configuration).onLine(5).atColumn(23);
    }

    @Test
    @DisplayName("Generation should work for MatcherConfiguration (singular) as well")
    void generationShouldWorkForMatcherConfigurationSingularAsWell() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
            package some.other.pck;

            public class SimplePojo{
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "generation-should-work-for-matcherconfiguration-singular-as-well", "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Generation should work for properties of inner enums")
    void generationShouldWorkForPropertiesOfInnerEnums() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
            package some.other.pck;

            public interface SimplePojo {
                interface InnerType {
                    enum InnerEnum {
                        ONE, TWO
                    }
                }

                InnerType.InnerEnum getSomeProperty();
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "generation-should-work-for-properties-of-inner-enums", "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Configured inner types should also generate all outer matchers")
    void configuredInnerTypesShouldAlsoGenerateAllOuterMatchers() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo.InnerType.InnerInnerType"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
            package some.other.pck;

            public interface SimplePojo {
                interface InnerType {
                    interface InnerInnerType {
                    }
                }
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "configured-inner-types-should-also-generate-all-outer-matchers", "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Only a single matcher method should be generated for properties of type Matcher")
    void onlyASingleMatcherMethodShouldBeGeneratedForPropertiesOfTypeMatcher() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
            package some.other.pck;

            import org.hamcrest.Matcher;

            public interface SimplePojo {
                Matcher<String> getProperty();
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "only-a-single-matcher-method-should-be-generated-for-properties-of-type-matcher", "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Generics and wildcard properties should be handled properly")
    void genericsAndWildcardPropertiesShouldBeHandledProperly() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
            package some.other.pck;

            import java.util.Map;
            import java.util.function.Consumer;import java.util.function.Function;import java.util.function.Supplier;
            import org.hamcrest.Matcher;

            import java.util.List;

            public interface SimplePojo <T,X extends String & Function<? super String, T> & Consumer<String>>{
                T getProperty();
                Map<T, List<Supplier<?>>> getNestedGenericProperty();
                List<? extends Function<? extends T, ? super Consumer<?>> > getWildcardProperty();
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "generics-and-wildcard-properties-should-be-handled-properly", "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Generation should work properly for primitives")
    void generationShouldWorkProperlyForPrimitives() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            """
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
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "generation-should-work-properly-for-primitives", "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("No error should be thrown if the processor finds a class generated by itself")
    void noErrorShouldBeThrownIfTheProcessorFindsAClassGeneratedByItself() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SomeGeneratedType")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomeGeneratedType",
            """
            package some.other.pck;

            import javax.annotation.processing.Generated;

            @Generated(
                    value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor",
                    date = "2019-04-19"
            )
            public class SomeGeneratedType{

            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadNoteContaining("Generation skipped for: 'some.other.pck.SomeGeneratedType' because is is already generated by this processor").inFile(javaFileObject).onLine(6).atColumn(17);
    }

    @Test
    @DisplayName("Matchers should be generated in configured base package")
    void matchersShouldBeGeneratedInConfiguredBasePackage() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration.GenerationConfiguration;
            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration.GenerationConfiguration.PackageConfiguration;

            @MatcherConfiguration(value = {"some.other.pck.SomePojo"}, generation = @GenerationConfiguration(packageConfig= @PackageConfiguration("my.base.pck.")))
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo",
            """
            package some.other.pck;

            public interface SomePojo{
                interface InnerInterface {
                }
            }
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matchers-should-be-generated-in-configured-base-package", "my.base.pck.some.other.pck.SomePojoMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for record with single String component")
    @EnabledForJreRange(min = JRE.JAVA_16)
    void matcherShouldBeGeneratedForRecordWithSingleStringComponent() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """

                            package some.pck;

                            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

                            @MatcherConfiguration("some.other.pck.SomeRecord")
                            public final class SomeConfiguration{

                            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomeRecord",
            """
            package some.other.pck;

            public record SomeRecord(String name) {}
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-be-generated-for-record-with-single-string-component", "some.other.pck.SomeRecordMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for record with multiple components")
    @EnabledForJreRange(min = JRE.JAVA_16)
    void matcherShouldBeGeneratedForRecordWithMultipleComponents() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """

                            package some.pck;

                            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

                            @MatcherConfiguration("some.other.pck.SomeRecord")
                            public final class SomeConfiguration{

                            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.SomeRecord",
            """
            package some.other.pck;

            public record SomeRecord(String name, int age) {}
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-should-be-generated-for-record-with-multiple-components", "some.other.pck.SomeRecordMatcher");
    }

    @Test
    @DisplayName("Matcher for record should include factory method, both withXxx overloads, reset methods and descriptors")
    @EnabledForJreRange(min = JRE.JAVA_16)
    void matcherForRecordShouldIncludeFactoryMethodBothWithXxxOverloadsResetMethodsAndDescriptors() throws IOException {
        // Preparation
        JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            """

                            package some.pck;

                            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

                            @MatcherConfiguration("some.other.pck.PersonRecord")
                            public final class SomeConfiguration{

                            }
            """);
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
            "some.other.pck.PersonRecord",
            """
            package some.other.pck;

            public record PersonRecord(String firstName, String lastName) {}
            """);

        // Execution
        Compilation compilation = Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(configuration, javaFileObject));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(compilation, "matcher-for-record-should-include-factory-method-both-withxxx-overloads-reset-methods-and-descriptors", "some.other.pck.PersonRecordMatcher");
    }

    private static void assertGeneratedSource(final Compilation compilation, final String scenario, final String qualifiedName) throws IOException {
        final String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        final String expected = readGolden(scenario, simpleName + ".java");
        final JavaFileObject generated = compilation.generatedSourceFile(qualifiedName)
            .orElseThrow(() -> new AssertionError("No source generated for " + qualifiedName));
        assertEquals(expected, generated.getCharContent(true).toString());
    }

    private static String readGolden(final String scenario, final String fileName) throws IOException {
        final String resourceName = "MatcherGenerationProcessorIT/" + scenario + "/" + fileName;
        try (InputStream in = MatcherGenerationProcessorIT.class.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new AssertionError("Missing golden file: " + resourceName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
