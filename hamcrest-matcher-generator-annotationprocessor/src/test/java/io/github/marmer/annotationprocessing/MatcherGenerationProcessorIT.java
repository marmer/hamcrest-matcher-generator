package io.github.marmer.annotationprocessing;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class MatcherGenerationProcessorIT {

    private static final LocalDateTime NOW = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Test
    @DisplayName("Matcher should have been generated for Pojo from Source file")
    void matcherShouldHaveBeenGeneratedForPojoFromSourceFile() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-have-been-generated-for-pojo-from-source-file", "SimplePojoMatcher.java"));
    }

    @Test
    @DisplayName("Matcher should be generated for Interfaces with property methods")
    void matcherShouldBeGeneratedForInterfacesWithPropertyMethods() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-be-generated-for-interfaces-with-property-methods", "SimplePojoInterfaceMatcher.java"));
    }

    @Test
    @DisplayName("Naming conflicts should be resolved")
    void namingConflictsShouldBeResolved() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("naming-conflicts-should-be-resolved", "SimplePojoInterfaceMatcher.java"));
    }

    @Test
    @DisplayName("Print a warning for not existing configured types")
    void printAWarningForNotExistingConfiguredTypes() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration))
            .processedWith(new MatcherGenerationProcessor()) // Assertion
            .compilesWithoutError()
            .withWarningContaining("Neither a type nor a type exists for 'some.other.pck.NotExistingType'")
            .in(configuration)
            .onLine(5)
            .atColumn(23);
    }

    @Test
    @DisplayName("Additional Annotations are allowed for configuration classes")
    void additionalAnnotationsAreAllowedForConfigurationClasses() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("additional-annotations-are-allowed-for-configuration-classes", "SimplePojoInterfaceMatcher.java"));
    }

    @Test
    @DisplayName("Generation should work for types generated by others")
    void generationShouldWorkForTypesGeneratedByOthers() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("generation-should-work-for-types-generated-by-others", "SimplePojoInterfaceMatcher.java"));
    }

    @Test
    @DisplayName("Matcher should be generated for arrays")
    void matcherShouldBeGeneratedForArrays() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject, javaFileObjectWithComplexInnerTypes))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-be-generated-for-arrays", "SimplePojoInterfaceMatcher.java"));
    }

    @Test
    @DisplayName("Matcher should be generated for inner non static classes")
    void matcherShouldBeGeneratedForInnerNonStaticClasses() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-be-generated-for-inner-non-static-classes", "SomeClassMatcher.java"));
    }

    @Test
    @DisplayName("Generated Matchers should work for inner interfaces")
    void generatedMatchersShouldWorkForInnerInterfaces() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("generated-matchers-should-work-for-inner-interfaces", "SomePojoMatcher.java"));
    }

    @Test
    @DisplayName("Matcher should be generated for Enums with property methods")
    void matcherShouldBeGeneratedForEnumsWithPropertyMethods() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-be-generated-for-enums-with-property-methods", "SimplePojoEnumMatcher.java"));
    }

    @Test
    @DisplayName("Matcher should contain inherited properties")
    void matcherShouldContainInheritedProperties() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject, parentPojo))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-contain-inherited-properties", "SimplePojoMatcher.java"));
    }

    @Test
    @DisplayName("Matcher and matcher methods should not be generated for non public types and properties")
    void matcherAndMatcherMethodsShouldNotBeGeneratedForNonPublicTypesAndProperties() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-and-matcher-methods-should-not-be-generated-for-non-public-types-and-properties", "SimplePojoMatcher.java"))
            .withNoteContaining("Matcher generation skipped for non public type: some.other.pck.SimplePojo.PrivateClass")
            .in(javaFileObject)
            .onLine(14)
            .atColumn(13)
            .and()
            .withNoteContaining("Matcher generation skipped for non public type: some.other.pck.SimplePojo.PackagePrivateClass")
            .in(javaFileObject)
            .onLine(17)
            .atColumn(5);
    }

    @Test
    @DisplayName("Matcher should be generated for types of outer dependencies as well")
    void matcherShouldBeGeneratedForTypesOfOuterDependenciesAsWell() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-be-generated-for-types-of-outer-dependencies-as-well", "ArgumentMatchersMatcher.java"));
    }

    @Test
    @DisplayName("Matchers should be generated for all classes directly located in a configured package")
    void matchersShouldBeGeneratedForAllClassesDirectlyLocatedInAConfiguredPackage() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject1, javaFileObject2))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matchers-should-be-generated-for-all-classes-directly-located-in-a-configured-package", "SimplePojo1Matcher.java"), golden("matchers-should-be-generated-for-all-classes-directly-located-in-a-configured-package", "SimplePojo2Matcher.java"));
    }

    @Test
    @DisplayName("Warns on not existing packages or types")
    void warnsOnNotExistingPackagesOrTypes() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .withWarningContaining("Neither a type nor a type exists for 'not.existing.pck'")
            .in(configuration)
            .onLine(5)
            .atColumn(23);
    }

    @Test
    @DisplayName("Generation should work for MatcherConfiguration (singular) as well")
    void generationShouldWorkForMatcherConfigurationSingularAsWell() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("generation-should-work-for-matcherconfiguration-singular-as-well", "SimplePojoMatcher.java"));
    }

    @Test
    @DisplayName("Generation should work for properties of inner enums")
    void generationShouldWorkForPropertiesOfInnerEnums() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("generation-should-work-for-properties-of-inner-enums", "SimplePojoMatcher.java"));
    }

    @Test
    @DisplayName("Configured inner types should also generate all outer matchers")
    void configuredInnerTypesShouldAlsoGenerateAllOuterMatchers() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("configured-inner-types-should-also-generate-all-outer-matchers", "SimplePojoMatcher.java"));
    }

    @Test
    @DisplayName("Only a single matcher method should be generated for properties of type Matcher")
    void onlyASingleMatcherMethodShouldBeGeneratedForPropertiesOfTypeMatcher() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("only-a-single-matcher-method-should-be-generated-for-properties-of-type-matcher", "SimplePojoMatcher.java"));
    }

    @Test
    @DisplayName("Generics and wildcard properties should be handled properly")
    void genericsAndWildcardPropertiesShouldBeHandledProperly() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("generics-and-wildcard-properties-should-be-handled-properly", "SimplePojoMatcher.java"));
    }

    @Test
    @DisplayName("Generation should work properly for primitives")
    void generationShouldWorkProperlyForPrimitives() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("generation-should-work-properly-for-primitives", "SimplePojoMatcher.java"));
    }

    @Test
    @DisplayName("No error should be thrown if the processor finds a class generated by itself")
    void noErrorShouldBeThrownIfTheProcessorFindsAClassGeneratedByItself() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .withNoteContaining("Generation skipped for: 'some.other.pck.SomeGeneratedType' because is is already generated by this processor")
            .in(javaFileObject)
            .onLine(6)
            .atColumn(17);
    }

    @Test
    @DisplayName("Matchers should be generated in configured base package")
    void matchersShouldBeGeneratedInConfiguredBasePackage() {
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
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW)) // Assertion
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matchers-should-be-generated-in-configured-base-package", "SomePojoMatcher.java"));
    }

    @Test
    @DisplayName("Matcher should be generated for record with single String component")
    @EnabledForJreRange(min = JRE.JAVA_16)
    void matcherShouldBeGeneratedForRecordWithSingleStringComponent() {
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

        // Execution + Assertion
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW))
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-be-generated-for-record-with-single-string-component", "SomeRecordMatcher.java"));
    }

    @Test
    @DisplayName("Matcher should be generated for record with multiple components")
    @EnabledForJreRange(min = JRE.JAVA_16)
    void matcherShouldBeGeneratedForRecordWithMultipleComponents() {
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

        // Execution + Assertion
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW))
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-should-be-generated-for-record-with-multiple-components", "SomeRecordMatcher.java"));
    }

    @Test
    @DisplayName("Matcher for record should include factory method, both withXxx overloads, reset methods and descriptors")
    @EnabledForJreRange(min = JRE.JAVA_16)
    void matcherForRecordShouldIncludeFactoryMethodBothWithXxxOverloadsResetMethodsAndDescriptors() {
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

        // Execution + Assertion
        Truth.assert_()
            .about(JavaSourcesSubjectFactory.javaSources())
            .that(Arrays.asList(configuration, javaFileObject))
            .processedWith(new MatcherGenerationProcessor(() -> NOW))
            .compilesWithoutError()
            .and()
            .generatesSources(golden("matcher-for-record-should-include-factory-method-both-withxxx-overloads-reset-methods-and-descriptors", "PersonRecordMatcher.java"));
    }

    private static JavaFileObject golden(final String scenario, final String fileName) {
        final URL resource = MatcherGenerationProcessorIT.class.getResource("MatcherGenerationProcessorIT/" + scenario + "/" + fileName);
        return JavaFileObjects.forResource(Objects.requireNonNull(resource, () -> "Missing golden file: " + scenario + "/" + fileName));
    }
}
