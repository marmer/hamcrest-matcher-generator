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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssertionGenerationIT {

    private static final Pattern CLASS_DECLARATION =
            Pattern.compile("class (\\w+)(?:<.*?>)?\\s+extends\\s+(\\w+)", Pattern.DOTALL);
    private static final Pattern TYPE_DECLARATION = Pattern.compile("(?:class|interface) (\\w+)");
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
        assertFalse(content.contains("hasAllPropertiesChecked"), content);
        assertFalse(content.contains("satisfiesCheckingAllProperties"), content);
        assertFalse(content.contains("addressed"), content);
    }

    @Test
    @DisplayName("A type with properties gets ignoringFoo and the completeness check in both forms")
    void aTypeWithPropertiesGetsIgnoringAndTheCompletenessCheck() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Person"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Person",
                                """
                                package some.other.pck;

                                public class Person {
                                    public String getName() {
                                        return null;
                                    }

                                    public String getPhone() {
                                        return null;
                                    }

                                    public boolean isActive() {
                                        return true;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(compilation, "completeness-check", "some.other.pck.AbstractPersonAssert");
    }

    @Test
    @DisplayName("The completeness check does not count class, the property of getClass()")
    void theCompletenessCheckDoesNotCountClass() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Person"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Person",
                                """
                                package some.other.pck;

                                public class Person {
                                    public String getName() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        final String content = generatedContent(compilation, "some.other.pck.AbstractPersonAssert");
        assertFalse(content.contains("ignoringClass"), content);
        assertFalse(content.contains("\"class\""), content);
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
        assertEquals(6, compilation.generatedSourceFiles().size());
        for (final JavaFileObject source : compilation.generatedSourceFiles()) {
            final String content = source.getCharContent(true).toString();
            final boolean mixin = source.getName().endsWith("/SoftAssertionsProvider.java");
            final boolean representation =
                    source.getName().endsWith("/PropertyRepresentation.java");
            final Matcher declaration = TYPE_DECLARATION.matcher(content);
            assertTrue(declaration.find(), content);
            assertFalse(
                    List.of("Assertions", "SoftAssertions").contains(declaration.group(1)),
                    declaration.group(1));
            assertFalse(content.contains("extends org.assertj.core.api.Assertions"), content);
            assertFalse(content.contains("import org.assertj.core.api.Assertions;"), content);
            assertFalse(content.contains("import org.assertj.core.api.SoftAssertions;"), content);
            if (mixin) {
                assertTrue(
                        content.contains(
                                "public interface SoftAssertionsProvider"
                                        + " extends org.assertj.core.api.SoftAssertionsProvider"),
                        content);
            }
            if (representation) {
                assertTrue(
                        content.contains(
                                "public class PropertyRepresentation"
                                        + " extends StandardRepresentation"),
                        content);
            }
            if (!mixin && !representation) {
                final Matcher classDeclaration = CLASS_DECLARATION.matcher(content);
                assertTrue(classDeclaration.find(), content);
                assertTrue(
                        List.of(
                                        "AbstractObjectAssert",
                                        "AbstractAssertionsAssert",
                                        "AbstractSoftAssertionsAssert")
                                .contains(classDeclaration.group(2)),
                        classDeclaration.group(2));
            }
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
    @DisplayName("Two boolean properties foo and notFoo both generate isNotFoo, a compile error")
    void twoBooleanPropertiesGeneratingTheSameNameAreACompileError() {
        final JavaFileObject configuration = configuration("some.other.pck.Pojo");
        final Compilation compilation =
                compileWithJSpecify(
                        configuration,
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public boolean isFoo() {
                                        return true;
                                    }

                                    public boolean isNotFoo() {
                                        return true;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'foo' and 'notFoo'")
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation).hadErrorContaining("'isNotFoo'");
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("Properties foo and fooSatisfying both generate hasFooSatisfying, a compile error")
    void twoPropertiesGeneratingTheSameSatisfyingNameAreACompileError() {
        final JavaFileObject configuration = configuration("some.other.pck.Pojo");
        final Compilation compilation =
                compileWithJSpecify(
                        configuration,
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public String getFoo() {
                                        return null;
                                    }

                                    public String getFooSatisfying() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'foo' and 'fooSatisfying'")
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation).hadErrorContaining("'hasFooSatisfying'");
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("Nested types whose factory constants share a name are a compile error")
    void nestedTypesWithTheSameFactoryConstantAreACompileError() {
        final JavaFileObject configuration = configuration("some.other.pck.Order");
        final Compilation compilation =
                compileWithJSpecify(
                        configuration,
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Order",
                                """
                                package some.other.pck;

                                public class Order {
                                    public String getName() {
                                        return null;
                                    }

                                    public static class FooBar {
                                        public String getName() {
                                            return null;
                                        }
                                    }

                                    public static class Foo_Bar {
                                        public String getName() {
                                            return null;
                                        }
                                    }

                                    public static class FOO_BAR {
                                        public String getName() {
                                            return null;
                                        }
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'FOO_BAR'")
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation).hadErrorContaining("FooBar");
        CompilationSubject.assertThat(compilation).hadErrorContaining("Foo_Bar");
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("A boolean foo and a non boolean notFoo generate no clash")
    void aBooleanAndANonBooleanNameThatResemblesItAreNoClash() {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Pojo"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Pojo",
                                """
                                package some.other.pck;

                                public class Pojo {
                                    public boolean isFoo() {
                                        return true;
                                    }

                                    public String getNotFoo() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
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

    private static JavaFileObject configurationNamed(
            final String simpleName, final String configuredType) {
        return JavaFileObjects.forSourceLines(
                "some.pck." + simpleName,
                "package some.pck;",
                "import io.github.marmer.matchergenerator.annotations.GenerateMatchers;",
                "@GenerateMatchers(\"" + configuredType + "\")",
                "public final class " + simpleName + " {",
                "}");
    }

    private static JavaFileObject beanWithAName(final String simpleName) {
        return JavaFileObjects.forSourceLines(
                "some.other.pck." + simpleName,
                "package some.other.pck;",
                "public class " + simpleName + " {",
                "    public String getName() { return null; }",
                "}");
    }

    @Test
    @DisplayName(
            "Two configurations of one package processed in one round write one mixin with the types of both")
    void twoConfigurationsOfOnePackageProcessedInOneRoundWriteOneMixinWithTheTypesOfBoth()
            throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configurationNamed("FirstConfiguration", "some.other.pck.Line"),
                        configurationNamed("SecondConfiguration", "some.other.pck.Order"),
                        beanWithAName("Line"),
                        beanWithAName("Order"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                1,
                compilation.generatedSourceFiles().stream()
                        .filter(source -> source.getName().endsWith("/SoftAssertionsProvider.java"))
                        .count());
        assertEquals(
                1,
                compilation.generatedSourceFiles().stream()
                        .filter(source -> source.getName().endsWith("/PropertyRepresentation.java"))
                        .count());
        final String mixin = generatedContent(compilation, "some.other.pck.SoftAssertionsProvider");
        assertTrue(
                mixin.contains("default LineAssert assertThat(final @Nullable Line actual)"),
                mixin);
        assertTrue(mixin.contains("default LineAssert then(final @Nullable Line actual)"), mixin);
        assertTrue(
                mixin.contains("default OrderAssert assertThat(final @Nullable Order actual)"),
                mixin);
        assertTrue(mixin.contains("default OrderAssert then(final @Nullable Order actual)"), mixin);
    }

    @Test
    @DisplayName("A mixin of the same package on the class path is a note naming its origin")
    void aMixinOfTheSamePackageOnTheClassPathIsANoteNamingItsOrigin(@TempDir final Path classes)
            throws IOException {
        final Compilation main =
                compileWithJSpecify(
                        configurationNamed("FirstConfiguration", "some.other.pck.Line"),
                        beanWithAName("Line"));
        CompilationSubject.assertThat(main).succeeded();
        for (final JavaFileObject file : main.generatedFiles()) {
            if (file.getKind() == JavaFileObject.Kind.CLASS) {
                final Path target =
                        classes.resolve(file.getName().replaceFirst("^/CLASS_OUTPUT/", ""));
                Files.createDirectories(target.getParent());
                try (InputStream in = file.openInputStream()) {
                    Files.write(target, in.readAllBytes());
                }
            }
        }
        final List<File> classPath = new ArrayList<>(classPathWithJSpecify());
        classPath.add(classes.toFile());

        final Compilation test =
                Compiler.javac()
                        .withClasspath(classPath)
                        .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                        .compile(
                                configurationNamed("SecondConfiguration", "some.other.pck.Order"),
                                beanWithAName("Order"));

        CompilationSubject.assertThat(test).succeeded();
        CompilationSubject.assertThat(test)
                .hadNoteContaining(
                        "some.other.pck.SoftAssertionsProvider also exists on the class path,"
                                + " written by another compilation. The one written now only knows"
                                + " the types generated in this compilation: some.other.pck.Order");
        assertEquals(
                0,
                main.notes().stream()
                        .filter(
                                note ->
                                        note.getMessage(null)
                                                .contains("also exists on the class path"))
                        .count(),
                () -> "Notes: " + main.notes());
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
    @DisplayName(
            "A generated Representation prints configured types by their properties, nested types"
                    + " included, and is written once per package")
    void aGeneratedRepresentationPrintsConfiguredTypesByTheirProperties() throws IOException {
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

                                    public int getQuantity() {
                                        return 0;
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
                        .filter(source -> source.getName().endsWith("/PropertyRepresentation.java"))
                        .count());
        assertGolden(compilation, "representation", "some.other.pck.PropertyRepresentation");
    }

    @Test
    @DisplayName("Without JSpecify on the class path the Representation carries no annotations")
    void withoutJSpecifyTheRepresentationCarriesNoAnnotations() throws IOException {
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
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden(
                compilation,
                "representation-without-jspecify",
                "some.other.pck.PropertyRepresentation");
    }

    @Test
    @DisplayName("A type without properties has no branch in the Representation")
    void aTypeWithoutPropertiesHasNoBranchInTheRepresentation() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Empty", "some.other.pck.Order"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Empty",
                                """
                                package some.other.pck;

                                public class Empty {
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Order",
                                """
                                package some.other.pck;

                                public class Order {
                                    public String getId() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        final String content =
                generatedContent(compilation, "some.other.pck.PropertyRepresentation");
        assertFalse(content.contains("instanceof Empty"), content);
        assertTrue(content.contains("instanceof Order"), content);
    }

    @Test
    @DisplayName("A subtype is tested before its supertype, so the subtype's branch is reachable")
    void aSubtypeIsTestedBeforeItsSupertype() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Animal", "some.other.pck.Dog"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Animal",
                                """
                                package some.other.pck;

                                public class Animal {
                                    public String getName() {
                                        return null;
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Dog",
                                """
                                package some.other.pck;

                                public class Dog extends Animal {
                                    public String getBreed() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        final String content =
                generatedContent(compilation, "some.other.pck.PropertyRepresentation");
        assertTrue(content.indexOf("instanceof Dog ") >= 0, content);
        assertTrue(
                content.indexOf("instanceof Dog ") < content.indexOf("instanceof Animal "),
                content);
        assertGolden(compilation, "subtype-order", "some.other.pck.PropertyRepresentation");
    }

    @Test
    @DisplayName("A generic configured type is tested with a wildcard, like its assertions")
    void aGenericConfiguredTypeIsTestedWithAWildcard() throws IOException {
        final Compilation compilation =
                compileWithJSpecify(
                        configuration("some.other.pck.Box"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Box",
                                """
                                package some.other.pck;

                                import java.util.List;

                                public class Box<T> {
                                    public T getContent() {
                                        return null;
                                    }

                                    public List<T> getContents() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        final String content =
                generatedContent(compilation, "some.other.pck.PropertyRepresentation");
        assertTrue(content.contains("instanceof Box<?> actual"), content);
        assertGolden(
                compilation, "representation-generic", "some.other.pck.PropertyRepresentation");
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
    @DisplayName(
            "The factory and the SoftAssertionsProvider mixin of a generic type produce no javac"
                    + " warning under -Xlint:all")
    void theFactoryAndTheMixinOfAGenericTypeProduceNoWarningUnderLintAll() throws IOException {
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
        assertTrue(
                generatedContent(compilation, "some.other.pck.SoftAssertionsProvider")
                        .contains("proxy(BoxAssert.class, Box.class, actual)"));
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
