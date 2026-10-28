/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest.kotlin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.marmer.matchergenerator.core.MatcherGenerationProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Compares the emitted Kotlin text with golden files. Compile-testing cannot compile Kotlin, so the
 * Kotlin end-to-end modules prove that the text compiles and behaves.
 */
class KotlinDslGenerationIT {

    private static final LocalDateTime NOW = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final String SCENARIOS =
            KotlinDslGenerationIT.class.getPackageName().replace('.', '/')
                    + "/KotlinDslGenerationIT/";

    @TempDir Path kotlinOutput;

    @Test
    @DisplayName("A bean gets a scope with a value and a matcher overload for every property")
    void aBeanGetsAScopeWithAValueAndAMatcherOverloadForEveryProperty() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("some.other.pck.SimplePojo"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.SimplePojo",
                                """
                                package some.other.pck;

                                import org.jspecify.annotations.Nullable;

                                public class SimplePojo {
                                    public String getName() {
                                        return "name";
                                    }

                                    public int getAge() {
                                        return 1;
                                    }

                                    public boolean isActive() {
                                        return true;
                                    }

                                    public Integer getCount() {
                                        return 1;
                                    }

                                    public @Nullable String getNickname() {
                                        return null;
                                    }

                                    public java.util.List<String> getTags() {
                                        return null;
                                    }

                                    public java.util.Map<String, Integer> getScores() {
                                        return null;
                                    }

                                    public String someNonPropertyMethod() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("a-bean", "some/other/pck/SimplePojoMatcherDsl.kt");
    }

    @Test
    @DisplayName("A record is described through its components")
    void aRecordIsDescribedThroughItsComponents() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("some.other.pck.Address"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Address",
                                """
                                package some.other.pck;

                                public record Address(String street, String city) {
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("a-record", "some/other/pck/AddressMatcherDsl.kt");
    }

    @Test
    @DisplayName("A property of a configured type gets a nested scope, also from another package")
    void aPropertyOfAConfiguredTypeGetsANestedScope() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration(
                                "some.other.pck.Order", "other.pck.Customer", "other.pck.Address"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Order",
                                """
                                package some.other.pck;

                                public class Order {
                                    public long getId() {
                                        return 1L;
                                    }

                                    public other.pck.Customer getCustomer() {
                                        return null;
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "other.pck.Customer",
                                """
                                package other.pck;

                                public class Customer {
                                    public String getName() {
                                        return null;
                                    }

                                    public Address getAddress() {
                                        return null;
                                    }
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "other.pck.Address",
                                """
                                package other.pck;

                                public class Address {
                                    public String getCity() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("nested-scopes", "some/other/pck/OrderMatcherDsl.kt");
        assertGolden("nested-scopes", "other/pck/CustomerMatcherDsl.kt");
        assertGolden("nested-scopes", "other/pck/AddressMatcherDsl.kt");
    }

    @Test
    @DisplayName(
            "A property of an enum, of a type without properties or of an unconfigured type gets no nested scope")
    void aPropertyOfAnEnumOrAnUnconfiguredTypeGetsNoNestedScope() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("some.other.pck.Order"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Order",
                                """
                                package some.other.pck;

                                public class Order {
                                    public Status getStatus() {
                                        return null;
                                    }

                                    public Unconfigured getUnconfigured() {
                                        return null;
                                    }

                                    public java.time.LocalDate getCreated() {
                                        return null;
                                    }

                                    public enum Status {
                                        OPEN
                                    }

                                    public static class Unconfigured {
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("no-nested-scope", "some/other/pck/OrderMatcherDsl.kt");
    }

    @Test
    @DisplayName("Nested types get their scopes in the file of the outer type")
    void nestedTypesGetTheirScopesInTheFileOfTheOuterType() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("some.other.pck.Outer"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Outer",
                                """
                                package some.other.pck;

                                public class Outer {
                                    public Inner getInner() {
                                        return null;
                                    }

                                    public static class Inner {
                                        public String getLabel() {
                                            return null;
                                        }
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("nested-types", "some/other/pck/OuterMatcherDsl.kt");
    }

    @Test
    @DisplayName("A generic type is described with star projections and Any for its type variables")
    void aGenericTypeIsDescribedWithStarProjections() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("some.other.pck.Box"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Box",
                                """
                                package some.other.pck;

                                import java.util.List;

                                public class Box<T extends Number> {
                                    public T getValue() {
                                        return null;
                                    }

                                    public List<T> getValues() {
                                        return null;
                                    }

                                    public Box<T> getSelf() {
                                        return null;
                                    }

                                    public List<? extends Number> getNumbers() {
                                        return null;
                                    }

                                    public List<? super Integer> getSinks() {
                                        return null;
                                    }

                                    public List<?> getAnything() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("a-generic-type", "some/other/pck/BoxMatcherDsl.kt");
    }

    @Test
    @DisplayName("Java types are written as their Kotlin counterparts")
    void javaTypesAreWrittenAsTheirKotlinCounterparts() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("some.other.pck.Kinds"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Kinds",
                                """
                                package some.other.pck;

                                public class Kinds {
                                    public byte getByteValue() {
                                        return 0;
                                    }

                                    public short getShortValue() {
                                        return 0;
                                    }

                                    public char getCharValue() {
                                        return 0;
                                    }

                                    public float getFloatValue() {
                                        return 0;
                                    }

                                    public double getDoubleValue() {
                                        return 0;
                                    }

                                    public Object getAnything() {
                                        return null;
                                    }

                                    public CharSequence getText() {
                                        return null;
                                    }

                                    public int[] getNumbers() {
                                        return null;
                                    }

                                    public String[] getNames() {
                                        return null;
                                    }

                                    public java.util.Set<String> getUnique() {
                                        return null;
                                    }

                                    public java.util.Collection<String> getAll() {
                                        return null;
                                    }

                                    public java.util.Map.Entry<String, Long> getEntry() {
                                        return null;
                                    }

                                    public java.util.Optional<String> getMaybe() {
                                        return null;
                                    }

                                    public java.math.BigDecimal getPrice() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("kotlin-types", "some/other/pck/KindsMatcherDsl.kt");
    }

    @Test
    @DisplayName("A type in the default package gets its DSL in the default package")
    void aTypeInTheDefaultPackageGetsItsDslInTheDefaultPackage() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("Plain"),
                        JavaFileObjects.forSourceLines(
                                "Plain",
                                """
                                public class Plain {
                                    public Plain getSelf() {
                                        return null;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("default-package", "PlainMatcherDsl.kt");
    }

    @Test
    @DisplayName(
            "A property of a matcher type has no matcher overload and a conflicting one is open")
    void aPropertyOfAMatcherTypeHasNoMatcherOverload() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("some.other.pck.Conflicting"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Conflicting",
                                """
                                package some.other.pck;

                                public class Conflicting {
                                    public org.hamcrest.Matcher<String> getMatcher() {
                                        return null;
                                    }

                                    public String getValue() {
                                        return null;
                                    }

                                    public boolean isValue() {
                                        return true;
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertGolden("matcher-and-conflict", "some/other/pck/ConflictingMatcherDsl.kt");
    }

    @Test
    @DisplayName(
            "Without KAPT there is exactly one compiler note and the Java matchers are the same")
    void withoutKaptThereIsExactlyOneCompilerNoteAndTheJavaMatchersAreTheSame() throws IOException {
        final JavaFileObject first =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.First",
                        "package some.other.pck;",
                        "public class First {",
                        "    public String getName() { return null; }",
                        "}");
        final JavaFileObject second =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Second",
                        "package some.other.pck;",
                        "public class Second {",
                        "    public String getName() { return null; }",
                        "}");
        final JavaFileObject configuration =
                configuration("some.other.pck.First", "some.other.pck.Second");

        final Compilation withoutKapt =
                Compiler.javac()
                        .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                        .compile(configuration, first, second);
        final Compilation withKapt = compileWithKapt(configuration, first, second);

        CompilationSubject.assertThat(withoutKapt).succeeded();
        CompilationSubject.assertThat(withoutKapt).hadNoteCount(1);
        CompilationSubject.assertThat(withoutKapt).hadNoteContaining("KAPT");
        CompilationSubject.assertThat(withoutKapt)
                .hadNoteContaining("hamcrest-matcher-generator-kotlin");
        assertTrue(
                withoutKapt.generatedFiles().stream()
                        .noneMatch(file -> file.getName().endsWith(".kt")),
                "No Kotlin file");
        for (final String name : List.of("First", "Second")) {
            assertEquals(
                    generatedContent(withKapt, "some.other.pck." + name + "Matcher"),
                    generatedContent(withoutKapt, "some.other.pck." + name + "Matcher"),
                    name);
        }
        assertEquals(0, withKapt.notes().size(), () -> "Notes: " + withKapt.notes());
    }

    @Test
    @DisplayName("A type without properties gets no DSL, unless a nested type has properties")
    void aTypeWithoutPropertiesGetsNoDsl() throws IOException {
        final Compilation compilation =
                compileWithKapt(
                        configuration("some.other.pck.Empty", "some.other.pck.Container"),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Empty",
                                """
                                package some.other.pck;

                                public class Empty {
                                }
                                """),
                        JavaFileObjects.forSourceLines(
                                "some.other.pck.Container",
                                """
                                package some.other.pck;

                                public class Container {
                                    public static class Content {
                                        public String getLabel() {
                                            return null;
                                        }
                                    }
                                }
                                """));

        CompilationSubject.assertThat(compilation).succeeded();
        assertFalse(Files.exists(kotlinOutput.resolve("some/other/pck/EmptyMatcherDsl.kt")));
        assertGolden("without-properties", "some/other/pck/ContainerMatcherDsl.kt");
    }

    @Test
    @DisplayName("A type gets exactly one Kotlin file, named after its Java matcher")
    void aTypeGetsExactlyOneKotlinFile() throws IOException {
        compileWithKapt(configuration("some.other.pck.First"), simpleBean("First"));

        try (var files = Files.walk(kotlinOutput)) {
            assertEquals(
                    List.of(kotlinOutput.resolve("some/other/pck/FirstMatcherDsl.kt")),
                    files.filter(Files::isRegularFile).collect(Collectors.toList()));
        }
    }

    private static JavaFileObject simpleBean(final String name) {
        return JavaFileObjects.forSourceLines(
                "some.other.pck." + name,
                "package some.other.pck;",
                "public class " + name + " {",
                "    public String getName() { return null; }",
                "}");
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

    private Compilation compileWithKapt(final JavaFileObject... sources) {
        return Compiler.javac()
                .withOptions("-Akapt.kotlin.generated=" + kotlinOutput)
                .withProcessors(new MatcherGenerationProcessor(() -> NOW))
                .compile(Arrays.asList(sources));
    }

    private static String generatedContent(
            final Compilation compilation, final String qualifiedName) throws IOException {
        return compilation
                .generatedSourceFile(qualifiedName)
                .orElseThrow(() -> new AssertionError("No source generated for " + qualifiedName))
                .getCharContent(true)
                .toString();
    }

    private void assertGolden(final String scenario, final String relativeFile) throws IOException {
        final Path generated = kotlinOutput.resolve(relativeFile);
        if (!Files.isRegularFile(generated)) {
            throw new AssertionError("No Kotlin file written to " + relativeFile);
        }
        GoldenFiles.forModule()
                .assertGolden(
                        SCENARIOS + scenario + "/" + relativeFile, Files.readString(generated));
    }
}
