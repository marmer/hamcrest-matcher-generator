/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests everything the core decides, seen through the recording test backend registered behind the
 * SPI.
 */
class CoreProcessorIT {

    /**
     * 5.x detects {@code Object.getClass()} as a property, and the generated matchers rely on it.
     */
    private static final String CLASS_PROPERTY = "class:java.lang.Class<?>:getClass";

    @TempDir Path kotlinOutput;

    @BeforeEach
    @AfterEach
    void resetRecording() {
        RecordingBackend.reset();
        ProbingBackend.reset();
    }

    private static JavaFileObject generateMatchersConfiguration(final String configuredTypes) {
        return generateMatchersConfiguration("SomeConfiguration", configuredTypes);
    }

    private static JavaFileObject generateMatchersConfiguration(
            final String simpleName, final String configuredTypes) {
        return JavaFileObjects.forSourceLines(
                "some.pck." + simpleName,
                "package some.pck;",
                "import io.github.marmer.matchergenerator.annotations.GenerateMatchers;",
                "@GenerateMatchers(" + configuredTypes + ")",
                "public final class " + simpleName + " {",
                "}");
    }

    private static JavaFileObject configuration(final String configuredTypes) {
        return JavaFileObjects.forSourceLines(
                "some.pck.SomeConfiguration",
                "package some.pck;",
                "import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;",
                "@MatcherConfiguration(" + configuredTypes + ")",
                "public final class SomeConfiguration {",
                "}");
    }

    private static JavaFileObject prefixedConfiguration(
            final String configuredType, final String prefix) {
        return configuration(
                "value = "
                        + configuredType
                        + ", generation = "
                        + "@MatcherConfiguration.GenerationConfiguration(packageConfig = "
                        + "@MatcherConfiguration.GenerationConfiguration.PackageConfiguration(\""
                        + prefix
                        + "\"))");
    }

    /**
     * A javac with the given processor options, and the Kotlin output directory that every test
     * needs.
     */
    private Compiler javacWith(final String... options) {
        final List<String> all = new ArrayList<>();
        all.add("-Akapt.kotlin.generated=" + kotlinOutput);
        all.addAll(Arrays.asList(options));
        return Compiler.javac().withOptions(all);
    }

    private Compilation compile(final JavaFileObject... sources) {
        return compile(javacWith(), sources);
    }

    private static Compilation compile(final Compiler compiler, final JavaFileObject... sources) {
        return compiler.withProcessors(new MatcherGenerationProcessor())
                .compile(Arrays.asList(sources));
    }

    private static Compilation compile(
            final Compiler compiler,
            final Processor otherProcessor,
            final JavaFileObject... sources) {
        return compiler.withProcessors(new MatcherGenerationProcessor(), otherProcessor)
                .compile(Arrays.asList(sources));
    }

    private Compilation compileVerbose(final JavaFileObject... sources) {
        return compile(javacWith("-Amatchergenerator.verbose"), sources);
    }

    private static TypeModel onlyModel() {
        final List<TypeModel> models = RecordingBackend.models();
        assertEquals(1, models.size(), () -> "Recorded models: " + models);
        return models.get(0);
    }

