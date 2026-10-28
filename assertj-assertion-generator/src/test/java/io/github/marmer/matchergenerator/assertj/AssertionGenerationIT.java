/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssertionGenerationIT {

    private static final Pattern CLASS_DECLARATION =
            Pattern.compile("class (\\w+)(?:<.*?>)?\\s+extends\\s+(\\w+)", Pattern.DOTALL);
    private static final LocalDateTime NOW = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Test
    @DisplayName("A bean gets an abstract assertion class and a concrete one")
    void aBeanGetsAnAbstractAssertionClassAndAConcreteOne() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.SimplePojo"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.SimplePojo",
                                """
                                package some.other.pck;

                                public class SimplePojo {
                                    private String nonPropertyField;

                                    public String getName() {
                                        return "name";
                                    }

                                    public int getAge() {
                                        return 1;
                                    }

                                    public boolean isActive() {
                                        return true;
                                    }

                                    public java.util.List<String> getTags() {
                                        return null;
                                    }

                                    public String someNonPropertyMethod() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(compilation, "a-bean", "some.other.pck.AbstractSimplePojoAssert");
        assertGolden(compilation, "a-bean", "some.other.pck.SimplePojoAssert");
    }

    @Test
    @DisplayName("A record is asserted through its components")
    void aRecordIsAssertedThroughItsComponents() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Address"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Address",
                                """
                                package some.other.pck;

                                public record Address(String street, String city) {
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(compilation, "a-record", "some.other.pck.AbstractAddressAssert");
        assertGolden(compilation, "a-record", "some.other.pck.AddressAssert");
    }

    @Test
    @DisplayName("Nested types get nested assertion classes and nullable properties are marked")
    void nestedTypesGetNestedAssertionClasses() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Order"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Order",
                                """
                                package some.other.pck;

                                import org.jspecify.annotations.Nullable;

                                public class Order {
                                    public @Nullable Customer getCustomer() {
                                        return null;
                                    }

                                    public static class Customer {
                                        public String getName() {
                                            return null;
                                        }

                                        public static class Address {
                                            public String getCity() {
                                                return null;
                                            }
                                        }
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(compilation, "nested-types", "some.other.pck.AbstractOrderAssert");
        assertGolden(compilation, "nested-types", "some.other.pck.OrderAssert");
    }

    @Test
    @DisplayName("A generic type is asserted with wildcards for its type parameters")
    void aGenericTypeIsAssertedWithWildcards() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Box"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Box",
                                """
                                package some.other.pck;

                                import java.util.List;
                                import java.util.Map;

                                public class Box<T, U extends Number> {
                                    public T getContent() {
                                        return null;
                                    }

                                    public List<T> getContents() {
                                        return null;
                                    }

                                    public Map<String, ? extends U> getNumbers() {
                                        return null;
                                    }

                                    public List<? super Integer> getSupers() {
                                        return null;
                                    }

                                    public List<?> getAnything() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(compilation, "a-generic-type", "some.other.pck.AbstractBoxAssert");
        assertGolden(compilation, "a-generic-type", "some.other.pck.BoxAssert");
    }

    @Test
    @DisplayName("A type without properties gets no helpers that nothing uses")
    void aTypeWithoutPropertiesGetsNoHelpersThatNothingUses() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Empty"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Empty",
                                """
                                package some.other.pck;

                                public class Empty {
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(compilation, "without-properties", "some.other.pck.AbstractEmptyAssert");
        final String content =
                compilation
                        .generatedSourceFile("some.other.pck.AbstractEmptyAssert")
                        .orElseThrow()
                        .getCharContent(true)
                        .toString();
        assertFalse(content.contains("requireActual"), content);
        assertFalse(content.contains("checkProperty"), content);
        assertFalse(content.contains("path"), content);
    }

    @Test
    @DisplayName("A property with a getter of two types is asserted as Object")
    void aPropertyWithAGetterOfTwoTypesIsAssertedAsObject() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Conflicting"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Conflicting",
                                """
                                package some.other.pck;

                                public interface Conflicting {
                                    String getConflictProperty();

                                    boolean isConflictProperty();
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(compilation, "conflicting-types", "some.other.pck.AbstractConflictingAssert");
    }

    @Test
    @DisplayName("Without JSpecify on the class path the generated code carries no annotations")
    void withoutJSpecifyTheGeneratedCodeCarriesNoAnnotations() throws IOException {
        final Compilation compilation =
                compileWithoutJSpecify(
                        configuration("some.other.pck.Order"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Order",
                                """
                                package some.other.pck;

                                public class Order {
                                    public String getId() {
                                        return null;
                                    }

                                    public int getQuantity() {
                                        return 0;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(compilation, "without-jspecify", "some.other.pck.AbstractOrderAssert");
        assertGolden(compilation, "without-jspecify", "some.other.pck.OrderAssert");
    }

    @Test
    @DisplayName("A configured target package prefix decides the package of both classes")
    void aConfiguredTargetPackagePrefixDecidesThePackageOfBothClasses() {
        final Compilation compilation =
                compileWithJSpecify(
                        JavaFileObjects.forSourceLines(
                                "some.pck.SomeConfiguration",
                                """
                                package some.pck;

                                import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

                                @GenerateMatchers(
                                    value = "some.other.pck.Pojo",
                                    generation = @GenerateMatchers.GenerationConfiguration(
                                        packageConfig = @GenerateMatchers.GenerationConfiguration.PackageConfiguration("prefix.")))
                                public final class SomeConfiguration {
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public String getName() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertTrue(
                compilation
                        .generatedSourceFile("prefix.some.other.pck.AbstractPojoAssert")
                        .isPresent());
        assertTrue(compilation.generatedSourceFile("prefix.some.other.pck.PojoAssert").isPresent());
    }

    @Test
    @DisplayName("Entry classes are never named Assertions or SoftAssertions and never extend them")
    void entryClassesAreNeverNamedAssertionsAndNeverExtendThem() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Assertions", "some.other.pck.SoftAssertions"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Assertions",
                                """
                                package some.other.pck;

                                public class Assertions {
                                    public String getName() {
                                        return null;
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.SoftAssertions",
                                """
                                package some.other.pck;

                                public class SoftAssertions {
                                    public String getName() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(5, compilation.generatedSourceFiles().size());
        for (final JavaFileObject source : compilation.generatedSourceFiles()) {
            final String content = source.getCharContent(true).toString();
            if (source.getName().endsWith("/SoftAssertionsProvider.java")) {
                assertTrue(content.contains("interface SoftAssertionsProvider "), content);
                assertFalse(content.contains("extends org.assertj.core.api.Assertions"), content);
            } else {
                final Matcher declaration = CLASS_DECLARATION.matcher(content);
                assertTrue(declaration.find(), content);
                assertFalse(
                        List.of("Assertions", "SoftAssertions").contains(declaration.group(1)),
                        declaration.group(1));
                assertTrue(
                        List.of(
                                        "AbstractObjectAssert",
                                        "AbstractAssertionsAssert",
                                        "AbstractSoftAssertionsAssert")
                                .contains(declaration.group(2)),
                        declaration.group(2));
            }
            assertFalse(content.contains("import org.assertj.core.api.Assertions;"), content);
            assertFalse(content.contains("import org.assertj.core.api.SoftAssertions;"), content);
        }
    }

    @Test
    @DisplayName("A collection or map property restarts the path at the element type")
    void aCollectionOrMapPropertyRestartsThePathAtTheElementType() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Invoice", "some.other.pck.Line"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Invoice",
                                """
                                package some.other.pck;

                                import java.util.List;
                                import java.util.Map;

                                public class Invoice {
                                    public String getNumber() {
                                        return null;
                                    }

                                    public List<Line> getLines() {
                                        return null;
                                    }

                                    public Map<String, Line> getLinesByCode() {
                                        return null;
                                    }

                                    public Line[] getArray() {
                                        return null;
                                    }

                                    public Tags getTags() {
                                        return null;
                                    }

                                    public Runnable getTask() {
                                        return null;
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Line",
                                """
                                package some.other.pck;

                                public class Line {
                                    public String getSku() {
                                        return null;
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Tags",
                                """
                                package some.other.pck;

                                public class Tags extends java.util.ArrayList<String> {
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        final String content =
                generatedContent(compilation, "some.other.pck.AbstractInvoiceAssert");
        assertTrue(
                content.contains("AssertionPath.restartingWithin(actual.getLines(), requirements)"),
                content);
        assertTrue(
                content.contains(
                        "AssertionPath.restartingWithin(actual.getLinesByCode(), requirements)"),
                content);
        assertTrue(
                content.contains("AssertionPath.restartingWithin(actual.getArray(), requirements)"),
                content);
        assertTrue(
                content.contains("AssertionPath.restartingWithin(actual.getTags(), requirements)"),
                content);
        assertTrue(
                content.contains(
                        "AssertionPath.within(path + \".task\", actual.getTask(), requirements)"),
                content);
        assertTrue(
                content.contains(
                        "AssertionPath.within(path + \".number\", actual.getNumber(), requirements)"),
                content);
        assertGolden(compilation, "collections", "some.other.pck.AbstractInvoiceAssert");
        assertGolden(compilation, "collections", "some.other.pck.InvoiceAssert");
        assertGolden(compilation, "collections", "some.other.pck.AbstractLineAssert");
        assertGolden(compilation, "collections", "some.other.pck.LineAssert");
    }

    @Test
    @DisplayName("A boolean property gets isFoo and isNotFoo, and other properties get none")
    void aBooleanPropertyGetsIsFooAndIsNotFoo() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Switches"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Switches",
                                """
                                package some.other.pck;

                                public class Switches {
                                    public boolean isActive() {
                                        return true;
                                    }

                                    public Boolean getFlagged() {
                                        return null;
                                    }

                                    public String getName() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        final String content =
                generatedContent(compilation, "some.other.pck.AbstractSwitchesAssert");
        assertTrue(content.contains("public SELF isActive() {"), content);
        assertTrue(content.contains("public SELF isNotActive() {"), content);
        assertTrue(content.contains("public SELF isFlagged() {"), content);
        assertTrue(content.contains("public SELF isNotFlagged() {"), content);
        assertFalse(content.contains("isName("), content);
        assertFalse(content.contains("isNotName("), content);
    }

    @Test
    @DisplayName("A generated method named like an inherited AssertJ method is a compile error")
    void aGeneratedMethodNamedLikeAnInheritedOneIsACompileError() {
        final JavaFileObject configuration = configuration("some.other.pck.Pojo");
        final Compilation compilation =
                compileWithJSpecify(
                        configuration,
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public String getSameClassAs() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'sameClassAs'")
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation).hadErrorContaining("'hasSameClassAs'");
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("A boolean property whose isFoo name clashes with an inherited method is an error")
    void aBooleanPropertyWhoseNameClashesIsACompileError() {
        final JavaFileObject configuration = configuration("some.other.pck.Pojo");
        final Compilation compilation =
                compileWithJSpecify(
                        configuration,
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public boolean isIn() {
                                        return true;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'in'")
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation).hadErrorContaining("'isIn'");
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("A property whose hasFieldOrProperty name clashes is a compile error")
    void aPropertyWhoseHasFieldOrPropertyNameClashesIsACompileError() {
        final JavaFileObject configuration = configuration("some.other.pck.Pojo");
        final Compilation compilation =
                compileWithJSpecify(
                        configuration,
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public String getFieldOrProperty() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'fieldOrProperty'")
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation).hadErrorContaining("'hasFieldOrProperty'");
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("A property whose hasFieldOrPropertyWithValue name clashes is a compile error")
    void aPropertyWhoseHasFieldOrPropertyWithValueNameClashesIsACompileError() {
        final JavaFileObject configuration = configuration("some.other.pck.Pojo");
        final Compilation compilation =
                compileWithJSpecify(
                        configuration,
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public String getFieldOrPropertyWithValue() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'fieldOrPropertyWithValue'")
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'hasFieldOrPropertyWithValue'");
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("A boolean null property clashes with isNull and isNotNull, one error for both")
    void aBooleanNullPropertyClashesWithIsNullAndIsNotNullInOneError() {
        final JavaFileObject configuration = configuration("some.other.pck.Pojo");
        final Compilation compilation =
                compileWithJSpecify(
                        configuration,
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public boolean isNull() {
                                        return true;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'null'")
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation).hadErrorContaining("'isNull' and 'isNotNull'");
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("A name that only resembles an inherited AssertJ method is no clash")
    void aNameThatOnlyResemblesAnInheritedMethodIsNoClash() {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Pojo"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public String getSameClass() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
    }

    @Test
    @DisplayName("A package gets one SoftAssertionsProvider with an assertThat and a then per type")
    void aPackageGetsOneSoftAssertionsProviderWithAnAssertThatAndAThenPerType() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Line", "some.other.pck.Order"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Line",
                                """
                                package some.other.pck;

                                public class Line {
                                    public String getSku() {
                                        return null;
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Order",
                                """
                                package some.other.pck;

                                import org.jspecify.annotations.Nullable;

                                public class Order {
                                    public @Nullable Customer getCustomer() {
                                        return null;
                                    }

                                    public static class Customer {
                                        public String getName() {
                                            return null;
                                        }
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                1,
                compilation.generatedSourceFiles().stream()
                        .filter(source -> source.getName().endsWith("/SoftAssertionsProvider.java"))
                        .count());
        assertGolden(
                compilation, "soft-assertions-provider", "some.other.pck.SoftAssertionsProvider");
    }

    @Test
    @DisplayName(
            "Without JSpecify on the class path the SoftAssertionsProvider carries no annotations")
    void withoutJSpecifyTheSoftAssertionsProviderCarriesNoAnnotations() throws IOException {
        final Compilation compilation =
                compileWithoutJSpecify(
                        configuration("some.other.pck.Order"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Order",
                                """
                                package some.other.pck;

                                public class Order {
                                    public String getName() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(
                compilation,
                "soft-assertions-provider-without-jspecify",
                "some.other.pck.SoftAssertionsProvider");
    }

    @Test
    @DisplayName("Generated assertions produce no javac warning under -Xlint:all with JSpecify")
    void generatedAssertionsProduceNoWarningUnderLintAllWithJSpecify() {
        assertNoWarningUnderLintAll(classPathWithJSpecify());
    }

    @Test
    @DisplayName("Generated assertions produce no javac warning under -Xlint:all without JSpecify")
    void generatedAssertionsProduceNoWarningUnderLintAllWithoutJSpecify() {
        assertNoWarningUnderLintAll(classPathWithoutJSpecify());
    }

    @Test
    @DisplayName("The factory of a generic type produces no javac warning under -Xlint:all")
    void theFactoryOfAGenericTypeProducesNoWarningUnderLintAll() {
        final Compilation compilation =
                Compiler.javac()
                        .withClasspath(classPathWithJSpecify())
                        .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                        .withOptions("-Xlint:all")
                        .compile(
                                configuration("some.other.pck.Box"),
                                JavaFileObjects.forSourceLines(
                                        "some.other.pck.Box",
                                        """
                                        package some.other.pck;

                                        public class Box<T, U extends Number> {
                                            public T getContent() {
                                                return null;
                                            }

                                            public U getSize() {
                                                return null;
                                            }
                                        }
                                        """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
    }

    @Test
    @DisplayName(
            "A type parameter bounded by another type parameter is erased to Object in the factory")
    void aTypeParameterBoundedByAnotherIsErasedToObjectInTheFactory() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Pair"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pair",
                                """
                                package some.other.pck;

                                public class Pair<T, U extends T> {
                                    public T getFirst() {
                                        return null;
                                    }

                                    public U getSecond() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertTrue(
                generatedContent(compilation, "some.other.pck.PairAssert")
                        .contains("new Type[] {Object.class, Object.class}"));
    }

    private static void assertNoWarningUnderLintAll(final List<File> classPath) {
        final Compilation compilation =
                Compiler.javac()
                        .withClasspath(classPath)
                        .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                        .withOptions("-Xlint:all")
                        .compile(
                                configuration("some.other.pck.Pojo"),
                                JavaFileObjects.forSourceLines(
                                        "some.other.pck.Pojo",
                                        """
                                        package some.other.pck;

                                        public class Pojo {
                                            public String getName() {
                                                return null;
                                            }

                                            public java.util.List<Pojo> getChildren() {
                                                return null;
                                            }
                                        }
                                        """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
    }

    private static JavaFileObject configuration(final String... configuredTypes) {
        return JavaFileObjects.forSourceLines(
                "some.pck.SomeConfiguration",
                "package some.pck;",
                "import io.github.marmer.matchergenerator.annotations.GenerateMatchers;",
                "@GenerateMatchers({"
                        + Arrays.stream(configuredTypes)
                                .map(type -> "\"" + type + "\"")
                                .collect(Collectors.joining(", "))
                        + "})",
                "public final class SomeConfiguration {",
                "}");
    }

    private static Compilation compileWithJSpecify(final JavaFileObject... sources) {
        return Compiler.javac()
                .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                .compile(Arrays.asList(sources));
    }

    private static Compilation compileWithoutJSpecify(final JavaFileObject... sources) {
        return Compiler.javac()
                .withClasspath(classPathWithoutJSpecify())
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

    private static String generatedContent(
            final Compilation compilation, final String qualifiedName) throws IOException {
        return compilation
                .generatedSourceFile(qualifiedName)
                .orElseThrow(() -> new AssertionError("No source generated for " + qualifiedName))
                .getCharContent(true)
                .toString();
    }

    private static void assertGolden(
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
                        AssertionGenerationIT.class.getPackageName().replace('.', '/')
                                + "/AssertionGenerationIT/"
                                + scenario
                                + "/"
                                + simpleName
                                + ".java",
                        generated.getCharContent(true).toString());
    }
}
