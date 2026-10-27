/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.marmer.matchergenerator.core.MatcherGenerationProcessor;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatcherGenerationProcessorIT {

    private static final LocalDateTime NOW = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Test
    @DisplayName("Matcher should have been generated for Pojo from Source file")
    void matcherShouldHaveBeenGeneratedForPojoFromSourceFile() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojo")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-have-been-generated-for-pojo-from-source-file",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for Interfaces with property methods")
    void matcherShouldBeGeneratedForInterfacesWithPropertyMethods() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojoInterface",
                        """
            package some.other.pck;

            public interface SimplePojoInterface{
                String getSomeStringProperty();
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-be-generated-for-interfaces-with-property-methods",
                "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Naming conflicts should be resolved")
    void namingConflictsShouldBeResolved() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojoInterface",
                        """
            package some.other.pck;

            public interface SimplePojoInterface{
                String getConflictProperty();
                boolean isConflictProperty();
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "naming-conflicts-should-be-resolved",
                "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Print a warning for not existing configured types")
    void printAWarningForNotExistingConfiguredTypes() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation =
                Compiler.javac()
                        .withProcessors(new MatcherGenerationProcessor())
                        .compile(Arrays.asList(configuration));

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Neither a type nor a package exists for 'some.other.pck.NotExistingType'")
                .inFile(configuration)
                .onLine(5)
                .atColumn(23);
    }

    @Test
    @DisplayName("Additional Annotations are allowed for configuration classes")
    void additionalAnnotationsAreAllowedForConfigurationClasses() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
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
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojoInterface",
                        """
            package some.other.pck;

            public interface SimplePojoInterface{
                String getSomeStringProperty();
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "additional-annotations-are-allowed-for-configuration-classes",
                "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Generation should work for types generated by others")
    void generationShouldWorkForTypesGeneratedByOthers() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
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
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "generation-should-work-for-types-generated-by-others",
                "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for arrays")
    void matcherShouldBeGeneratedForArrays() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoInterface")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        JavaFileObject javaFileObjectWithComplexInnerTypes =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation =
                compile(configuration, javaFileObject, javaFileObjectWithComplexInnerTypes);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-be-generated-for-arrays",
                "some.other.pck.SimplePojoInterfaceMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for inner non static classes")
    void matcherShouldBeGeneratedForInnerNonStaticClasses() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SomeClass")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomeClass",
                        """
            package some.other.pck;

            public class SomeClass{
                public class SomeNonStaticInnerClass{

                }
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-be-generated-for-inner-non-static-classes",
                "some.other.pck.SomeClassMatcher");
    }

    @Test
    @DisplayName("Generated Matchers should work for inner interfaces")
    void generatedMatchersShouldWorkForInnerInterfaces() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SomePojo")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        """
            package some.other.pck;

            public class SomePojo{
                public interface InnerInterface {
                }
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "generated-matchers-should-work-for-inner-interfaces",
                "some.other.pck.SomePojoMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for Enums with property methods")
    void matcherShouldBeGeneratedForEnumsWithPropertyMethods() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojoEnum")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-be-generated-for-enums-with-property-methods",
                "some.other.pck.SimplePojoEnumMatcher");
    }

    @Test
    @DisplayName("Matcher should contain inherited properties")
    void matcherShouldContainInheritedProperties() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojo")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject parentPojo =
                JavaFileObjects.forSourceLines(
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
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compile(configuration, javaFileObject, parentPojo);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-contain-inherited-properties",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName(
            "Matcher and matcher methods should not be generated for non public types and properties")
    void matcherAndMatcherMethodsShouldNotBeGeneratedForNonPublicTypesAndProperties()
            throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SimplePojo")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compileVerbose(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-and-matcher-methods-should-not-be-generated-for-non-public-types-and-properties",
                "some.other.pck.SimplePojoMatcher");
        CompilationSubject.assertThat(compilation)
                .hadNoteContaining(
                        "Matcher generation skipped for non public type: some.other.pck.SimplePojo.PrivateClass")
                .inFile(javaFileObject)
                .onLine(14)
                .atColumn(13);
        CompilationSubject.assertThat(compilation)
                .hadNoteContaining(
                        "Matcher generation skipped for non public type: some.other.pck.SimplePojo.PackagePrivateClass")
                .inFile(javaFileObject)
                .onLine(17)
                .atColumn(5);
    }

    @Test
    @DisplayName("Matcher should be generated for types of outer dependencies as well")
    void matcherShouldBeGeneratedForTypesOfOuterDependenciesAsWell() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("org.mockito.ArgumentMatchers")
            public final class SomeConfiguration{

            }
            """);

        // Execution
        Compilation compilation = compile(configuration);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-be-generated-for-types-of-outer-dependencies-as-well",
                "org.mockito.ArgumentMatchersMatcher");
    }

    @Test
    @DisplayName(
            "Matchers should be generated for all classes directly located in a configured package")
    void matchersShouldBeGeneratedForAllClassesDirectlyLocatedInAConfiguredPackage()
            throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject1 =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo1",
                        """
            package some.other.pck;

            public class SimplePojo1{
            }
            """);
        JavaFileObject javaFileObject2 =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo2",
                        """
            package some.other.pck;

            public class SimplePojo2{
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject1, javaFileObject2);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        final String scenario =
                "matchers-should-be-generated-for-all-classes-directly-located-in-a-configured-package";
        assertGeneratedSource(compilation, scenario, "some.other.pck.SimplePojo1Matcher");
        assertGeneratedSource(compilation, scenario, "some.other.pck.SimplePojo2Matcher");
    }

    @Test
    @DisplayName("Warns on not existing packages or types")
    void warnsOnNotExistingPackagesOrTypes() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"not.existing.pck"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        """
            package some.other.pck;

            public class SimplePojo{
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Neither a type nor a package exists for 'not.existing.pck'")
                .inFile(configuration)
                .onLine(5)
                .atColumn(24);
    }

    @Test
    @DisplayName("Generation should work for MatcherConfiguration (singular) as well")
    void generationShouldWorkForMatcherConfigurationSingularAsWell() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        """
            package some.other.pck;

            public class SimplePojo{
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "generation-should-work-for-matcherconfiguration-singular-as-well",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Generation should work for properties of inner enums")
    void generationShouldWorkForPropertiesOfInnerEnums() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "generation-should-work-for-properties-of-inner-enums",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Configured inner types should also generate all outer matchers")
    void configuredInnerTypesShouldAlsoGenerateAllOuterMatchers() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo.InnerType.InnerInnerType"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "configured-inner-types-should-also-generate-all-outer-matchers",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Only a single matcher method should be generated for properties of type Matcher")
    void onlyASingleMatcherMethodShouldBeGeneratedForPropertiesOfTypeMatcher() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        """
            package some.other.pck;

            import org.hamcrest.Matcher;

            public interface SimplePojo {
                Matcher<String> getProperty();
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "only-a-single-matcher-method-should-be-generated-for-properties-of-type-matcher",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName(
            "Completeness check methods should be generated: ignoring per property and checking all")
    void completenessCheckMethodsShouldBeGenerated() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        """
            package some.other.pck;

            public interface SimplePojo {
                String getProperty();
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "completeness-check-methods-should-be-generated",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName(
            "Completeness check methods should be generated for types configured with GenerateMatchers")
    void completenessCheckMethodsShouldBeGeneratedForGenerateMatchers() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers("some.other.pck.SimplePojo")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        """
            package some.other.pck;

            public interface SimplePojo {
                String getProperty();
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "completeness-check-methods-should-be-generated",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Reference object matcher should preset deep checks for types with a matcher")
    void referenceObjectMatcherShouldPresetDeepChecksForTypesWithAMatcher() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers({
                "some.other.pck.Order",
                "some.other.pck.Line",
                "some.other.pck.Color",
                "some.other.pck.Marker"
            })
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject order =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Order",
                        """
            package some.other.pck;

            import java.util.Collection;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            public class Order {
                public String getId() { return null; }
                public int getQuantity() { return 0; }
                public Line getFirstLine() { return null; }
                public List<Line> getLines() { return null; }
                public Set<Line> getLineSet() { return null; }
                public Collection<? extends Line> getWildcardLines() { return null; }
                public Map<String, Line> getLinesBySku() { return null; }
                public Map<Line, String> getSkuByLine() { return null; }
                public Line[] getLineArray() { return null; }
                public List<String> getTags() { return null; }
                public Color getColor() { return null; }
                public Marker getMarker() { return null; }
                public Detail getDetail() { return null; }

                public static class Detail {
                    public Order getOrder() { return null; }
                }
            }
            """);
        JavaFileObject line =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Line",
                        """
            package some.other.pck;

            public class Line {
                public String getSku() { return null; }
                public Order getOrder() { return null; }
            }
            """);
        JavaFileObject color =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Color",
                        """
            package some.other.pck;

            public enum Color {
                RED, GREEN;

                public String getLabel() { return name(); }
            }
            """);
        JavaFileObject marker =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Marker",
                        """
            package some.other.pck;

            public class Marker {
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, order, line, color, marker);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "reference-object-matcher-presets-deep-checks",
                "some.other.pck.OrderMatcher");
        assertGeneratedSource(
                compilation,
                "reference-object-matcher-presets-deep-checks",
                "some.other.pck.MarkerMatcher");
    }

    @Test
    @DisplayName("Reference object matcher should refer to matchers of other configurations")
    void referenceObjectMatcherShouldReferToMatchersOfOtherConfigurations() throws IOException {
        // Preparation
        JavaFileObject firstConfiguration =
                JavaFileObjects.forSourceLines(
                        "some.pck.FirstConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers("some.other.pck.Order")
            public final class FirstConfiguration{

            }
            """);
        JavaFileObject secondConfiguration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SecondConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;
            import io.github.marmer.matchergenerator.annotations.GenerateMatchers.GenerationConfiguration;
            import io.github.marmer.matchergenerator.annotations.GenerateMatchers.GenerationConfiguration.PackageConfiguration;

            @GenerateMatchers(
                value = "some.other.pck.Customer",
                generation = @GenerationConfiguration(packageConfig = @PackageConfiguration("prefixed")))
            public final class SecondConfiguration{

            }
            """);
        JavaFileObject order =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Order",
                        """
            package some.other.pck;

            public class Order {
                public Customer getCustomer() { return null; }
            }
            """);
        JavaFileObject customer =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Customer",
                        """
            package some.other.pck;

            public class Customer {
                public String getName() { return null; }
            }
            """);

        // Execution
        Compilation compilation = compile(firstConfiguration, secondConfiguration, order, customer);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "reference-object-matcher-refers-to-other-configurations",
                "some.other.pck.OrderMatcher");
    }

    @Test
    @DisplayName("Generics and wildcard properties should be handled properly")
    void genericsAndWildcardPropertiesShouldBeHandledProperly() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "generics-and-wildcard-properties-should-be-handled-properly",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("Generation should work properly for primitives")
    void generationShouldWorkProperlyForPrimitives() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration({"some.other.pck.SimplePojo"})
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
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
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "generation-should-work-properly-for-primitives",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("No error should be thrown if the processor finds a class generated by itself")
    void noErrorShouldBeThrownIfTheProcessorFindsAClassGeneratedByItself() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SomeGeneratedType")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomeGeneratedType",
                        """
            package some.other.pck;

            import javax.annotation.processing.Generated;

            @Generated(
                    value = "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
                    date = "2019-04-19"
            )
            public class SomeGeneratedType{

            }
            """);

        // Execution
        Compilation compilation = compileVerbose(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadNoteContaining(
                        "Generation skipped for: 'some.other.pck.SomeGeneratedType' because it is already generated by this processor")
                .inFile(javaFileObject)
                .onLine(6)
                .atColumn(17);
    }

    @Test
    @DisplayName("Matchers should be generated in configured base package")
    void matchersShouldBeGeneratedInConfiguredBasePackage() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
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
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        """
            package some.other.pck;

            public interface SomePojo{
                interface InnerInterface {
                }
            }
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matchers-should-be-generated-in-configured-base-package",
                "my.base.pck.some.other.pck.SomePojoMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for record with single String component")
    void matcherShouldBeGeneratedForRecordWithSingleStringComponent() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SomeRecord")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomeRecord",
                        """
            package some.other.pck;

            public record SomeRecord(String name) {}
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-be-generated-for-record-with-single-string-component",
                "some.other.pck.SomeRecordMatcher");
    }

    @Test
    @DisplayName("Matcher should be generated for record with multiple components")
    void matcherShouldBeGeneratedForRecordWithMultipleComponents() throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.SomeRecord")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomeRecord",
                        """
            package some.other.pck;

            public record SomeRecord(String name, int age) {}
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-should-be-generated-for-record-with-multiple-components",
                "some.other.pck.SomeRecordMatcher");
    }

    @Test
    @DisplayName(
            "Matcher for record should include factory method, both withXxx overloads, reset methods and descriptors")
    void matcherForRecordShouldIncludeFactoryMethodBothWithXxxOverloadsResetMethodsAndDescriptors()
            throws IOException {
        // Preparation
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

            @MatcherConfiguration("some.other.pck.PersonRecord")
            public final class SomeConfiguration{

            }
            """);
        JavaFileObject javaFileObject =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.PersonRecord",
                        """
            package some.other.pck;

            public record PersonRecord(String firstName, String lastName) {}
            """);

        // Execution
        Compilation compilation = compile(configuration, javaFileObject);

        // Assertion
        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "matcher-for-record-should-include-factory-method-both-withxxx-overloads-reset-methods-and-descriptors",
                "some.other.pck.PersonRecordMatcher");
    }

    @Test
    @DisplayName(
            "Generated code is null marked and accepts null when JSpecify is on the class path")
    void generatedCodeIsNullMarkedWhenJSpecifyIsOnTheClassPath() throws IOException {
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers("some.other.pck.SimplePojo")
            public final class SomeConfiguration{
            }
            """);
        JavaFileObject pojo =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        """
            package some.other.pck;

            import java.util.List;
            import java.util.Map;
            import org.jspecify.annotations.Nullable;

            public class SimplePojo<T>{
                public String getName(){
                    return "name";
                }

                public @Nullable String getNickname(){
                    return null;
                }

                public int getCount(){
                    return 1;
                }

                public @Nullable Integer getLimit(){
                    return null;
                }

                public @Nullable List<String> getTags(){
                    return null;
                }

                public String[] getParts(){
                    return null;
                }

                public T getPayload(){
                    return null;
                }

                public Map.@Nullable Entry<String, String> getEntry(){
                    return null;
                }

                public Map.Entry<String, String> getRequiredEntry(){
                    return null;
                }

                public static class Inner{
                    public String getInnerName(){
                        return null;
                    }
                }
            }
            """);

        Compilation compilation = compileWithJSpecify(configuration, pojo);

        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "generated-code-is-null-marked-with-jspecify",
                "some.other.pck.SimplePojoMatcher");
    }

    @Test
    @DisplayName("A nullable record component gets the nullable annotations")
    void aNullableRecordComponentGetsTheNullableAnnotations() throws IOException {
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers("some.other.pck.PersonRecord")
            public final class SomeConfiguration{
            }
            """);
        JavaFileObject record =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.PersonRecord",
                        """
            package some.other.pck;

            import org.jspecify.annotations.Nullable;

            public record PersonRecord(String name, @Nullable String nickname, int age){
            }
            """);

        Compilation compilation = compileWithJSpecify(configuration, record);

        CompilationSubject.assertThat(compilation).succeeded();
        assertGeneratedSource(
                compilation,
                "nullable-record-component-gets-nullable-annotations",
                "some.other.pck.PersonRecordMatcher");
    }

    @Test
    @DisplayName(
            "Generated code carries no JSpecify annotations when JSpecify is not on the class path")
    void generatedCodeCarriesNoJSpecifyAnnotationsWithoutJSpecify() throws IOException {
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers("some.other.pck.SimplePojo")
            public final class SomeConfiguration{
            }
            """);
        JavaFileObject pojo =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        """
            package some.other.pck;

            public class SimplePojo{
                public String getName(){
                    return "name";
                }
            }
            """);

        Compilation compilation = compile(configuration, pojo);

        CompilationSubject.assertThat(compilation).succeeded();
        String generated =
                compilation
                        .generatedSourceFile("some.other.pck.SimplePojoMatcher")
                        .orElseThrow()
                        .getCharContent(true)
                        .toString();
        assertFalse(generated.contains("jspecify"), generated);
    }

    @Test
    @DisplayName("Cyclic references between configured types compile")
    void cyclicReferencesBetweenConfiguredTypesCompile() {
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers({"some.other.pck.Parent", "some.other.pck.Child"})
            public final class SomeConfiguration{
            }
            """);
        JavaFileObject parent =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Parent",
                        """
            package some.other.pck;

            public class Parent{
                public Child getChild(){
                    return null;
                }
            }
            """);
        JavaFileObject child =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Child",
                        """
            package some.other.pck;

            public class Child{
                public Parent getParent(){
                    return null;
                }
            }
            """);

        Compilation compilation = compile(configuration, parent, child);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        CompilationSubject.assertThat(compilation)
                .generatedSourceFile("some.other.pck.ParentMatcher");
        CompilationSubject.assertThat(compilation)
                .generatedSourceFile("some.other.pck.ChildMatcher");
    }

    @Test
    @DisplayName("A recursive generic bound compiles")
    void recursiveGenericBoundCompiles() {
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers("some.other.pck.Ranked")
            public final class SomeConfiguration{
            }
            """);
        JavaFileObject ranked =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Ranked",
                        """
            package some.other.pck;

            public class Ranked<T extends Comparable<T>>{
                public T getRank(){
                    return null;
                }
            }
            """);

        Compilation compilation = compile(configuration, ranked);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        CompilationSubject.assertThat(compilation)
                .generatedSourceFile("some.other.pck.RankedMatcher");
    }

    @Test
    @DisplayName("A public inner class compiles")
    void publicInnerClassCompiles() {
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers("some.other.pck.Holder")
            public final class SomeConfiguration{
            }
            """);
        JavaFileObject holder =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Holder",
                        """
            package some.other.pck;

            public class Holder{
                public Inner getInner(){
                    return null;
                }

                public class Inner{
                    public String getName(){
                        return null;
                    }
                }
            }
            """);

        Compilation compilation = compile(configuration, holder);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        CompilationSubject.assertThat(compilation)
                .generatedSourceFile("some.other.pck.HolderMatcher");
    }

    @Test
    @DisplayName("Generated matchers produce no javac warning under -Xlint:all")
    void generatedMatchersProduceNoWarningUnderLintAll() {
        assertNoWarningUnderLintAll(false);
    }

    @Test
    @DisplayName(
            "Generated JSpecify null marked matchers produce no javac warning under -Xlint:all")
    void generatedNullMarkedMatchersProduceNoWarningUnderLintAll() {
        assertNoWarningUnderLintAll(true);
    }

    /**
     * Matchers written in one round are compiled in the next one, which carries the annotations the
     * generator wrote on them. With JSpecify on the class path that is {@code @NullMarked}.
     */
    private static void assertNoWarningUnderLintAll(final boolean withJSpecify) {
        JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        """
            package some.pck;

            import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

            @GenerateMatchers("some.other.pck.Pojo")
            public final class SomeConfiguration{
            }
            """);
        JavaFileObject pojo =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Pojo",
                        """
            package some.other.pck;

            public class Pojo{
                public String getName(){
                    return null;
                }
            }
            """);
        final Compilation compilation =
                Compiler.javac()
                        .withClasspath(
                                withJSpecify ? classPathWithJSpecify() : classPathWithoutJSpecify())
                        .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                        .withOptions("-Xlint:all")
                        .compile(Arrays.asList(configuration, pojo));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
    }

    /**
     * Compiles without JSpecify on the class path, so the generated code carries no JSpecify
     * annotations.
     */
    private static Compilation compile(final JavaFileObject... sources) {
        return Compiler.javac()
                .withClasspath(classPathWithoutJSpecify())
                .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                .compile(Arrays.asList(sources));
    }

    private static Compilation compileWithJSpecify(final JavaFileObject... sources) {
        return Compiler.javac()
                .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                .compile(Arrays.asList(sources));
    }

    private static List<File> classPathWithoutJSpecify() {
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .filter(entry -> !entry.contains("jspecify"))
                .map(File::new)
                .collect(Collectors.toList());
    }

    private static List<File> classPathWithJSpecify() {
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(File::new)
                .collect(Collectors.toList());
    }

    private static Compilation compileVerbose(final JavaFileObject... sources) {
        return Compiler.javac()
                .withClasspath(classPathWithoutJSpecify())
                .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                .withOptions("-Amatchergenerator.verbose")
                .compile(Arrays.asList(sources));
    }

    private static void assertGeneratedSource(
            final Compilation compilation, final String scenario, final String qualifiedName)
            throws IOException {
        final String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        final JavaFileObject generated =
                compilation
                        .generatedSourceFile(qualifiedName)
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "No source generated for " + qualifiedName));
        GoldenFiles.forModule()
                .assertGolden(
                        MatcherGenerationProcessorIT.class.getPackageName().replace('.', '/')
                                + "/MatcherGenerationProcessorIT/"
                                + scenario
                                + "/"
                                + simpleName
                                + ".java",
                        generated.getCharContent(true).toString());
    }
}