    private static List<String> describe(final List<PropertyModel> properties) {
        return properties.stream()
                .map(
                        property ->
                                property.name() + ":" + property.type() + ":" + property.accessor())
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("A bean is described by its public non static getters")
    void aBeanIsDescribedByItsPublicNonStaticGetters() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        "package some.other.pck;",
                        "public class SimplePojo {",
                        "    private String field;",
                        "    public String getName() { return \"\"; }",
                        "    public boolean isActive() { return true; }",
                        "    public Boolean getWrapped() { return true; }",
                        "    public int getCount() { return 1; }",
                        "    public String notAProperty() { return \"\"; }",
                        "    public void getVoid() { }",
                        "    public String getWithParameter(int param) { return \"\"; }",
                        "    public boolean getBooleanWithGet() { return true; }",
                        "    public static String getStatic() { return \"\"; }",
                        "    protected String getProtected() { return \"\"; }",
                        "    String getPackagePrivate() { return \"\"; }",
                        "    private String getPrivate() { return \"\"; }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.SimplePojo\""), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals("some.other.pck.SimplePojo", model.qualifiedName());
        assertEquals("SimplePojo", model.simpleName());
        assertEquals("some.other.pck", model.packageName());
        assertEquals("some.other.pck", model.targetPackage());
        assertEquals(TypeModel.Kind.POJO, model.kind());
        assertEquals(List.of(), model.typeParameters());
        assertEquals(
                List.of(
                        "name:java.lang.String:getName",
                        "active:boolean:isActive",
                        "wrapped:java.lang.Boolean:getWrapped",
                        "count:int:getCount",
                        CLASS_PROPERTY),
                describe(model.properties()));
        assertEquals(
                List.of(
                        "name:UNSPECIFIED",
                        "active:NON_NULL",
                        "wrapped:UNSPECIFIED",
                        "count:NON_NULL",
                        "class:UNSPECIFIED"),
                describeNullability(model.properties()));
    }

    private static List<String> describeNullability(final List<PropertyModel> properties) {
        return properties.stream()
                .map(property -> property.name() + ":" + property.nullability())
                .collect(Collectors.toList());
    }

    private static final JavaFileObject JETBRAINS_NULLABLE =
            JavaFileObjects.forSourceLines(
                    "org.jetbrains.annotations.Nullable",
                    "package org.jetbrains.annotations;",
                    "public @interface Nullable {",
                    "}");

    private static final JavaFileObject JETBRAINS_NOT_NULL =
            JavaFileObjects.forSourceLines(
                    "org.jetbrains.annotations.NotNull",
                    "package org.jetbrains.annotations;",
                    "public @interface NotNull {",
                    "}");

    @Test
    @DisplayName("JSpecify annotations on the return type decide the nullability")
    void jSpecifyAnnotationsOnTheReturnTypeDecideTheNullability() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SpecifiedPojo",
                        "package some.other.pck;",
                        "import org.jspecify.annotations.NonNull;",
                        "import org.jspecify.annotations.Nullable;",
                        "public class SpecifiedPojo {",
                        "    public @Nullable String getMaybe() { return null; }",
                        "    public @NonNull String getSure() { return \"\"; }",
                        "    public String getUnknown() { return \"\"; }",
                        "    public @Nullable String[] getMaybeArrayElements() { return null; }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.SpecifiedPojo\""), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "maybe:NULLABLE",
                        "sure:NON_NULL",
                        "unknown:UNSPECIFIED",
                        "maybeArrayElements:UNSPECIFIED",
                        "class:UNSPECIFIED"),
                describeNullability(onlyModel().properties()));
    }

    @Test
    @DisplayName("Inside a NullMarked scope a property without Nullable is non null")
    void insideANullMarkedScopeAPropertyWithoutNullableIsNonNull() {
        final JavaFileObject marked =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.MarkedPojo",
                        "package some.other.pck;",
                        "import org.jspecify.annotations.NullMarked;",
                        "import org.jspecify.annotations.NullUnmarked;",
                        "import org.jspecify.annotations.Nullable;",
                        "@NullMarked",
                        "public class MarkedPojo {",
                        "    public String getName() { return \"\"; }",
                        "    public @Nullable String getMaybe() { return null; }",
                        "    public Inner getInner() { return null; }",
                        "    @NullUnmarked",
                        "    public String getUnmarked() { return \"\"; }",
                        "    public static class Inner {",
                        "        public String getInnerName() { return \"\"; }",
                        "    }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.MarkedPojo\""), marked);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(
                List.of(
                        "name:NON_NULL",
                        "maybe:NULLABLE",
                        "inner:NON_NULL",
                        "unmarked:UNSPECIFIED",
                        "class:UNSPECIFIED"),
                describeNullability(model.properties()));
        assertEquals(
                List.of("innerName:NON_NULL", "class:UNSPECIFIED"),
                describeNullability(model.nestedTypes().get(0).properties()));
    }

    @Test
    @DisplayName("A type variable is null agnostic inside a NullMarked scope")
    void aTypeVariableIsNullAgnosticInsideANullMarkedScope() {
        final JavaFileObject box =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Box",
                        "package some.other.pck;",
                        "import org.jspecify.annotations.NullMarked;",
                        "import org.jspecify.annotations.Nullable;",
                        "@NullMarked",
                        "public class Box<T extends @Nullable Object> {",
                        "    public T getContent() { return null; }",
                        "    public @Nullable T getMaybeContent() { return null; }",
                        "    public String getLabel() { return \"\"; }",
                        "}");

        final Compilation compilation = compile(configuration("\"some.other.pck.Box\""), box);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "content:UNSPECIFIED",
                        "maybeContent:NULLABLE",
                        "label:NON_NULL",
                        "class:UNSPECIFIED"),
                describeNullability(onlyModel().properties()));
    }

    @Test
    @DisplayName("A NullMarked package makes its properties non null")
    void aNullMarkedPackageMakesItsPropertiesNonNull() {
        final JavaFileObject packageInfo =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.package-info",
                        "@NullMarked",
                        "package some.other.pck;",
                        "import org.jspecify.annotations.NullMarked;");
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.PackagePojo",
                        "package some.other.pck;",
                        "public class PackagePojo {",
                        "    public String getName() { return \"\"; }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.PackagePojo\""), packageInfo, bean);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of("name:NON_NULL", "class:UNSPECIFIED"),
                describeNullability(onlyModel().properties()));
    }

    @Test
    @DisplayName("Kotlin nullability annotations on the accessor decide the nullability")
    void kotlinNullabilityAnnotationsOnTheAccessorDecideTheNullability() {
        final JavaFileObject kotlinProbe =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.KotlinProbe",
                        "package some.other.pck;",
                        "import org.jetbrains.annotations.NotNull;",
                        "import org.jetbrains.annotations.Nullable;",
                        "@kotlin.Metadata",
                        "public class KotlinProbe {",
                        "    @NotNull public String getName() { return \"\"; }",
                        "    @Nullable public String getMaybe() { return null; }",
                        "    @Nullable public Boolean isFlag() { return null; }",
                        "    public boolean getEnabled() { return true; }",
                        "}");

        final Compilation compilation =
                compile(
                        configuration("\"some.other.pck.KotlinProbe\""),
                        kotlinProbe,
                        KOTLIN_METADATA,
                        JETBRAINS_NULLABLE,
                        JETBRAINS_NOT_NULL);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "name:NON_NULL",
                        "maybe:NULLABLE",
                        "isFlag:NULLABLE",
                        "enabled:NON_NULL",
                        "class:UNSPECIFIED"),
                describeNullability(onlyModel().properties()));
    }

    @Test
    @DisplayName("Record components carry their nullability")
    void recordComponentsCarryTheirNullability() {
        final JavaFileObject record =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SpecifiedRecord",
                        "package some.other.pck;",
                        "import org.jspecify.annotations.Nullable;",
                        "public record SpecifiedRecord(@Nullable String maybe, String unknown, int count) {",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.SpecifiedRecord\""), record);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "maybe:NULLABLE",
                        "unknown:UNSPECIFIED",
                        "count:NON_NULL",
                        "class:UNSPECIFIED"),
                describeNullability(onlyModel().properties()));
    }

    @Test
    @DisplayName("Backends learn whether JSpecify resolves at compile time")
    void backendsLearnWhetherJSpecifyResolvesAtCompileTime() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");

        compile(configuration("\"some.other.pck.SomePojo\""), bean);
        compile(
                Compiler.javac().withClasspath(classPathWithout("jspecify")),
                configuration("\"some.other.pck.SomePojo\""),
                bean);

        assertEquals(
                List.of(true, false),
                RecordingBackend.contexts().stream()
                        .map(GenerationContext::jspecifyAvailable)
                        .toList());
    }

    @Test
    @DisplayName("A backend error fails the compilation once, at the annotation of the type")
    void aBackendErrorFailsTheCompilationOnceAtTheAnnotationOfTheType() {
        final JavaFileObject configuration =
                generateMatchersConfiguration("\"some.other.pck.Probed\"");
        final JavaFileObject probed =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Probed",
                        "package some.other.pck;",
                        "public class Probed {",
                        "}");

        final Compilation compilation = compile(configuration, probed);

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining(ProbingBackend.ERROR + " for Probed")
                .inFile(configuration)
                .onLine(3);
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
    }

    @Test
    @DisplayName("A backend learns the inherited methods of a class, and none of a missing class")
    void aBackendLearnsTheInheritedMethodsOfAClassAndNoneOfAMissingClass() {
        final JavaFileObject probed =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Probed",
                        "package some.other.pck;",
                        "public class Probed {",
                        "}");

        compile(generateMatchersConfiguration("\"some.other.pck.Probed\""), probed);

        final List<Set<String>> inherited = ProbingBackend.inheritedOfObject();
        assertEquals(1, inherited.size());
        assertTrue(inherited.get(0).containsAll(Set.of("getClass", "hashCode", "toString")));
        assertEquals(List.of(Set.of()), ProbingBackend.inheritedOfMissingClass());
    }

    @Test
    @DisplayName("A private method of the class is no inherited method name")
    void aPrivateMethodOfTheClassIsNoInheritedMethodName() {
        final JavaFileObject probed =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Probed",
                        "package some.other.pck;",
                        "public class Probed {",
                        "}");

        compile(generateMatchersConfiguration("\"some.other.pck.Probed\""), probed);

        final Set<String> own = ProbingBackend.inheritedOfItself().get(0);
        assertTrue(own.contains("generate"), () -> "Names: " + own);
        assertFalse(own.contains("hiddenFromSubclasses"), () -> "Names: " + own);
    }

    @Test
    @DisplayName("A backend learns that Kotlin output is available when KAPT names the directory")
    void aBackendLearnsThatKotlinOutputIsAvailableWhenKaptNamesTheDirectory() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");

        compile(configuration("\"some.other.pck.SomePojo\""), bean);
        final boolean withKapt = RecordingBackend.contexts().get(0).kotlinOutputAvailable();
        RecordingBackend.reset();
        compile(Compiler.javac(), configuration("\"some.other.pck.SomePojo\""), bean);
        final boolean withoutKapt = RecordingBackend.contexts().get(0).kotlinOutputAvailable();

        assertTrue(withKapt, "with the KAPT directory");
        assertFalse(withoutKapt, "without the KAPT directory");
    }

    @Test
    @DisplayName("A backend note is a compiler note without a verbose option")
    void aBackendNoteIsACompilerNoteWithoutAVerboseOption() {
        final JavaFileObject noted =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Noted",
                        "package some.other.pck;",
                        "public class Noted {",
                        "}");

        final Compilation compilation =
                compile(generateMatchersConfiguration("\"some.other.pck.Noted\""), noted);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadNoteCount(1);
        CompilationSubject.assertThat(compilation).hadNoteContaining(ProbingBackend.NOTE);
    }

    private static JavaFileObject summarizedType(final String simpleName) {
        return JavaFileObjects.forSourceLines(
                "some.summarized." + simpleName,
                "package some.summarized;",
                "public class " + simpleName + " {",
                "    public String getName() { return null; }",
                "}");
    }

    private static long countGenerated(final Compilation compilation, final String fileName) {
        return compilation.generatedSourceFiles().stream()
                .filter(source -> source.getName().endsWith("/" + fileName))
                .count();
    }

    private static String summary(final Compilation compilation) throws IOException {
        return compilation
                .generatedSourceFile("some.summarized.PackageSummary")
                .orElseThrow()
                .getCharContent(true)
                .toString();
    }

    @Test
    @DisplayName(
            "Two configurations of one round share the files a backend writes once per package")
    void twoConfigurationsOfOneRoundShareTheFilesABackendWritesOncePerPackage() throws IOException {
        final JavaFileObject other =
                JavaFileObjects.forSourceLines(
                        "some.pck.OtherConfiguration",
                        "package some.pck;",
                        "import io.github.marmer.matchergenerator.annotations.GenerateMatchers;",
                        "@GenerateMatchers(\"some.summarized.Second\")",
                        "public final class OtherConfiguration {",
                        "}");

        final Compilation compilation =
                compile(
                        generateMatchersConfiguration("\"some.summarized.First\""),
                        other,
                        summarizedType("First"),
                        summarizedType("Second"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(1, countGenerated(compilation, "PackageSummary.java"));
        final String summary = summary(compilation);
        assertTrue(
                summary.contains("[some.summarized.First, some.summarized.Second]"),
                () -> "Summary: " + summary);
    }

    @Test
    @DisplayName(
            "A file a backend writes once per package is not written again by a later round, which is a warning")
    void aFileABackendWritesOncePerPackageIsNotWrittenAgainByALaterRoundWhichIsAWarning() {
        final Compilation compilation =
                compile(
                        javacWith(),
                        new LateConfigurationProcessor(
                                "some.summarized.LateConfiguration", "some.summarized.Second"),
                        generateMatchersConfiguration("\"some.summarized.First\""),
                        summarizedType("First"),
                        summarizedType("Second"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(1, countGenerated(compilation, "PackageSummary.java"));
        assertEquals(1, countGenerated(compilation, "SecondRecorded.java"));
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "some.summarized.PackageSummary was already written in an earlier"
                                + " processing round and is not written again. It does not know"
                                + " the types generated in this round: some.summarized.Second");
        CompilationSubject.assertThat(compilation).hadWarningCount(1);
    }

    @Test
    @DisplayName("A class in the own output of an earlier build does not count as outside of it")
    void aClassInTheOwnOutputOfAnEarlierBuildDoesNotCountAsOutsideOfIt(@TempDir final Path output)
            throws IOException {
        final JavaFileObject located =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Located",
                        "package some.other.pck;",
                        "public class Located {",
                        "}");
        final JavaFileObject configuration =
                generateMatchersConfiguration("\"some.other.pck.Located\"");

        buildInto(output, configuration, located);
        ProbingBackend.reset();
        buildInto(output, configuration, located);

        assertEquals(
                List.of(
                        "java.lang.Object=true",
                        "no.such.Class=false",
                        "some.other.pck.Located=false"),
                ProbingBackend.locations());
    }

    /** Compiles like a build tool, with the output directory on the class path as well. */
    private void buildInto(final Path output, final JavaFileObject... sources) throws IOException {
        final javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        final javax.tools.DiagnosticCollector<JavaFileObject> diagnostics =
                new javax.tools.DiagnosticCollector<>();
        try (javax.tools.StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, null)) {
            final javax.tools.JavaCompiler.CompilationTask task =
                    compiler.getTask(
                            null,
                            fileManager,
                            diagnostics,
                            List.of(
                                    "-d",
                                    output.toString(),
                                    "-classpath",
                                    System.getProperty("java.class.path")
                                            + File.pathSeparator
                                            + output,
                                    "-Akapt.kotlin.generated=" + kotlinOutput),
                            null,
                            Arrays.asList(sources));
            task.setProcessors(List.of(new MatcherGenerationProcessor()));
            assertTrue(task.call(), () -> "Diagnostics: " + diagnostics.getDiagnostics());
        }
    }

    @Test
    @DisplayName("A backend verbose note is a compiler note only with the verbose option")
    void aBackendVerboseNoteIsACompilerNoteOnlyWithTheVerboseOption() {
        final JavaFileObject chatty =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Chatty",
                        "package some.other.pck;",
                        "public class Chatty {",
                        "}");
        final JavaFileObject configuration =
                generateMatchersConfiguration("\"some.other.pck.Chatty\"");

        final Compilation quiet = compile(configuration, chatty);
        final Compilation verbose = compileVerbose(configuration, chatty);

        CompilationSubject.assertThat(quiet).succeeded();
        CompilationSubject.assertThat(quiet).hadNoteCount(0);
        CompilationSubject.assertThat(verbose).succeeded();
        CompilationSubject.assertThat(verbose).hadNoteContaining(ProbingBackend.VERBOSE_NOTE);
    }

    @Test
    @DisplayName(
            "A backend warning is a compiler warning at the annotation, and the compilation succeeds")
    void aBackendWarningIsACompilerWarningAtTheAnnotationAndTheCompilationSucceeds() {
        final JavaFileObject configuration =
                generateMatchersConfiguration("\"some.other.pck.Warned\"");
        final JavaFileObject warned =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Warned",
                        "package some.other.pck;",
                        "public class Warned {",
                        "    public String getName() { return null; }",
                        "}");

        final Compilation compilation = compile(configuration, warned);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(ProbingBackend.WARNING)
                .inFile(configuration)
                .onLine(3);
        CompilationSubject.assertThat(compilation).hadWarningCount(1);
    }

    private static List<File> classPathWithout(final String fragment) {
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .filter(entry -> !entry.contains(fragment))
                .map(File::new)
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("A Kotlin class is described by its Kotlin getters, whatever their return type")
    void aKotlinClassIsDescribedByItsKotlinGetters() {
        final JavaFileObject kotlinProbe =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.KotlinProbe",
                        "package some.other.pck;",
                        "@kotlin.Metadata",
                        "public class KotlinProbe {",
                        "    public boolean getEnabled() { return true; }",
                        "    public Boolean isX() { return true; }",
                        "    public String isLabel() { return \"\"; }",
                        "    public boolean isPrimitive() { return true; }",
                        "    public String notAProperty() { return \"\"; }",
                        "}");

        final Compilation compilation =
                compile(
                        configuration("\"some.other.pck.KotlinProbe\""),
                        kotlinProbe,
                        KOTLIN_METADATA);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "enabled:boolean:getEnabled",
                        "isX:java.lang.Boolean:isX",
                        "isLabel:java.lang.String:isLabel",
                        "primitive:boolean:isPrimitive",
                        CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("In a Kotlin class an upper case letter must follow the is and get prefix")
    void kotlinAccessorsNeedAnUpperCaseLetterAfterThePrefix() {
        final JavaFileObject kotlinProbe =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.KotlinProbe",
                        "package some.other.pck;",
                        "@kotlin.Metadata",
                        "public class KotlinProbe {",
                        "    public String getName() { return \"\"; }",
                        "    public String getaway() { return \"\"; }",
                        "    public boolean isActive() { return true; }",
                        "    public boolean isolate() { return true; }",
                        "    public String isolated() { return \"\"; }",
                        "}");

        final Compilation compilation =
                compile(
                        configuration("\"some.other.pck.KotlinProbe\""),
                        kotlinProbe,
                        KOTLIN_METADATA);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of("name:java.lang.String:getName", "active:boolean:isActive", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("Kotlin getters whose names are not Java identifiers are skipped (QS-U4)")
    void kotlinGettersWithMangledNamesAreSkipped(@TempDir final Path classes) throws IOException {
        final JavaFileObject mangled =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Mangled",
                        "package some.other.pck;",
                        "@kotlin.Metadata",
                        "public class Mangled {",
                        "    public String getPlain() { return \"\"; }",
                        "    public String getXyzQ() { return \"\"; }",
                        "}");
        final Compilation compiledMangled = Compiler.javac().compile(mangled, KOTLIN_METADATA);
        CompilationSubject.assertThat(compiledMangled).succeeded();
        writeClassFile(compiledMangled, classes, "some.other.pck.Mangled", "getXyzQ", "getX-zQ");
        writeClassFile(compiledMangled, classes, "kotlin.Metadata");

        final Compilation compilation =
                compile(
                        javacWith().withClasspath(classPathWith(classes)),
                        configuration("\"some.other.pck.Mangled\""));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of("plain:java.lang.String:getPlain", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    /**
     * The directory with the copied class files, followed by the test class path that
     * compile-testing would use.
     */
    private static List<File> classPathWith(final Path classes) {
        final List<File> entries = new ArrayList<>();
        entries.add(classes.toFile());
        Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(File::new)
                .forEach(entries::add);
        return entries;
    }

    /**
     * Copies a compiled class file into the class path, optionally renaming one method inside it.
     * Kotlin emits names such as {@code getX-abc} for mangled accessors, which javac refuses to
     * compile from source. The rename keeps the length, so the constant pool stays valid.
     */
    private static void writeClassFile(
            final Compilation compilation, final Path classes, final String name)
            throws IOException {
        writeClassContent(compilation, classes, name, UnaryOperator.identity());
    }

    private static void writeClassFile(
            final Compilation compilation,
            final Path classes,
            final String name,
            final String from,
            final String to)
            throws IOException {
        writeClassContent(compilation, classes, name, content -> content.replace(from, to));
    }

    private static void writeClassContent(
            final Compilation compilation,
            final Path classes,
            final String name,
            final UnaryOperator<String> rename)
            throws IOException {
        final String path = name.replace('.', '/') + ".class";
        final byte[] bytes =
                compilation
                        .generatedFile(StandardLocation.CLASS_OUTPUT, path)
                        .orElseThrow()
                        .openInputStream()
                        .readAllBytes();
        final String content = new String(bytes, StandardCharsets.ISO_8859_1);
        final String renamed = rename.apply(content);
        final Path target = classes.resolve(path);
        Files.createDirectories(target.getParent());
        Files.write(target, renamed.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    @DisplayName("A record is described by its components")
    void aRecordIsDescribedByItsComponents() {
        final JavaFileObject record =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.PersonRecord",
                        "package some.other.pck;",
                        "public record PersonRecord(String name, int age) {",
                        "    public String getNickname() { return \"\"; }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.PersonRecord\""), record);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(TypeModel.Kind.RECORD, model.kind());
        assertEquals(
                List.of(
                        "name:java.lang.String:name",
                        "age:int:age",
                        "nickname:java.lang.String:getNickname",
                        CLASS_PROPERTY),
                describe(model.properties()));
    }

    @Test
    @DisplayName("Record components starting with is or get are properties named as the component")
    void recordComponentsStartingWithIsOrGetArePropertiesNamedAsTheComponent() {
        final JavaFileObject record =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.BookRecord",
                        "package some.other.pck;",
                        "public record BookRecord(String isbn, String getaway, String name) {}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.BookRecord\""), record);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(TypeModel.Kind.RECORD, model.kind());
        assertEquals(
                List.of(
                        "isbn:java.lang.String:isbn",
                        "getaway:java.lang.String:getaway",
                        "name:java.lang.String:name",
                        CLASS_PROPERTY),
                describe(model.properties()));
    }

    @Test
    @DisplayName("A bean getter named getIsbn is the property isbn")
    void aBeanGetterNamedGetIsbnIsThePropertyIsbn() {
        final JavaFileObject pojo =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.IsbnPojo",
                        "package some.other.pck;",
                        "public class IsbnPojo {",
                        "    public String getIsbn() { return \"\"; }",
                        "}");

        final Compilation compilation = compile(configuration("\"some.other.pck.IsbnPojo\""), pojo);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of("isbn:java.lang.String:getIsbn", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("Inherited properties of super classes and interfaces are part of the model")
    void inheritedPropertiesArePartOfTheModel() {
        final JavaFileObject base =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Base",
                        "package some.other.pck;",
                        "public class Base {",
                        "    public String getFromBase() { return \"\"; }",
                        "}");
        final JavaFileObject iface =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Named",
                        "package some.other.pck;",
                        "public interface Named {",
                        "    String getFromInterface();",
                        "}");
        final JavaFileObject child =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Child",
                        "package some.other.pck;",
                        "public class Child extends Base implements Named {",
                        "    public String getOwn() { return \"\"; }",
                        "    public String getFromInterface() { return \"\"; }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.Child\""), base, iface, child);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "own:java.lang.String:getOwn",
                        "fromInterface:java.lang.String:getFromInterface",
                        "fromBase:java.lang.String:getFromBase",
                        CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("Generic types keep their type parameters and generic property types")
    void genericTypesKeepTheirTypeParameters() {
        final JavaFileObject generic =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Box",
                        "package some.other.pck;",
                        "import java.util.List;",
                        "public class Box<T, U extends Number> {",
                        "    public T getContent() { return null; }",
                        "    public List<? extends U> getNumbers() { return null; }",
                        "}");

        final Compilation compilation = compile(configuration("\"some.other.pck.Box\""), generic);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(List.of("T", "U"), model.typeParameters());
        assertEquals("some.other.pck.Box<T,U>", model.type().toString());
        assertEquals(
                List.of(
                        "content:T:getContent",
                        "numbers:java.util.List<? extends U>:getNumbers",
                        CLASS_PROPERTY),
                describe(model.properties()));
    }

    @Test
    @DisplayName("An inherited generic property has the type argument its subtype gives")
    void inheritedGenericPropertiesHaveTheTypeArgumentOfTheSubtype() {
        final JavaFileObject base =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Base",
                        "package some.other.pck;",
                        "import java.util.List;",
                        "public class Base<T> {",
                        "    public T getValue() { return null; }",
                        "    public List<T> getItems() { return null; }",
                        "}");
        final JavaFileObject sub =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Sub",
                        "package some.other.pck;",
                        "public class Sub extends Base<String> {}");

        final Compilation compilation = compile(configuration("\"some.other.pck.Sub\""), base, sub);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "value:java.lang.String:getValue",
                        "items:java.util.List<java.lang.String>:getItems",
                        CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("An inherited generic property is resolved through every level of the hierarchy")
    void inheritedGenericPropertiesAreResolvedThroughEveryLevel() {
        final JavaFileObject base =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Base",
                        "package some.other.pck;",
                        "public class Base<T> {",
                        "    public T getValue() { return null; }",
                        "}");
        final JavaFileObject mid =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Mid",
                        "package some.other.pck;",
                        "public class Mid<U> extends Base<U> {}");
        final JavaFileObject leaf =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Leaf",
                        "package some.other.pck;",
                        "public class Leaf extends Mid<Integer> {}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.Leaf\""), base, mid, leaf);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of("value:java.lang.Integer:getValue", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("An inherited generic interface property has the type argument of the class")
    void inheritedGenericInterfacePropertiesHaveTheTypeArgumentOfTheClass() {
        final JavaFileObject named =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Named",
                        "package some.other.pck;",
                        "public interface Named<T> {",
                        "    default T getName() { return null; }",
                        "}");
        final JavaFileObject person =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Person",
                        "package some.other.pck;",
                        "public class Person implements Named<String> {}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.Person\""), named, person);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(CLASS_PROPERTY, "name:java.lang.String:getName"),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("An inherited generic property of a generic type keeps its type variable")
    void inheritedGenericPropertiesOfAGenericTypeKeepTheTypeVariable() {
        final JavaFileObject base =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Base",
                        "package some.other.pck;",
                        "import java.util.List;",
                        "public class Base<T> {",
                        "    public T getValue() { return null; }",
                        "    public List<T> getItems() { return null; }",
                        "}");
        final JavaFileObject box =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Box",
                        "package some.other.pck;",
                        "public class Box<T> extends Base<T> {}");

        final Compilation compilation = compile(configuration("\"some.other.pck.Box\""), base, box);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(List.of("T"), model.typeParameters());
        assertEquals(
                List.of("value:T:getValue", "items:java.util.List<T>:getItems", CLASS_PROPERTY),
                describe(model.properties()));
    }

    @Test
    @DisplayName("An inherited generic getter of a record has the type argument of the record")
    void anInheritedGenericGetterOfARecordHasTheTypeArgumentOfTheRecord() {
        final JavaFileObject named =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Named",
                        "package some.other.pck;",
                        "public interface Named<T> {",
                        "    default T getName() { return null; }",
                        "}");
        final JavaFileObject person =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Person",
                        "package some.other.pck;",
                        "public record Person() implements Named<String> {}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.Person\""), named, person);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(TypeModel.Kind.RECORD, model.kind());
        assertEquals(
                List.of(CLASS_PROPERTY, "name:java.lang.String:getName"),
                describe(model.properties()));
    }

    @Test
    @DisplayName("An inherited abstract generic getter has the type argument of the abstract class")
    void anInheritedAbstractGenericGetterHasTheTypeArgumentOfTheClass() {
        final JavaFileObject named =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Named",
                        "package some.other.pck;",
                        "public interface Named<T> {",
                        "    T getName();",
                        "}");
        final JavaFileObject person =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Person",
                        "package some.other.pck;",
                        "public abstract class Person implements Named<String> {}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.Person\""), named, person);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(CLASS_PROPERTY, "name:java.lang.String:getName"),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("A raw supertype does not throw and its type variables are not kept")
    void aRawSupertypeDoesNotThrowAndItsTypeVariablesAreNotKept() {
        final JavaFileObject base =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Base",
                        "package some.other.pck;",
                        "import java.util.List;",
                        "public class Base<T> {",
                        "    public T getValue() { return null; }",
                        "    public List<T> getItems() { return null; }",
                        "}");
        final JavaFileObject sub =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Sub",
                        "package some.other.pck;",
                        "public class Sub extends Base {}");

        final Compilation compilation = compile(configuration("\"some.other.pck.Sub\""), base, sub);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "value:java.lang.Object:getValue",
                        "items:java.util.List:getItems",
                        CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName(
            "Public member types are nested models and other member types are skipped with a note")
    void publicMemberTypesAreNestedModels() {
        final JavaFileObject outer =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Outer",
                        "package some.other.pck;",
                        "public class Outer {",
                        "    public String getOuterProperty() { return \"\"; }",
                        "    public static class Inner {",
                        "        public String getInnerProperty() { return \"\"; }",
                        "    }",
                        "    class Hidden {",
                        "    }",
                        "}");

        final Compilation compilation =
                compileVerbose(configuration("\"some.other.pck.Outer\""), outer);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(1, model.nestedTypes().size());
        final TypeModel inner = model.nestedTypes().get(0);
        assertEquals("some.other.pck.Outer.Inner", inner.qualifiedName());
        assertEquals("Inner", inner.simpleName());
        assertEquals(
                List.of("innerProperty:java.lang.String:getInnerProperty", CLASS_PROPERTY),
                describe(inner.properties()));
        CompilationSubject.assertThat(compilation)
                .hadNoteContaining(
                        "Matcher generation skipped for non public type: some.other.pck.Outer.Hidden");
    }

    @Test
    @DisplayName("A configured package prefix decides the target package")
    void aConfiguredPackagePrefixDecidesTheTargetPackage() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");
        final Compilation compilation =
                compile(prefixedConfiguration("\"some.other.pck.SomePojo\"", "prefix."), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals("some.other.pck", model.packageName());
        assertEquals("prefix.some.other.pck", model.targetPackage());
        assertTrue(
                compilation
                        .generatedSourceFile("prefix.some.other.pck.SomePojoRecorded")
                        .isPresent());
    }

    @Test
    @DisplayName("Every trailing dot of a configured package prefix is dropped")
    void everyTrailingDotOfAConfiguredPackagePrefixIsDropped() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");
        final Compilation compilation =
                compile(prefixedConfiguration("\"some.other.pck.SomePojo\"", "prefix..."), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals("prefix.some.other.pck", onlyModel().targetPackage());
    }

    /**
     * A configuration of either annotation with the given value and package prefix. The annotation
     * is on line 3.
     */
    private static JavaFileObject packagePrefixedConfiguration(
            final boolean deprecatedAlias, final String configuredTypes, final String prefix) {
        final String annotation = deprecatedAlias ? "MatcherConfiguration" : "GenerateMatchers";
        return JavaFileObjects.forSourceLines(
                "some.pck.SomeConfiguration",
                "package some.pck;",
                "import "
                        + (deprecatedAlias
                                ? "io.github.marmer.testutils.generators.beanmatcher.dependencies."
                                : "io.github.marmer.matchergenerator.annotations.")
                        + annotation
                        + ";",
                "@"
                        + annotation
                        + "(value = "
                        + configuredTypes
                        + ", generation = @"
                        + annotation
                        + ".GenerationConfiguration(packageConfig = @"
                        + annotation
                        + ".GenerationConfiguration.PackageConfiguration(\""
                        + prefix
                        + "\")))",
                "public final class SomeConfiguration {",
                "}");
    }

    private static JavaFileObject packagePrivateBean(
            final String packageName, final String simpleName) {
        return JavaFileObjects.forSourceLines(
                packageName + "." + simpleName,
                "package " + packageName + ";",
                "class " + simpleName + " {",
                "    public String getName() { return \"\"; }",
                "}");
    }

    /** The 1-based column at which the quoted name starts on the given line of the source. */
    private static long columnOfQuotedName(
            final JavaFileObject source, final int line, final String quotedName)
            throws IOException {
        return source.getCharContent(true)
                        .toString()
                        .lines()
                        .skip(line - 1L)
                        .findFirst()
                        .orElseThrow()
                        .indexOf(quotedName)
                + 1L;
    }

    @Test
    @DisplayName(
            "A package entry with a package prefix skips a non public top level type and generates the public ones")
    void aNonPublicTopLevelTypeIsNotGeneratedIntoAnotherPackage() {
        final Compilation compilation =
                compile(
                        packagePrefixedConfiguration(false, "\"some.other.pck\"", "prefix."),
                        packagePrivateBean("some.other.pck", "Hidden"),
                        bean("some.other.pck", "Visible"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of("some.other.pck.Visible"), recordedModelNames());
        assertTrue(
                compilation
                        .generatedSourceFile("prefix.some.other.pck.VisibleRecorded")
                        .isPresent());
        assertFalse(
                compilation
                        .generatedSourceFile("prefix.some.other.pck.HiddenRecorded")
                        .isPresent());
    }

    @Test
    @DisplayName(
            "A skipped non public top level type is a verbose note, which only the verbose option prints")
    void aSkippedNonPublicTopLevelTypeIsAVerboseNote() {
        final JavaFileObject configuration =
                packagePrefixedConfiguration(false, "\"some.other.pck\"", "prefix.");
        final JavaFileObject hidden = packagePrivateBean("some.other.pck", "Hidden");

        final Compilation verbose = compileVerbose(configuration, hidden);
        final Compilation quiet = compile(configuration, hidden);

        CompilationSubject.assertThat(verbose).succeeded();
        CompilationSubject.assertThat(verbose)
                .hadNoteContaining(
                        "Matcher generation skipped for non public type: some.other.pck.Hidden")
                .inFile(hidden)
                .onLine(2);
        CompilationSubject.assertThat(quiet).succeeded();
        assertEquals(
                List.of(),
                quiet.notes().stream()
                        .map(d -> d.getMessage(null))
                        .filter(message -> message.contains("non public type"))
                        .collect(Collectors.toList()));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A configured non public top level type that a package prefix moves is one error at its name, on both annotations")
    void aConfiguredNonPublicTopLevelTypeIsOneErrorAtItsName(final boolean deprecatedAlias)
            throws IOException {
        final JavaFileObject configuration =
                packagePrefixedConfiguration(
                        deprecatedAlias, "\"some.other.pck.Hidden\"", "prefix.");

        final Compilation compilation =
                compile(configuration, packagePrivateBean("some.other.pck", "Hidden"));

        CompilationSubject.assertThat(compilation).failed();
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining(
                        "Type 'some.other.pck.Hidden' is not public and cannot be used from the target package 'prefix.some.other.pck'")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Hidden\""));
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        assertEquals(List.of(), RecordingBackend.models());
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A configured nested type of a non public top level type is one error at its name, naming that top level type, on both annotations")
    void aConfiguredNestedTypeOfANonPublicTopLevelTypeIsOneError(final boolean deprecatedAlias)
            throws IOException {
        final JavaFileObject configuration =
                packagePrefixedConfiguration(
                        deprecatedAlias, "\"some.other.pck.Outer.Inner\"", "prefix.");
        final JavaFileObject outer =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Outer",
                        "package some.other.pck;",
                        "class Outer {",
                        "    public static class Inner {",
                        "        public String getName() { return \"\"; }",
                        "    }",
                        "}");

        final Compilation compilation = compile(configuration, outer);

        CompilationSubject.assertThat(compilation).failed();
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining(
                        "Type 'some.other.pck.Outer' is not public and cannot be used from the target package 'prefix.some.other.pck'")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Outer.Inner\""));
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName(
            "Naming a non public top level type and its nested type is one error, at the value that names the type first")
    void aNonPublicTopLevelTypeNamedWithItsNestedTypeIsOneErrorAtTheFirstValue()
            throws IOException {
        final JavaFileObject outer =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Outer",
                        "package some.other.pck;",
                        "class Outer {",
                        "    public static class Inner {",
                        "        public String getName() { return \"\"; }",
                        "    }",
                        "}");
        final JavaFileObject outerFirst =
                packagePrefixedConfiguration(
                        false,
                        "{\"some.other.pck.Outer\", \"some.other.pck.Outer.Inner\"}",
                        "prefix.");

        final Compilation outerFirstCompilation = compile(outerFirst, outer);

        CompilationSubject.assertThat(outerFirstCompilation).failed();
        CompilationSubject.assertThat(outerFirstCompilation)
                .hadErrorContaining(
                        "Type 'some.other.pck.Outer' is not public and cannot be used from the target package 'prefix.some.other.pck'")
                .inFile(outerFirst)
                .onLine(3)
                .atColumn(columnOfQuotedName(outerFirst, 3, "\"some.other.pck.Outer\""));
        assertEquals(
                1,
                outerFirstCompilation.errors().size(),
                () -> "Errors: " + outerFirstCompilation.errors());

        RecordingBackend.reset();
        final JavaFileObject innerFirst =
                packagePrefixedConfiguration(
                        false,
                        "{\"some.other.pck.Outer.Inner\", \"some.other.pck.Outer\"}",
                        "prefix.");

        final Compilation innerFirstCompilation = compile(innerFirst, outer);

        CompilationSubject.assertThat(innerFirstCompilation).failed();
        CompilationSubject.assertThat(innerFirstCompilation)
                .hadErrorContaining(
                        "Type 'some.other.pck.Outer' is not public and cannot be used from the target package 'prefix.some.other.pck'")
                .inFile(innerFirst)
                .onLine(3)
                .atColumn(columnOfQuotedName(innerFirst, 3, "\"some.other.pck.Outer.Inner\""));
        assertEquals(
                1,
                innerFirstCompilation.errors().size(),
                () -> "Errors: " + innerFirstCompilation.errors());
    }

    @Test
    @DisplayName(
            "A skipped non public nested type is a verbose note at its declaration, as a top level type is")
    void aSkippedNonPublicNestedTypeIsANoteAtItsDeclaration() {
        final JavaFileObject configuration = configuration("\"some.other.pck.Outer\"");
        final JavaFileObject outer =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Outer",
                        "package some.other.pck;",
                        "public class Outer {",
                        "    public String getName() { return \"\"; }",
                        "    class Hidden {",
                        "    }",
                        "}");

        final Compilation verbose = compileVerbose(configuration, outer);

        CompilationSubject.assertThat(verbose).succeeded();
        CompilationSubject.assertThat(verbose)
                .hadNoteContaining(
                        "Matcher generation skipped for non public type: some.other.pck.Outer.Hidden")
                .inFile(outer)
                .onLine(4);
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "An excluded non public top level type that a package prefix moves is neither an error nor a note, on both annotations")
    void anExcludedNonPublicTopLevelTypeIsNeitherAnErrorNorANote(final boolean deprecatedAlias) {
        final String annotation = deprecatedAlias ? "MatcherConfiguration" : "GenerateMatchers";
        final JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        "package some.pck;",
                        "import "
                                + (deprecatedAlias
                                        ? "io.github.marmer.testutils.generators.beanmatcher.dependencies."
                                        : "io.github.marmer.matchergenerator.annotations.")
                                + annotation
                                + ";",
                        "@"
                                + annotation
                                + "(value = \"some.other.pck.Hidden\", exclude = \"some.other.pck.Hidden\", generation = @"
                                + annotation
                                + ".GenerationConfiguration(packageConfig = @"
                                + annotation
                                + ".GenerationConfiguration.PackageConfiguration(\"prefix.\")))",
                        "public final class SomeConfiguration {",
                        "}");

        final Compilation compilation =
                compileVerbose(configuration, packagePrivateBean("some.other.pck", "Hidden"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of(),
                compilation.notes().stream()
                        .map(d -> d.getMessage(null))
                        .filter(message -> message.contains("non public type"))
                        .collect(Collectors.toList()));
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName(
            "A non public top level type stays generated when no package prefix moves it, named or picked up")
    void aNonPublicTopLevelTypeWithoutAPackagePrefixIsGenerated() {
        final Compilation picked =
                compile(
                        generateMatchersConfiguration("\"some.other.pck\""),
                        packagePrivateBean("some.other.pck", "Hidden"),
                        bean("some.other.pck", "Visible"));

        CompilationSubject.assertThat(picked).succeeded();
        assertEquals(0, picked.warnings().size(), () -> "Warnings: " + picked.warnings());
        assertEquals(
                List.of("some.other.pck.Hidden", "some.other.pck.Visible"),
                recordedModelNames().stream().sorted().collect(Collectors.toList()));
        assertTrue(picked.generatedSourceFile("some.other.pck.HiddenRecorded").isPresent());

        RecordingBackend.reset();
        final Compilation named =
                compile(
                        generateMatchersConfiguration("\"some.other.pck.Hidden\""),
                        packagePrivateBean("some.other.pck", "Hidden"));

        CompilationSubject.assertThat(named).succeeded();
        assertEquals(List.of("some.other.pck.Hidden"), recordedModelNames());
    }

    private static JavaFileObject exclusionConfiguration(
            final boolean deprecatedAlias,
            final String configuredTypes,
            final String excludedTypes) {
        return JavaFileObjects.forSourceLines(
                "some.pck.SomeConfiguration",
                "package some.pck;",
                deprecatedAlias
                        ? "import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;"
                        : "import io.github.marmer.matchergenerator.annotations.GenerateMatchers;",
                (deprecatedAlias ? "@MatcherConfiguration(" : "@GenerateMatchers(")
                        + "value = "
                        + configuredTypes
                        + ", exclude = "
                        + excludedTypes
                        + ")",
                "public final class SomeConfiguration {",
                "}");
    }

    private static JavaFileObject bean(final String packageName, final String simpleName) {
        return JavaFileObjects.forSourceLines(
                packageName + "." + simpleName,
                "package " + packageName + ";",
                "public class " + simpleName + " {",
                "    public String getName() { return \"\"; }",
                "}");
    }

    private static JavaFileObject outerWithPublicMemberTypes() {
        return JavaFileObjects.forSourceLines(
                "some.other.pck.Outer",
                "package some.other.pck;",
                "public class Outer {",
                "    public String getName() { return \"\"; }",
                "    public static class Inner {",
                "        public String getName() { return \"\"; }",
                "    }",
                "    public static class Kept {",
                "        public String getName() { return \"\"; }",
                "    }",
                "}");
    }

    private static List<String> recordedModelNames() {
        return RecordingBackend.models().stream()
                .map(TypeModel::qualifiedName)
                .collect(Collectors.toList());
    }

    private static List<String> recordedNestedNames(final TypeModel model) {
        return model.nestedTypes().stream()
                .map(TypeModel::qualifiedName)
                .collect(Collectors.toList());
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("An excluded class is left out of a configured package, on both annotations")
    void anExcludedClassIsLeftOutOfAConfiguredPackage(final boolean deprecatedAlias) {
        final Compilation compilation =
                compile(
                        exclusionConfiguration(
                                deprecatedAlias,
                                "\"some.other.pck\"",
                                "\"some.other.pck.Excluded\""),
                        bean("some.other.pck", "Kept"),
                        bean("some.other.pck", "Excluded"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(List.of("some.other.pck.Kept"), recordedModelNames());
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("An excluded class name removes that configured class, on both annotations")
    void anExcludedClassNameRemovesTheConfiguredClass(final boolean deprecatedAlias) {
        final Compilation compilation =
                compile(
                        exclusionConfiguration(
                                deprecatedAlias,
                                "{\"some.other.pck.Kept\", \"some.other.pck.Excluded\"}",
                                "\"some.other.pck.Excluded\""),
                        bean("some.other.pck", "Kept"),
                        bean("some.other.pck", "Excluded"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(List.of("some.other.pck.Kept"), recordedModelNames());
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "An excluded package leaves out every configured class of that package, on both annotations")
    void anExcludedPackageLeavesOutEveryConfiguredClassOfThatPackage(
            final boolean deprecatedAlias) {
        final Compilation compilation =
                compile(
                        exclusionConfiguration(
                                deprecatedAlias,
                                "{\"some.other.pck\", \"some.third.pck\"}",
                                "\"some.other.pck\""),
                        bean("some.other.pck", "First"),
                        bean("some.other.pck", "Second"),
                        bean("some.third.pck", "Third"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(List.of("some.third.pck.Third"), recordedModelNames());
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "Exclusions that remove every configured type generate nothing and warn about nothing")
    void exclusionsRemovingEveryConfiguredTypeGenerateNothingAndWarnNothing(
            final boolean deprecatedAlias) {
        final Compilation compilation =
                compile(
                        exclusionConfiguration(
                                deprecatedAlias,
                                "{\"some.other.pck.Kept\", \"some.other.pck.Second\"}",
                                "\"some.other.pck\""),
                        bean("some.other.pck", "Kept"),
                        bean("some.other.pck", "Second"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(List.of(), recordedModelNames());
        assertEquals(List.of(), compilation.generatedSourceFiles());
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "An excluded public member type of a configured package is not generated as nested model")
    void anExcludedMemberTypeOfAConfiguredPackageIsNotNested(final boolean deprecatedAlias) {
        final Compilation compilation =
                compile(
                        exclusionConfiguration(
                                deprecatedAlias,
                                "\"some.other.pck\"",
                                "\"some.other.pck.Outer.Inner\""),
                        outerWithPublicMemberTypes());

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(List.of("some.other.pck.Outer.Kept"), recordedNestedNames(onlyModel()));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "An excluded public member type of a configured class is not generated as nested model")
    void anExcludedMemberTypeOfAConfiguredClassIsNotNested(final boolean deprecatedAlias) {
        final Compilation compilation =
                compile(
                        exclusionConfiguration(
                                deprecatedAlias,
                                "\"some.other.pck.Outer\"",
                                "\"some.other.pck.Outer.Inner\""),
                        outerWithPublicMemberTypes());

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(List.of("some.other.pck.Outer.Kept"), recordedNestedNames(onlyModel()));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("An exclude entry that matches no configured type is one warning naming the entry")
    void anUnmatchedExcludeEntryIsOneWarningNamingTheEntry(final boolean deprecatedAlias) {
        final JavaFileObject configuration =
                exclusionConfiguration(
                        deprecatedAlias,
                        "\"some.other.pck.Kept\"",
                        "{\"some.other.pck.Missing\", \"some.other.pck.Missing\"}");

        final Compilation compilation = compile(configuration, bean("some.other.pck", "Kept"));

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("some.other.pck.Missing")
                .inFile(configuration)
                .onLine(3);
        assertEquals(1, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(0, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        assertEquals(List.of("some.other.pck.Kept"), recordedModelNames());
    }

    @Test
    @DisplayName("An exclude entry that matches a configured type is not warned about")
    void aMatchedExcludeEntryIsNotWarnedAbout() {
        final Compilation compilation =
                compile(
                        exclusionConfiguration(
                                false, "\"some.other.pck.Kept\"", "\"some.other.pck.Kept\""),
                        bean("some.other.pck", "Kept"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(List.of(), recordedModelNames());
    }

    @Test
    @DisplayName(
            "A configured package prefix is the target package of a type in the default package")
    void aConfiguredPackagePrefixIsTheTargetPackageOfADefaultPackageType() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines("DefaultPojo", "public class DefaultPojo {", "}");

        final Compilation compilation =
                compile(prefixedConfiguration("\"DefaultPojo\"", "prefix"), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals("", model.packageName());
        assertEquals("prefix", model.targetPackage());
        assertTrue(compilation.generatedSourceFile("prefix.DefaultPojoRecorded").isPresent());
    }

    @Test
    @DisplayName("Every backend runs for every configured type, whatever language it generates")
    void everyBackendRunsForEveryConfiguredType() {
        final JavaFileObject first =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.First",
                        "package some.other.pck;",
                        "public class First {",
                        "}");
        final JavaFileObject second =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Second",
                        "package some.other.pck;",
                        "public class Second {",
                        "}");

        final Compilation compilation =
                compile(
                        configuration("{\"some.other.pck.First\", \"some.other.pck.Second\"}"),
                        first,
                        second);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of("some.other.pck.First", "some.other.pck.Second"),
                RecordingBackend.models().stream()
                        .map(TypeModel::qualifiedName)
                        .collect(Collectors.toList()));
        for (final String name : List.of("First", "Second")) {
            assertTrue(
                    compilation
                            .generatedSourceFile("some.other.pck." + name + "Recorded")
                            .isPresent(),
                    name);
            assertTrue(
                    compilation
                            .generatedSourceFile("some.other.pck." + name + "Second")
                            .isPresent(),
                    name);
            assertTrue(
                    Files.isRegularFile(kotlinOutput.resolve("some/other/pck/" + name + "Dsl.kt")),
                    name);
        }
    }

    @Test
    @DisplayName("A backend can write several source files for one type")
    void aBackendCanWriteSeveralSourceFilesForOneType() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");

        final Compilation compilation = compile(configuration("\"some.other.pck.SomePojo\""), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        assertTrue(compilation.generatedSourceFile("some.other.pck.SomePojoSecond").isPresent());
        assertTrue(
                compilation.generatedSourceFile("some.other.pck.SomePojoSecondExtra").isPresent());
    }

    @Test
    @DisplayName("Kotlin sources go to the directory KAPT names")
    void kotlinSourcesGoToTheDirectoryKaptNames() throws IOException {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");

        final Compilation compilation = compile(configuration("\"some.other.pck.SomePojo\""), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                "package some.other.pck\n\nclass SomePojoDsl\n",
                Files.readString(kotlinOutput.resolve("some/other/pck/SomePojoDsl.kt")));
        assertFalse(
                compilation
                        .generatedFile(
                                StandardLocation.SOURCE_OUTPUT, "some.other.pck", "SomePojoDsl.kt")
                        .isPresent());
    }

    @Test
    @DisplayName("Kotlin output without the KAPT directory is an error naming backend and type")
    void kotlinOutputWithoutTheKaptDirectoryIsAnError() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");

        final Compilation compilation =
                compile(Compiler.javac(), configuration("\"some.other.pck.SomePojo\""), bean);

        CompilationSubject.assertThat(compilation).failed();
        CompilationSubject.assertThat(compilation).hadErrorCount(1);
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining(
                        "io.github.marmer.matchergenerator.core.FileOnlyBackends$KotlinBackend");
        CompilationSubject.assertThat(compilation).hadErrorContaining("some.other.pck.SomePojo");
        CompilationSubject.assertThat(compilation).hadErrorContaining("kapt.kotlin.generated");
    }

    @Test
    @DisplayName("Backends get the generator name")
    void backendsGetTheGeneratorName() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");

        compile(configuration("\"some.other.pck.SomePojo\""), bean);

        assertEquals(1, RecordingBackend.contexts().size());
        assertEquals(
                "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
                RecordingBackend.contexts().get(0).generatorName());
    }

    @Test
    @DisplayName(
            "Backends learn every type generated in the run, also those of other configurations")
    void backendsLearnEveryTypeGeneratedInTheRun() {
        final JavaFileObject first =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.First",
                        "package some.other.pck;",
                        "public class First {",
                        "    public static class Inner {",
                        "    }",
                        "}");
        final JavaFileObject second =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Second",
                        "package some.other.pck;",
                        "public class Second {",
                        "}");

        compile(
                generateMatchersConfiguration("FirstConfiguration", "\"some.other.pck.First\""),
                generateMatchersConfiguration("SecondConfiguration", "\"some.other.pck.Second\""),
                first,
                second);

        for (final GenerationContext context : RecordingBackend.contexts()) {
            assertEquals(
                    List.of("some.other.pck.First", "some.other.pck.Second"),
                    context.generatedTypes().stream()
                            .map(TypeModel::qualifiedName)
                            .collect(Collectors.toList()));
            assertEquals(
                    List.of("some.other.pck.First.Inner"),
                    context.generatedTypes().get(0).nestedTypes().stream()
                            .map(TypeModel::qualifiedName)
                            .collect(Collectors.toList()));
        }
        assertEquals(2, RecordingBackend.contexts().size());
    }

    @Test
    @DisplayName("Types already generated by this processor are skipped")
    void typesAlreadyGeneratedByThisProcessorAreSkipped() {
        final JavaFileObject generated =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "@javax.annotation.processing.Generated(\"io.github.marmer.matchergenerator.core.MatcherGenerationProcessor\")",
                        "public class SomePojo {",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.SomePojo\""), generated);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName("An unresolvable name of the deprecated alias is one warning, as in 5.x")
    void unresolvableNameOfTheDeprecatedAliasIsAWarning() {
        final JavaFileObject configuration = configuration("\"not.existing.Type\"");

        final Compilation compilation = compile(configuration);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Neither a type nor a package exists for 'not.existing.Type'")
                .inFile(configuration)
                .onLine(3);
        assertEquals(1, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(0, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName(
            "An unresolvable name of GenerateMatchers is one error at the annotation value, no follow-up")
    void unresolvableNameOfGenerateMatchersIsAnError() {
        final JavaFileObject configuration = generateMatchersConfiguration("\"not.existing.Type\"");

        final Compilation compilation = compile(configuration);

        CompilationSubject.assertThat(compilation).failed();
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("Neither a type nor a package exists for 'not.existing.Type'")
                .inFile(configuration)
                .onLine(3);
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName(
            "Every unresolvable GenerateMatchers name yields exactly one error and nothing else is reported")
    void everyUnresolvableNameYieldsExactlyOneError() {
        final JavaFileObject configuration =
                generateMatchersConfiguration(
                        "{\"not.existing.Type\", \"not.existing.pck\", \"not.existing.pck\"}");

        final Compilation compilation = compile(configuration);

        CompilationSubject.assertThat(compilation).failed();
        assertEquals(2, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("Neither a type nor a package exists for 'not.existing.Type'")
                .inFile(configuration);
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("Neither a type nor a package exists for 'not.existing.pck'")
                .inFile(configuration);
        assertEquals(
                List.of(), compilation.warnings().stream().map(d -> d.getMessage(null)).toList());
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName("Every unresolvable name is reported at its own annotation value")
    void unresolvableNamesAreReportedAtTheirOwnValue() {
        final JavaFileObject configuration =
                generateMatchersConfiguration("{\"not.existing.First\", \"not.existing.Second\"}");

        final Compilation compilation = compile(configuration);

        CompilationSubject.assertThat(compilation).failed();
        assertEquals(2, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        final long firstColumn = columnOf(compilation, "'not.existing.First'");
        final long secondColumn = columnOf(compilation, "'not.existing.Second'");
        assertTrue(
                firstColumn > 0 && secondColumn > firstColumn, firstColumn + " / " + secondColumn);
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'not.existing.First'")
                .inFile(configuration)
                .onLine(3)
                .atColumn((int) firstColumn);
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("'not.existing.Second'")
                .inFile(configuration)
                .onLine(3)
                .atColumn((int) secondColumn);
    }

    @Test
    @DisplayName(
            "A configured type that another processor writes in the first round is modelled, and its matchers are generated")
    void aConfiguredTypeWrittenByAnotherProcessorInTheFirstRoundIsModelled() {
        final JavaFileObject configuration = generateMatchersConfiguration("\"gen.Made\"");

        // The other processor comes first: a processor that claims the annotation hides it from the
        // processors after it in the same round, so only the first one writes in round 1.
        final Compilation compilation =
                javacWith()
                        .withProcessors(
                                new FirstRoundTypeProcessor("gen.Made"),
                                new MatcherGenerationProcessor())
                        .compile(List.of(configuration));

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals("gen.Made", model.qualifiedName());
        assertEquals(
                List.of("name:java.lang.String:getName", CLASS_PROPERTY),
                describe(model.properties()));
    }

    /**
     * Compiles with the other processor before the matcher generator, so that the other one runs in
     * the first round: a processor that claims the annotation hides it from the processors after
     * it.
     */
    private Compilation compileWithOtherFirst(
            final Processor other, final JavaFileObject... sources) {
        return javacWith()
                .withProcessors(other, new MatcherGenerationProcessor())
                .compile(Arrays.asList(sources));
    }

    private static List<String> qualifiedNamesOf(final List<TypeModel> models) {
        return models.stream().map(TypeModel::qualifiedName).sorted().toList();
    }

    @Test
    @DisplayName(
            "A name that resolves in a later round is modelled once, next to a name that resolves in the first round")
    void aNameOfALaterRoundIsModelledOnceNextToANameOfTheFirstRound() throws IOException {
        final Compilation compilation =
                compileWithOtherFirst(
                        new FirstRoundTypeProcessor("gen.Made"),
                        generateMatchersConfiguration("{\"some.summarized.First\", \"gen.Made\"}"),
                        summarizedType("First"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of("gen.Made", "some.summarized.First"),
                qualifiedNamesOf(RecordingBackend.models()));
        assertEquals(1, countGenerated(compilation, "PackageSummary.java"));
        final String summary = summary(compilation);
        assertTrue(summary.contains("[some.summarized.First]"), () -> "Summary: " + summary);
    }

    @Test
    @DisplayName(
            "A package file of a configuration with a name that never resolves is written once, in its round")
    void aPackageFileOfAConfigurationWithANeverResolvingNameIsWrittenOnceInItsRound()
            throws IOException {
        final Compilation compilation =
                compile(
                        configuration("{\"some.summarized.First\", \"not.existing.Type\"}"),
                        summarizedType("First"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(1, countGenerated(compilation, "PackageSummary.java"));
        final String summary = summary(compilation);
        assertTrue(summary.contains("[some.summarized.First]"), () -> "Summary: " + summary);
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Neither a type nor a package exists for 'not.existing.Type'");
    }

    @Test
    @DisplayName(
            "A resolvable name next to a name that never resolves is generated in its round, for GenerateMatchers")
    void aResolvableNameOfAFailingGenerateMatchersIsGeneratedInItsRound() {
        final JavaFileObject configuration =
                generateMatchersConfiguration(
                        "{\"some.other.pck.SomePojo\", \"not.existing.Type\"}");

        final Compilation compilation = compile(configuration, SOME_POJO);

        CompilationSubject.assertThat(compilation).failed();
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("Neither a type nor a package exists for 'not.existing.Type'")
                .inFile(configuration)
                .onLine(3);
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of("some.other.pck.SomePojo"), qualifiedNamesOf(RecordingBackend.models()));
    }

    @Test
    @DisplayName(
            "A resolvable name next to a name that never resolves is generated in its round, for the deprecated alias")
    void aResolvableNameOfAFailingDeprecatedAliasIsGeneratedInItsRound() {
        final JavaFileObject configuration =
                configuration("{\"some.other.pck.SomePojo\", \"not.existing.Type\"}");

        final Compilation compilation = compile(configuration, SOME_POJO);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Neither a type nor a package exists for 'not.existing.Type'")
                .inFile(configuration)
                .onLine(3);
        assertEquals(1, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of("some.other.pck.SomePojo"), qualifiedNamesOf(RecordingBackend.models()));
    }

    @Test
    @DisplayName(
            "A name that still matches nothing after a later round is reported once, at its own value, for GenerateMatchers")
    void aNameThatNeverResolvesIsReportedOnceAtTheEndAfterALaterRound() {
        final JavaFileObject configuration =
                generateMatchersConfiguration("{\"gen.Made\", \"not.existing.Type\"}");

        final Compilation compilation =
                compileWithOtherFirst(new FirstRoundTypeProcessor("gen.Made"), configuration);

        CompilationSubject.assertThat(compilation).failed();
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("Neither a type nor a package exists for 'not.existing.Type'")
                .inFile(configuration)
                .onLine(3)
                .atColumn((int) columnOf(compilation, "'not.existing.Type'"));
        assertEquals(List.of("gen.Made"), qualifiedNamesOf(RecordingBackend.models()));
    }

    @Test
    @DisplayName(
            "A name that still matches nothing after a later round is reported once, at its own value, for the deprecated alias")
    void aNameOfTheAliasThatNeverResolvesIsWarnedOnceAtTheEndAfterALaterRound() {
        final JavaFileObject configuration = configuration("{\"gen.Made\", \"not.existing.Type\"}");

        final Compilation compilation =
                compileWithOtherFirst(new FirstRoundTypeProcessor("gen.Made"), configuration);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                1,
                warningsContaining(
                        compilation, "Neither a type nor a package exists for 'not.existing.Type'"),
                () -> "Warnings: " + compilation.warnings());
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Neither a type nor a package exists for 'not.existing.Type'")
                .inFile(configuration)
                .onLine(3)
                .atColumn((int) columnOf(compilation, "'not.existing.Type'"));
        assertEquals(List.of("gen.Made"), qualifiedNamesOf(RecordingBackend.models()));
    }

    @Test
    @DisplayName(
            "A type with both configuration annotations is one error, even when a name matches nothing")
    void aTypeWithBothAnnotationsIsOneErrorEvenWithAnUnresolvableName() {
        final JavaFileObject both =
                JavaFileObjects.forSourceLines(
                        "some.pck.BothConfiguration",
                        "package some.pck;",
                        "import io.github.marmer.matchergenerator.annotations.GenerateMatchers;",
                        "import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;",
                        "@GenerateMatchers(\"not.existing.Type\")",
                        "@MatcherConfiguration(\"not.existing.Type\")",
                        "public final class BothConfiguration {",
                        "}");

        final Compilation compilation = compile(both);

        CompilationSubject.assertThat(compilation).failed();
        assertEquals(1, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("Use either @GenerateMatchers or @MatcherConfiguration");
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "An unmatched exclude entry of a configuration with a name that never resolves is warned about once")
    void anUnmatchedExcludeEntryOfAConfigurationWithANeverResolvingNameIsWarnedOnce(
            final boolean deprecatedAlias) {
        final JavaFileObject configuration =
                exclusionConfiguration(
                        deprecatedAlias,
                        "{\"some.other.pck.SomePojo\", \"not.existing.Type\"}",
                        "\"not.existing.Excluded\"");

        final Compilation compilation = compile(configuration, SOME_POJO);

        assertEquals(
                deprecatedAlias ? 0 : 1,
                compilation.errors().size(),
                () -> "Errors: " + compilation.errors());
        assertEquals(
                1,
                warningsContaining(compilation, "Exclusion 'not.existing.Excluded'"),
                () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of("some.other.pck.SomePojo"), qualifiedNamesOf(RecordingBackend.models()));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "An exclude entry that names a type of a name that resolves in a later round is no warning")
    void anExcludeEntryOfANameOfALaterRoundIsNoWarning(final boolean deprecatedAlias) {
        final JavaFileObject configuration =
                exclusionConfiguration(
                        deprecatedAlias,
                        "{\"some.other.pck.SomePojo\", \"gen.Made\"}",
                        "\"gen.Made\"");

        final Compilation compilation =
                compileWithOtherFirst(
                        new FirstRoundTypeProcessor("gen.Made"), configuration, SOME_POJO);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of("some.other.pck.SomePojo"), qualifiedNamesOf(RecordingBackend.models()));
    }

    @Test
    @DisplayName(
            "A name that resolves in a later round into a package whose file an earlier round wrote is warned about, and the file is not written again")
    void aLateTypeOfAPackageWrittenByAnEarlierRoundIsWarnedAboutAndNotWrittenAgain()
            throws IOException {
        final Compilation compilation =
                compileWithOtherFirst(
                        new FirstRoundTypeProcessor("some.summarized.Late"),
                        generateMatchersConfiguration(
                                "{\"some.summarized.First\", \"some.summarized.Late\"}"),
                        summarizedType("First"));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(1, countGenerated(compilation, "PackageSummary.java"));
        final String summary = summary(compilation);
        assertTrue(summary.contains("[some.summarized.First]"), () -> "Summary: " + summary);
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "some.summarized.PackageSummary was already written in an earlier"
                                + " processing round and is not written again. It does not know"
                                + " the types generated in this round: some.summarized.Late");
        assertEquals(1, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of("some.summarized.First", "some.summarized.Late"),
                qualifiedNamesOf(RecordingBackend.models()));
    }

    private static long warningsContaining(final Compilation compilation, final String text) {
        return compilation.warnings().stream()
                .filter(diagnostic -> diagnostic.getMessage(null).contains(text))
                .count();
    }

    private static long columnOf(final Compilation compilation, final String messagePart) {
        return Stream.concat(compilation.errors().stream(), compilation.warnings().stream())
                .filter(diagnostic -> diagnostic.getMessage(null).contains(messagePart))
                .mapToLong(javax.tools.Diagnostic::getColumnNumber)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("Every unresolvable name of the deprecated alias yields exactly one warning")
    void everyUnresolvableAliasNameYieldsExactlyOneWarning() {
        final JavaFileObject configuration =
                configuration(
                        "{\"not.existing.Type\", \"not.existing.pck\", \"not.existing.pck\"}");

        final Compilation compilation = compile(configuration);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(2, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(0, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName("Malformed GenerateMatchers names are one error each, never a crash")
    void malformedConfiguredNamesAreOneErrorEach() {
        for (final String name :
                List.of(
                        "\"\"",
                        "\"some.other.pck.\"",
                        "\"1abc\"",
                        "\"some..pck\"",
                        "\"*\"",
                        "\"some.other.pck.SomePojo.Missing\"",
                        "\" \"")) {
            final Compilation compilation = compile(generateMatchersConfiguration(name), SOME_POJO);

            CompilationSubject.assertThat(compilation).failed();
            assertEquals(
                    1,
                    compilation.errors().size(),
                    () -> name + " errors: " + compilation.errors());
            assertEquals(List.of(), RecordingBackend.models());
            RecordingBackend.reset();
        }
    }

    @Test
    @DisplayName("A configured type without properties is generated and warned about")
    void aConfiguredTypeWithoutPropertiesIsWarnedAbout() {
        final JavaFileObject empty =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.EmptyPojo",
                        "package some.other.pck;",
                        "public class EmptyPojo {",
                        "    public void doSomething() { }",
                        "}");
        final JavaFileObject emptyInterface =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.EmptyInterface",
                        "package some.other.pck;",
                        "public interface EmptyInterface {",
                        "    void doSomething();",
                        "}");

        final Compilation compilation =
                compile(
                        configuration(
                                "{\"some.other.pck.EmptyPojo\", \"some.other.pck.EmptyInterface\"}"),
                        empty,
                        emptyInterface);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Type 'some.other.pck.EmptyPojo' has no properties");
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Type 'some.other.pck.EmptyInterface' has no properties");
        assertEquals(2, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of("some.other.pck.EmptyPojo", "some.other.pck.EmptyInterface"),
                RecordingBackend.models().stream()
                        .map(TypeModel::qualifiedName)
                        .collect(Collectors.toList()));
    }

    @Test
    @DisplayName(
            "A type without properties that a package entry picks up is generated without a warning")
    void aPackageScannedTypeWithoutPropertiesIsNotWarnedAbout() {
        final JavaFileObject empty =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.EmptyPojo",
                        "package some.other.pck;",
                        "public class EmptyPojo {",
                        "    public void doSomething() { }",
                        "}");
        final JavaFileObject emptyInterface =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.EmptyInterface",
                        "package some.other.pck;",
                        "public interface EmptyInterface {",
                        "    void doSomething();",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck\""), empty, emptyInterface);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
                List.of("some.other.pck.EmptyInterface", "some.other.pck.EmptyPojo"),
                RecordingBackend.models().stream()
                        .map(TypeModel::qualifiedName)
                        .sorted()
                        .collect(Collectors.toList()));
    }

    @Test
    @DisplayName(
            "A package scanned type without properties is a verbose note, and only in verbose mode")
    void aPackageScannedTypeWithoutPropertiesIsAVerboseNote() {
        final JavaFileObject empty =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.EmptyPojo",
                        "package some.other.pck;",
                        "public class EmptyPojo {",
                        "    public void doSomething() { }",
                        "}");

        final Compilation verbose = compileVerbose(configuration("\"some.other.pck\""), empty);
        final Compilation quiet = compile(configuration("\"some.other.pck\""), empty);

        CompilationSubject.assertThat(verbose).succeeded();
        assertEquals(
                List.of(
                        "Type 'some.other.pck.EmptyPojo' picked up by a package entry has no properties to match"),
                verbose.notes().stream()
                        .map(d -> d.getMessage(null))
                        .filter(message -> message.startsWith("Type '"))
                        .collect(Collectors.toList()));
        CompilationSubject.assertThat(quiet).succeeded();
        assertEquals(
                List.of(),
                quiet.notes().stream()
                        .map(d -> d.getMessage(null))
                        .filter(message -> message.startsWith("Type '"))
                        .collect(Collectors.toList()));
    }

    @Test
    @DisplayName(
            "A configured nested type without properties is warned about, not its unconfigured container")
    void aConfiguredNestedTypeWithoutPropertiesIsWarnedAbout() {
        final JavaFileObject outer =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Outer",
                        "package some.other.pck;",
                        "public class Outer {",
                        "    public String getOuterProperty() { return \"\"; }",
                        "    public static class Inner {",
                        "        public void doSomething() { }",
                        "    }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.Outer.Inner\""), outer);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Type 'some.other.pck.Outer.Inner' has no properties");
        assertEquals(1, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(1, RecordingBackend.models().size());
    }

    @Test
    @DisplayName(
            "A configured nested type with properties is not warned about, whatever its container holds")
    void aConfiguredNestedTypeWithPropertiesIsNotWarnedAbout() {
        final JavaFileObject outer =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Outer",
                        "package some.other.pck;",
                        "public class Outer {",
                        "    public static class Inner {",
                        "        public String getInnerProperty() { return \"\"; }",
                        "    }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.Outer.Inner\""), outer);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(1, RecordingBackend.models().size());
    }

    @Test
    @DisplayName(
            "A Kotlin file facade without properties is not warned about, it holds only top level functions")
    void aKotlinFileFacadeIsNotWarnedAbout() {
        final JavaFileObject metadata =
                JavaFileObjects.forSourceLines(
                        "kotlin.Metadata",
                        "package kotlin;",
                        "public @interface Metadata {",
                        "    int k() default 1;",
                        "}");
        final JavaFileObject facade =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.HelloKt",
                        "package some.other.pck;",
                        "@kotlin.Metadata(k = 2)",
                        "public final class HelloKt {",
                        "    public static String hello() { return \"\"; }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.HelloKt\""), metadata, facade);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(1, RecordingBackend.models().size());
    }

    @Test
    @DisplayName("A type with at least one property is not warned about")
    void aTypeWithPropertiesIsNotWarnedAbout() {
        final Compilation compilation =
                compile(configuration("\"some.other.pck.SomePojo\""), SOME_POJO);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
    }

    @Test
    @DisplayName("Without the verbose option no notes are emitted at all")
    void withoutTheVerboseOptionNoNotesAreEmitted() {
        final Compilation compilation =
                compile(
                        configurationWith(
                                "io.github.marmer.matchergenerator.annotations.GenerateMatchers",
                                "@GenerateMatchers(\"some.other.pck.SkippingPojo\")"),
                        skippingPojo());

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of(), compilation.notes().stream().map(d -> d.getMessage(null)).toList());
    }

    @Test
    @DisplayName(
            "A named type's skipped getters are verbose notes, except its two wrong prefix getters, which are warnings at its name")
    void aNamedTypesSkippedGettersAreNotesExceptTheWrongPrefixOnesWhichAreWarnings()
            throws IOException {
        final JavaFileObject configuration = skippingConfiguration();

        final Compilation compilation = compileVerbose(configuration, skippingPojo());

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadWarningCount(2);
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'getBooleanWithGet' of some.other.pck.SkippingPojo: a boolean property must use the 'is' prefix; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.SkippingPojo\""));
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'isNotBoolean' of some.other.pck.SkippingPojo: the 'is' prefix requires a boolean return type; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.SkippingPojo\""));
        final List<String> skipNotes = skipNotes(compilation);
        assertEquals(
                List.of(
                        "Skipped property 'getProtected' of some.other.pck.SkippingPojo: method is not public",
                        "Skipped property 'getPrivate' of some.other.pck.SkippingPojo: method is not public",
                        "Skipped property 'getStatic' of some.other.pck.SkippingPojo: method is static",
                        "Skipped property 'getWithParameter' of some.other.pck.SkippingPojo: method takes parameters",
                        "Skipped property 'getVoid' of some.other.pck.SkippingPojo: method returns void",
                        "Skipped property 'get' of some.other.pck.SkippingPojo: no property name follows the 'get' prefix"),
                skipNotes);
        assertEquals(
                List.of("name:java.lang.String:getName", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    private static List<String> skipNotes(final Compilation compilation) {
        return compilation.notes().stream()
                .map(d -> d.getMessage(null))
                .filter(message -> message.startsWith("Skipped property"))
                .collect(Collectors.toList());
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A named type with a boolean get getter is one warning at its name, naming the method, on both annotations")
    void aNamedTypeWithABooleanGetterIsOneWarningAtItsName(final boolean deprecatedAlias)
            throws IOException {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck.Sub\"");

        final Compilation compilation =
                compile(configuration, subWith("    public boolean getActive() { return true; }"));

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'getActive' of some.other.pck.Sub: a boolean property must use the 'is' prefix; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Sub\""));
        CompilationSubject.assertThat(compilation).hadWarningCount(1);
        assertEquals(
                List.of("name:java.lang.String:getName", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A named type with an is method that returns no boolean is one warning at its name, naming the method, on both annotations")
    void aNamedTypeWithAnIsMethodWithoutBooleanIsOneWarningAtItsName(final boolean deprecatedAlias)
            throws IOException {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck.Sub\"");

        final Compilation compilation =
                compile(configuration, subWith("    public String isName() { return \"\"; }"));

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'isName' of some.other.pck.Sub: the 'is' prefix requires a boolean return type; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Sub\""));
        CompilationSubject.assertThat(compilation).hadWarningCount(1);
        assertEquals(
                List.of("name:java.lang.String:getName", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A type a package entry picks up with the same skipped getters is no warning, its notes stay verbose")
    void aPackagePickedUpTypeWithTheSkippedGettersIsNoWarning(final boolean deprecatedAlias) {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck\"");
        final JavaFileObject sub =
                subWith(
                        "    public boolean getActive() { return true; }",
                        "    public String isName() { return \"\"; }");

        final Compilation quiet = compile(configuration, sub);
        final Compilation verbose = compileVerbose(configuration, sub);

        CompilationSubject.assertThat(quiet).succeeded();
        CompilationSubject.assertThat(quiet).hadWarningCount(0);
        assertEquals(List.of(), skipNotes(quiet));
        CompilationSubject.assertThat(verbose).succeeded();
        CompilationSubject.assertThat(verbose).hadWarningCount(0);
        assertEquals(
                List.of(
                        "Skipped property 'getActive' of some.other.pck.Sub: a boolean property must use the 'is' prefix",
                        "Skipped property 'isName' of some.other.pck.Sub: the 'is' prefix requires a boolean return type"),
                skipNotes(verbose));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A named type with a private, a static, a parameterised or a void get getter is no warning, on both annotations")
    void aNamedTypeWithAnotherSkippedGetterIsNoWarning(final boolean deprecatedAlias) {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck.Sub\"");

        final Compilation compilation =
                compile(
                        configuration,
                        subWith(
                                "    private String getPrivate() { return \"\"; }",
                                "    public static String getStatic() { return \"\"; }",
                                "    public String getWithParameter(int param) { return \"\"; }",
                                "    public void getVoid() { }"));

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadWarningCount(0);
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A skipped boolean get getter that a Kotlin subtype overrides with an accepted getter is no warning, on both annotations")
    void aSkippedGetterOverriddenByAnAcceptedOneIsNoWarning(final boolean deprecatedAlias) {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck.Sub\"");
        final JavaFileObject base =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Base",
                        "package some.other.pck;",
                        "public class Base {",
                        "    public boolean getActive() { return true; }",
                        "}");
        final JavaFileObject sub =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Sub",
                        "package some.other.pck;",
                        "@kotlin.Metadata",
                        "public class Sub extends Base {",
                        "    public boolean getActive() { return true; }",
                        "}");

        final Compilation compilation = compile(configuration, base, sub, KOTLIN_METADATA);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadWarningCount(0);
        assertEquals(
                List.of("active:boolean:getActive", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A named nested type with a skipped boolean get getter is one warning at the value that names it, on both annotations")
    void aNamedNestedTypeWithABooleanGetterIsOneWarningAtItsValue(final boolean deprecatedAlias)
            throws IOException {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck.Outer.Inner\"");

        final Compilation compilation = compile(configuration, outerWithInner());

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'getActive' of some.other.pck.Outer.Inner: a boolean property must use the 'is' prefix; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Outer.Inner\""));
        CompilationSubject.assertThat(compilation).hadWarningCount(1);
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A nested type that only its named container brings in is a verbose note at its getter, not a warning, on both annotations")
    void aNestedTypeOfANamedTypeIsOnlyAVerboseNote(final boolean deprecatedAlias) {
        final JavaFileObject outer = outerWithInner();

        final Compilation compilation =
                compileVerbose(
                        configurationNaming(deprecatedAlias, "\"some.other.pck.Outer\""), outer);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadWarningCount(0);
        assertEquals(
                List.of(
                        "Skipped property 'getActive' of some.other.pck.Outer.Inner: a boolean property must use the 'is' prefix"),
                skipNotes(compilation));
        CompilationSubject.assertThat(compilation)
                .hadNoteContaining(
                        "Skipped property 'getActive' of some.other.pck.Outer.Inner: a boolean property must use the 'is' prefix")
                .inFile(outer)
                .onLine(6);
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A named type whose only getter is a boolean get getter has two warnings: the skipped getter and the missing properties")
    void aNamedTypeWhoseOnlyGetterIsSkippedHasTwoWarnings(final boolean deprecatedAlias)
            throws IOException {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck.Sub\"");
        final JavaFileObject sub =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Sub",
                        "package some.other.pck;",
                        "public class Sub {",
                        "    public boolean getActive() { return true; }",
                        "}");

        final Compilation compilation = compile(configuration, sub);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadWarningCount(2);
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'getActive' of some.other.pck.Sub: a boolean property must use the 'is' prefix; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Sub\""));
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Type 'some.other.pck.Sub' has no properties to match")
                .inFile(sub)
                .onLine(2);
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "Under the verbose option a wrong prefix getter of a named type is the warning only, no note with the same text")
    void aWrongPrefixGetterOfANamedTypeIsTheWarningAndNoNote(final boolean deprecatedAlias)
            throws IOException {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck.Sub\"");

        final Compilation compilation =
                compileVerbose(
                        configuration, subWith("    public boolean getActive() { return true; }"));

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'getActive' of some.other.pck.Sub: a boolean property must use the 'is' prefix; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Sub\""));
        CompilationSubject.assertThat(compilation).hadWarningCount(1);
        assertEquals(List.of(), skipNotes(compilation));
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "A named subtype that inherits a wrong prefix getter from an unnamed supertype has the warning at its own name")
    void aWrongPrefixGetterInheritedFromAnUnnamedSupertypeWarnsAtTheNamedSubtype(
            final boolean deprecatedAlias) throws IOException {
        final JavaFileObject configuration =
                configurationNaming(deprecatedAlias, "\"some.other.pck.Sub\"");
        final JavaFileObject base =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Base",
                        "package some.other.pck;",
                        "public class Base {",
                        "    public boolean getActive() { return true; }",
                        "}");
        final JavaFileObject sub =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Sub",
                        "package some.other.pck;",
                        "public class Sub extends Base {",
                        "    public String getName() { return \"\"; }",
                        "}");

        final Compilation compilation = compile(configuration, base, sub);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'getActive' of some.other.pck.Sub: a boolean property must use the 'is' prefix; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Sub\""));
        CompilationSubject.assertThat(compilation).hadWarningCount(1);
    }

    @ParameterizedTest(name = "deprecated alias: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
            "When the supertype is named too, an inherited wrong prefix getter warns at the name of each named type that has it")
    void anInheritedWrongPrefixGetterWarnsAtEachNamedTypeThatHasIt(final boolean deprecatedAlias)
            throws IOException {
        final JavaFileObject configuration =
                configurationNaming(
                        deprecatedAlias, "{\"some.other.pck.Sub\", \"some.other.pck.Base\"}");
        final JavaFileObject base =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Base",
                        "package some.other.pck;",
                        "public class Base {",
                        "    public boolean getActive() { return true; }",
                        "}");
        final JavaFileObject sub =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Sub",
                        "package some.other.pck;",
                        "public class Sub extends Base {",
                        "    public String getName() { return \"\"; }",
                        "}");

        final Compilation compilation = compile(configuration, base, sub);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation).hadWarningCount(3);
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'getActive' of some.other.pck.Sub: a boolean property must use the 'is' prefix; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Sub\""));
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining(
                        "Skipped property 'getActive' of some.other.pck.Base: a boolean property must use the 'is' prefix; the completeness check does not see it")
                .inFile(configuration)
                .onLine(3)
                .atColumn(columnOfQuotedName(configuration, 3, "\"some.other.pck.Base\""));
        CompilationSubject.assertThat(compilation)
                .hadWarningContaining("Type 'some.other.pck.Base' has no properties to match")
                .inFile(base)
                .onLine(2);
    }

    /** The configuration names the given values with the annotation the flag selects. */
    private static JavaFileObject configurationNaming(
            final boolean deprecatedAlias, final String configuredTypes) {
        return deprecatedAlias
                ? configuration(configuredTypes)
                : generateMatchersConfiguration(configuredTypes);
    }

    /**
     * A public class some.other.pck.Sub with the given members. It has the property name besides
     * them, since a type without any property is a warning of its own.
     */
    private static JavaFileObject subWith(final String... members) {
        final List<String> lines = new ArrayList<>();
        lines.add("package some.other.pck;");
        lines.add("public class Sub {");
        lines.add("    public String getName() { return \"\"; }");
        lines.addAll(Arrays.asList(members));
        lines.add("}");
        return JavaFileObjects.forSourceLines("some.other.pck.Sub", lines.toArray(String[]::new));
    }

    /**
     * A public class Outer with a public nested class Inner. Inner has a boolean get getter, and
     * the property label besides it.
     */
    private static JavaFileObject outerWithInner() {
        return JavaFileObjects.forSourceLines(
                "some.other.pck.Outer",
                "package some.other.pck;",
                "public class Outer {",
                "    public String getName() { return \"\"; }",
                "    public static class Inner {",
                "        public String getLabel() { return \"\"; }",
                "        public boolean getActive() { return true; }",
                "    }",
                "}");
    }

    @Test
    @DisplayName(
            "A record's accessors are its components and its record style accessors, nothing else")
    void aRecordsAccessorsAreItsComponentsAndRecordStyleAccessors() {
        final JavaFileObject record =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.MixedRecord",
                        "package some.other.pck;",
                        "public record MixedRecord(String name) {",
                        "    public String label() { return \"\"; }",
                        "    public static String staticLabel() { return \"\"; }",
                        "    public String withParameter(int index) { return \"\"; }",
                        "    public void voidLabel() { }",
                        "    private String hiddenLabel() { return \"\"; }",
                        "    public String getTitle() { return \"\"; }",
                        "    public boolean isOpen() { return true; }",
                        "    @Override",
                        "    public String toString() { return \"\"; }",
                        "    @Override",
                        "    public int hashCode() { return 0; }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.MixedRecord\""), record);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of(
                        "label:java.lang.String:label",
                        "name:java.lang.String:name",
                        "title:java.lang.String:getTitle",
                        "open:boolean:isOpen",
                        CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName(
            "An accessor with parameters that shares its name with a property is not reported as skipped")
    void anOverloadedAccessorIsNotReportedAsSkipped() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.OverloadedPojo",
                        "package some.other.pck;",
                        "public class OverloadedPojo {",
                        "    public String getName() { return \"\"; }",
                        "    public String getName(int index) { return \"\"; }",
                        "}");

        final Compilation compilation =
                compileVerbose(configuration("\"some.other.pck.OverloadedPojo\""), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of(), skipNotes(compilation));
        assertEquals(
                List.of("name:java.lang.String:getName", CLASS_PROPERTY),
                describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("The verbose option is switched off by false and on by true")
    void theVerboseOptionHonoursItsExplicitValue() {
        final Compilation off =
                compile(
                        javacWith("-Amatchergenerator.verbose=false"),
                        skippingConfiguration(),
                        skippingPojo());
        final Compilation on =
                compile(
                        javacWith("-Amatchergenerator.verbose=true"),
                        skippingConfiguration(),
                        skippingPojo());

        CompilationSubject.assertThat(off).succeeded();
        CompilationSubject.assertThat(on).succeeded();
        assertEquals(List.of(), skipNotes(off));
        assertEquals(
                skipNotes(compileVerbose(skippingConfiguration(), skippingPojo())), skipNotes(on));
    }

    @Test
    @DisplayName("Types in the default package get their matchers in the default package")
    void typesInTheDefaultPackageGetTheirMatchersInTheDefaultPackage() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines("DefaultPojo", "public class DefaultPojo {", "}");

        final Compilation compilation =
                compile(generateMatchersConfiguration("\"DefaultPojo\""), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals("DefaultPojo", model.qualifiedName());
        assertEquals("", model.packageName());
        assertEquals("", model.targetPackage());
        assertTrue(compilation.generatedSourceFile("DefaultPojoRecorded").isPresent());
    }

    @Test
    @DisplayName(
            "A generated file that cannot be written fails the compilation instead of being dropped")
    void aGeneratedFileThatCannotBeWrittenFailsTheCompilation() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "public class SomePojo {",
                        "}");
        // Two configurations naming the same type make the backend write the same file twice.
        final JavaFileObject first =
                generateMatchersConfiguration("FirstConfiguration", "\"some.other.pck.SomePojo\"");
        final JavaFileObject second =
                generateMatchersConfiguration("SecondConfiguration", "\"some.other.pck.SomePojo\"");

        final RuntimeException thrown =
                assertThrows(RuntimeException.class, () -> compile(first, second, bean));

        assertTrue(thrown.getCause() instanceof UncheckedIOException, () -> "Thrown: " + thrown);
    }

    @Test
    @DisplayName("Types generated by another processor are not skipped")
    void typesGeneratedByAnotherProcessorAreNotSkipped() {
        final JavaFileObject generated =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "@javax.annotation.processing.Generated(\"some.other.processing.OtherProcessor\")",
                        "public class SomePojo {",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.SomePojo\""), generated);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(1, RecordingBackend.models().size());
    }

    private static JavaFileObject skippingConfiguration() {
        return configuration("\"some.other.pck.SkippingPojo\"");
    }

    private static JavaFileObject skippingPojo() {
        return JavaFileObjects.forSourceLines(
                "some.other.pck.SkippingPojo",
                "package some.other.pck;",
                "public class SkippingPojo {",
                "    public String getName() { return \"\"; }",
                "    protected String getProtected() { return \"\"; }",
                "    private String getPrivate() { return \"\"; }",
                "    public static String getStatic() { return \"\"; }",
                "    public String getWithParameter(int param) { return \"\"; }",
                "    public void getVoid() { }",
                "    public boolean getBooleanWithGet() { return true; }",
                "    public String isNotBoolean() { return \"\"; }",
                "    public String get() { return \"\"; }",
                "}");
    }

    @Test
    @DisplayName("Public non static inner classes are nested models as well")
    void publicInnerClassesAreNestedModels() {
        final JavaFileObject outer =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Holder",
                        "package some.other.pck;",
                        "public class Holder {",
                        "    public String getHolderProperty() { return \"\"; }",
                        "    public class Inner {",
                        "        public String getInnerProperty() { return \"\"; }",
                        "    }",
                        "}");

        final Compilation compilation = compile(configuration("\"some.other.pck.Holder\""), outer);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(1, model.nestedTypes().size());
        assertEquals("some.other.pck.Holder.Inner", model.nestedTypes().get(0).qualifiedName());
        assertEquals(
                List.of("innerProperty:java.lang.String:getInnerProperty", CLASS_PROPERTY),
                describe(model.nestedTypes().get(0).properties()));
    }

    @Test
    @DisplayName("Recursive generic bounds are modelled without failing")
    void recursiveGenericBoundsAreModelled() {
        final JavaFileObject node =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Node",
                        "package some.other.pck;",
                        "public class Node<T extends Node<T>> {",
                        "    public T getSelf() { return null; }",
                        "}");

        final Compilation compilation = compile(configuration("\"some.other.pck.Node\""), node);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of("T"), onlyModel().typeParameters());
        assertEquals(List.of("self:T:getSelf", CLASS_PROPERTY), describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("Cyclic references between configured types are modelled for both types")
    void cyclicReferencesBetweenConfiguredTypesAreModelled() {
        final JavaFileObject parent =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Parent",
                        "package some.other.pck;",
                        "public class Parent {",
                        "    public Child getChild() { return null; }",
                        "}");
        final JavaFileObject child =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Child",
                        "package some.other.pck;",
                        "public class Child {",
                        "    public Parent getParent() { return null; }",
                        "}");

        final Compilation compilation =
                compile(
                        configuration("{\"some.other.pck.Parent\", \"some.other.pck.Child\"}"),
                        parent,
                        child);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
                List.of("some.other.pck.Parent", "some.other.pck.Child"),
                RecordingBackend.models().stream()
                        .map(TypeModel::qualifiedName)
                        .collect(Collectors.toList()));
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
    }

    @Test
    @DisplayName("Cyclic inheritance is reported by the compiler without a processor crash")
    void cyclicInheritanceDoesNotCrashTheProcessor() {
        final JavaFileObject first =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.First",
                        "package some.other.pck;",
                        "public class First extends Second {",
                        "}");
        final JavaFileObject second =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.Second",
                        "package some.other.pck;",
                        "public class Second extends First {",
                        "    public String getName() { return \"\"; }",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.First\""), first, second);

        CompilationSubject.assertThat(compilation).failed();
        assertEquals(
                List.of(),
                compilation.errors().stream()
                        .map(d -> d.getMessage(null))
                        .filter(message -> !message.contains("cyclic inheritance"))
                        .toList());
    }

    private static JavaFileObject configurationWith(
            final String annotationImport, final String annotation) {
        return JavaFileObjects.forSourceLines(
                "some.pck.SomeConfiguration",
                "package some.pck;",
                "import " + annotationImport + ";",
                annotation,
                "public final class SomeConfiguration {",
                "}");
    }

    private static String summary(final TypeModel model) {
        return model.qualifiedName()
                + "|"
                + model.targetPackage()
                + "|"
                + model.kind()
                + "|"
                + model.typeParameters()
                + "|"
                + model.type()
                + "|"
                + describe(model.properties())
                + "|"
                + model.nestedTypes().stream()
                        .map(CoreProcessorIT::summary)
                        .collect(Collectors.toList());
    }

    private static byte[] classFile(final Compilation compilation, final String name)
            throws IOException {
        return compilation
                .generatedFile(StandardLocation.CLASS_OUTPUT, name.replace('.', '/') + ".class")
                .orElseThrow()
                .openInputStream()
                .readAllBytes();
    }

    private static boolean contains(final byte[] bytes, final String text) {
        return new String(bytes, StandardCharsets.ISO_8859_1).contains(text);
    }

    /**
     * A stand-in for {@code kotlin.Metadata}, which marks the classes that Kotlin compiled. The
     * core only checks the name.
     */
    private static final JavaFileObject KOTLIN_METADATA =
            JavaFileObjects.forSourceLines(
                    "kotlin.Metadata", "package kotlin;", "public @interface Metadata {", "}");

    private static final JavaFileObject SOME_POJO =
            JavaFileObjects.forSourceLines(
                    "some.other.pck.SomePojo",
                    "package some.other.pck;",
                    "public class SomePojo {",
                    "    public String getName() { return \"\"; }",
                    "    public boolean isActive() { return true; }",
                    "}");

    @Test
    @DisplayName(
            "GenerateMatchers and the deprecated MatcherConfiguration produce identical models")
    void generateMatchersAndTheDeprecatedAliasProduceIdenticalModels() {
        final Compilation current =
                compile(
                        configurationWith(
                                "io.github.marmer.matchergenerator.annotations.GenerateMatchers",
                                "@GenerateMatchers(value = \"some.other.pck.SomePojo\", generation = "
                                        + "@GenerateMatchers.GenerationConfiguration(packageConfig = "
                                        + "@GenerateMatchers.GenerationConfiguration.PackageConfiguration(\"prefix.\")))"),
                        SOME_POJO);
        CompilationSubject.assertThat(current).succeeded();
        final List<String> currentSummary =
                RecordingBackend.models().stream()
                        .map(CoreProcessorIT::summary)
                        .collect(Collectors.toList());
        final List<String> currentGenerated =
                current.generatedSourceFiles().stream()
                        .map(JavaFileObject::getName)
                        .collect(Collectors.toList());
        RecordingBackend.reset();

        final Compilation alias =
                compile(
                        configurationWith(
                                "io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration",
                                "@MatcherConfiguration(value = \"some.other.pck.SomePojo\", generation = "
                                        + "@MatcherConfiguration.GenerationConfiguration(packageConfig = "
                                        + "@MatcherConfiguration.GenerationConfiguration.PackageConfiguration(\"prefix.\")))"),
                        SOME_POJO);
        CompilationSubject.assertThat(alias).succeeded();

        assertEquals(1, currentSummary.size());
        assertEquals(
                currentSummary,
                RecordingBackend.models().stream()
                        .map(CoreProcessorIT::summary)
                        .collect(Collectors.toList()));
        assertEquals(
                currentGenerated,
                alias.generatedSourceFiles().stream()
                        .map(JavaFileObject::getName)
                        .collect(Collectors.toList()));
        assertEquals("prefix.some.other.pck", RecordingBackend.models().get(0).targetPackage());
    }

    @Test
    @DisplayName("GenerateMatchers keeps its defaults like the alias does")
    void generateMatchersKeepsItsDefaults() {
        compile(
                configurationWith(
                        "io.github.marmer.matchergenerator.annotations.GenerateMatchers",
                        "@GenerateMatchers(\"some.other.pck\")"),
                SOME_POJO);

        assertEquals("some.other.pck", onlyModel().targetPackage());
    }

    @Test
    @DisplayName("Neither configuration annotation leaves a trace in compiled class files (QS-N1)")
    void neitherConfigurationAnnotationLeavesATraceInCompiledClassFiles() throws IOException {
        final Compilation alias =
                compile(
                        configurationWith(
                                "io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration",
                                "@MatcherConfiguration(\"some.other.pck.SomePojo\")"),
                        SOME_POJO);
        CompilationSubject.assertThat(alias).succeeded();
        final Compilation current =
                compile(
                        configurationWith(
                                "io.github.marmer.matchergenerator.annotations.GenerateMatchers",
                                "@GenerateMatchers(\"some.other.pck.SomePojo\")"),
                        SOME_POJO);
        CompilationSubject.assertThat(current).succeeded();

        final byte[] aliasClass = classFile(alias, "some.pck.SomeConfiguration");
        assertFalse(contains(aliasClass, "MatcherConfiguration"));
        assertFalse(contains(aliasClass, "beanmatcher"));
        final byte[] currentClass = classFile(current, "some.pck.SomeConfiguration");
        assertFalse(contains(currentClass, "GenerateMatchers"));
        assertFalse(contains(currentClass, "RuntimeInvisibleAnnotations"));

        // proves the checks above can fail: a foreign RUNTIME annotation is visible in the class
        // file
        final Compilation control =
                compile(
                        JavaFileObjects.forSourceLines(
                                "some.pck.SomeControl",
                                "package some.pck;",
                                "@Deprecated",
                                "public final class SomeControl {",
                                "}"),
                        SOME_POJO);
        CompilationSubject.assertThat(control).succeeded();
        final byte[] controlClass = classFile(control, "some.pck.SomeControl");
        assertTrue(contains(controlClass, "RuntimeVisibleAnnotations"));
        assertTrue(contains(controlClass, "Deprecated"));
    }

    @Test
    @DisplayName("Types generated by the 5.x processor are skipped as well")
    void typesGeneratedByTheFiveXProcessorAreSkipped() {
        final JavaFileObject generated =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SomePojo",
                        "package some.other.pck;",
                        "@javax.annotation.processing.Generated(\"io.github.marmer.annotationprocessing.MatcherGenerationProcessor\")",
                        "public class SomePojo {",
                        "}");

        final Compilation compilation =
                compile(configuration("\"some.other.pck.SomePojo\""), generated);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName("Both configuration annotations on one type are an error at the type")
    void bothConfigurationAnnotationsOnOneTypeAreAnError() {
        final JavaFileObject configuration =
                JavaFileObjects.forSourceLines(
                        "some.pck.SomeConfiguration",
                        "package some.pck;",
                        "@io.github.marmer.matchergenerator.annotations.GenerateMatchers(\"some.other.pck.SomePojo\")",
                        "@io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration("
                                + "\"some.other.pck.SomePojo\")",
                        "public final class SomeConfiguration {",
                        "}");

        final Compilation compilation = compile(configuration, SOME_POJO);

        CompilationSubject.assertThat(compilation).failed();
        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("Use either @GenerateMatchers or @MatcherConfiguration")
                .inFile(configuration);
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName("The model is immutable")
    void theModelIsImmutable() {
        final JavaFileObject bean =
                JavaFileObjects.forSourceLines(
                        "some.other.pck.SimplePojo",
                        "package some.other.pck;",
                        "public class SimplePojo {",
                        "    public String getName() { return \"\"; }",
                        "}");
        compile(configuration("\"some.other.pck.SimplePojo\""), bean);

        final TypeModel model = onlyModel();

        assertThrows(UnsupportedOperationException.class, () -> model.properties().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.nestedTypes().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.typeParameters().clear());
    }
}
