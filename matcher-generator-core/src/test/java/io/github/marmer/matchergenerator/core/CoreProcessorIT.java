package io.github.marmer.matchergenerator.core;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests everything the core decides, seen through the recording test backend registered behind the SPI.
 */
class CoreProcessorIT {

    private static final LocalDateTime NOW = LocalDateTime.of(2020, 1, 1, 0, 0);

    /**
     * 5.x detects {@code Object.getClass()} as a property, and the generated matchers rely on it.
     */
    private static final String CLASS_PROPERTY = "class:java.lang.Class<?>:getClass";

    @TempDir
    Path kotlinOutput;

    @BeforeEach
    void resetRecording() {
        RecordingBackend.reset();
    }

    private static JavaFileObject generateMatchersConfiguration(final String configuredTypes) {
        return JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            "package some.pck;",
            "import io.github.marmer.matchergenerator.annotations.GenerateMatchers;",
            "@GenerateMatchers(" + configuredTypes + ")",
            "public final class SomeConfiguration {",
            "}"
        );
    }

    private static JavaFileObject configuration(final String configuredTypes) {
        return JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            "package some.pck;",
            "import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;",
            "@MatcherConfiguration(" + configuredTypes + ")",
            "public final class SomeConfiguration {",
            "}"
        );
    }

    private Compilation compile(final JavaFileObject... sources) {
        return compile(Compiler.javac().withOptions("-Akapt.kotlin.generated=" + kotlinOutput), sources);
    }

    private static Compilation compile(final Compiler compiler, final JavaFileObject... sources) {
        return compiler
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(sources));
    }

    private Compilation compileVerbose(final JavaFileObject... sources) {
        return compile(
            Compiler.javac().withOptions("-Akapt.kotlin.generated=" + kotlinOutput, "-Amatchergenerator.verbose"),
            sources);
    }

    private static TypeModel onlyModel() {
        final List<TypeModel> models = RecordingBackend.models();
        assertEquals(1, models.size(), () -> "Recorded models: " + models);
        return models.get(0);
    }

    private static List<String> describe(final List<PropertyModel> properties) {
        return properties.stream()
            .map(property -> property.name() + ":" + property.type() + ":" + property.accessor())
            .collect(Collectors.toList());
    }

    @Test
    @DisplayName("A bean is described by its public non static getters")
    void aBeanIsDescribedByItsPublicNonStaticGetters() {
        final JavaFileObject bean = JavaFileObjects.forSourceLines(
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
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.SimplePojo\""), bean);

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
                CLASS_PROPERTY
            ),
            describe(model.properties()));
        model.properties().forEach(property -> assertEquals(Nullability.UNSPECIFIED, property.nullability()));
    }

    @Test
    @DisplayName("A Kotlin class is described by its Kotlin getters, whatever their return type")
    void aKotlinClassIsDescribedByItsKotlinGetters() {
        final JavaFileObject kotlinProbe = JavaFileObjects.forSourceLines(
            "some.other.pck.KotlinProbe",
            "package some.other.pck;",
            "@kotlin.Metadata",
            "public class KotlinProbe {",
            "    public boolean getEnabled() { return true; }",
            "    public Boolean isX() { return true; }",
            "    public String isLabel() { return \"\"; }",
            "    public boolean isPrimitive() { return true; }",
            "    public String notAProperty() { return \"\"; }",
            "}"
        );

        final Compilation compilation = compile(
            configuration("\"some.other.pck.KotlinProbe\""), kotlinProbe, KOTLIN_METADATA);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
            List.of(
                "enabled:boolean:getEnabled",
                "isX:java.lang.Boolean:isX",
                "isLabel:java.lang.String:isLabel",
                "primitive:boolean:isPrimitive",
                CLASS_PROPERTY
            ),
            describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("In a Kotlin class an upper case letter must follow the is and get prefix")
    void kotlinAccessorsNeedAnUpperCaseLetterAfterThePrefix() {
        final JavaFileObject kotlinProbe = JavaFileObjects.forSourceLines(
            "some.other.pck.KotlinProbe",
            "package some.other.pck;",
            "@kotlin.Metadata",
            "public class KotlinProbe {",
            "    public String getName() { return \"\"; }",
            "    public String getaway() { return \"\"; }",
            "    public boolean isActive() { return true; }",
            "    public boolean isolate() { return true; }",
            "    public String isolated() { return \"\"; }",
            "}"
        );

        final Compilation compilation = compile(
            configuration("\"some.other.pck.KotlinProbe\""), kotlinProbe, KOTLIN_METADATA);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
            List.of("name:java.lang.String:getName", "active:boolean:isActive", CLASS_PROPERTY),
            describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("Kotlin getters whose names are not Java identifiers are skipped (QS-U4)")
    void kotlinGettersWithMangledNamesAreSkipped(@TempDir final Path classes) throws IOException {
        final JavaFileObject mangled = JavaFileObjects.forSourceLines(
            "some.other.pck.Mangled",
            "package some.other.pck;",
            "@kotlin.Metadata",
            "public class Mangled {",
            "    public String getPlain() { return \"\"; }",
            "    public String getXyzQ() { return \"\"; }",
            "}"
        );
        final Compilation compiledMangled = Compiler.javac().compile(mangled, KOTLIN_METADATA);
        CompilationSubject.assertThat(compiledMangled).succeeded();
        writeClassFile(compiledMangled, classes, "some.other.pck.Mangled", "getXyzQ", "getX-zQ");
        writeClassFile(compiledMangled, classes, "kotlin.Metadata", null, null);

        final Compilation compilation = Compiler.javac()
            .withOptions("-Akapt.kotlin.generated=" + kotlinOutput)
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .withClasspath(classPathWith(classes))
            .compile(configuration("\"some.other.pck.Mangled\""));

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
            List.of("plain:java.lang.String:getPlain", CLASS_PROPERTY),
            describe(onlyModel().properties()));
    }

    /**
     * The directory with the copied class files, followed by the test class path that compile-testing would use.
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
     * Copies a compiled class file into the class path, optionally renaming one method inside it. Kotlin emits names
     * such as {@code getX-abc} for mangled accessors, which javac refuses to compile from source. The rename keeps the
     * length, so the constant pool stays valid.
     */
    private static void writeClassFile(final Compilation compilation, final Path classes, final String name,
                                       final String from, final String to) throws IOException {
        final String path = name.replace('.', '/') + ".class";
        final byte[] bytes = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, path)
            .orElseThrow()
            .openInputStream()
            .readAllBytes();
        final String content = new String(bytes, StandardCharsets.ISO_8859_1);
        final String renamed = from == null ? content : content.replace(from, to);
        final Path target = classes.resolve(path);
        Files.createDirectories(target.getParent());
        Files.write(target, renamed.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    @DisplayName("A record is described by its components")
    void aRecordIsDescribedByItsComponents() {
        final JavaFileObject record = JavaFileObjects.forSourceLines(
            "some.other.pck.PersonRecord",
            "package some.other.pck;",
            "public record PersonRecord(String name, int age) {",
            "    public String getNickname() { return \"\"; }",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.PersonRecord\""), record);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(TypeModel.Kind.RECORD, model.kind());
        assertEquals(
            List.of(
                "name:java.lang.String:name",
                "age:int:age",
                "nickname:java.lang.String:getNickname",
                CLASS_PROPERTY
            ),
            describe(model.properties()));
    }

    @Test
    @DisplayName("Inherited properties of super classes and interfaces are part of the model")
    void inheritedPropertiesArePartOfTheModel() {
        final JavaFileObject base = JavaFileObjects.forSourceLines(
            "some.other.pck.Base",
            "package some.other.pck;",
            "public class Base {",
            "    public String getFromBase() { return \"\"; }",
            "}"
        );
        final JavaFileObject iface = JavaFileObjects.forSourceLines(
            "some.other.pck.Named",
            "package some.other.pck;",
            "public interface Named {",
            "    String getFromInterface();",
            "}"
        );
        final JavaFileObject child = JavaFileObjects.forSourceLines(
            "some.other.pck.Child",
            "package some.other.pck;",
            "public class Child extends Base implements Named {",
            "    public String getOwn() { return \"\"; }",
            "    public String getFromInterface() { return \"\"; }",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.Child\""), base, iface, child);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
            List.of(
                "own:java.lang.String:getOwn",
                "fromInterface:java.lang.String:getFromInterface",
                "fromBase:java.lang.String:getFromBase",
                CLASS_PROPERTY
            ),
            describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("Generic types keep their type parameters and generic property types")
    void genericTypesKeepTheirTypeParameters() {
        final JavaFileObject generic = JavaFileObjects.forSourceLines(
            "some.other.pck.Box",
            "package some.other.pck;",
            "import java.util.List;",
            "public class Box<T, U extends Number> {",
            "    public T getContent() { return null; }",
            "    public List<? extends U> getNumbers() { return null; }",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.Box\""), generic);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(List.of("T", "U"), model.typeParameters());
        assertEquals("some.other.pck.Box<T,U>", model.type().toString());
        assertEquals(
            List.of(
                "content:T:getContent",
                "numbers:java.util.List<? extends U>:getNumbers",
                CLASS_PROPERTY
            ),
            describe(model.properties()));
    }

    @Test
    @DisplayName("Public member types are nested models and other member types are skipped with a note")
    void publicMemberTypesAreNestedModels() {
        final JavaFileObject outer = JavaFileObjects.forSourceLines(
            "some.other.pck.Outer",
            "package some.other.pck;",
            "public class Outer {",
            "    public String getOuterProperty() { return \"\"; }",
            "    public static class Inner {",
            "        public String getInnerProperty() { return \"\"; }",
            "    }",
            "    class Hidden {",
            "    }",
            "}"
        );

        final Compilation compilation = compileVerbose(configuration("\"some.other.pck.Outer\""), outer);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals(1, model.nestedTypes().size());
        final TypeModel inner = model.nestedTypes().get(0);
        assertEquals("some.other.pck.Outer.Inner", inner.qualifiedName());
        assertEquals("Inner", inner.simpleName());
        assertEquals(List.of("innerProperty:java.lang.String:getInnerProperty", CLASS_PROPERTY), describe(inner.properties()));
        CompilationSubject.assertThat(compilation)
            .hadNoteContaining("Matcher generation skipped for non public type: some.other.pck.Outer.Hidden");
    }

    @Test
    @DisplayName("A configured package prefix decides the target package")
    void aConfiguredPackagePrefixDecidesTheTargetPackage() {
        final JavaFileObject bean = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo",
            "package some.other.pck;",
            "public class SomePojo {",
            "}"
        );
        final JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            "package some.pck;",
            "import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;",
            "@MatcherConfiguration(value = \"some.other.pck.SomePojo\", generation = "
                + "@MatcherConfiguration.GenerationConfiguration(packageConfig = "
                + "@MatcherConfiguration.GenerationConfiguration.PackageConfiguration(\"prefix.\")))",
            "public final class SomeConfiguration {",
            "}"
        );

        final Compilation compilation = compile(configuration, bean);

        CompilationSubject.assertThat(compilation).succeeded();
        final TypeModel model = onlyModel();
        assertEquals("some.other.pck", model.packageName());
        assertEquals("prefix.some.other.pck", model.targetPackage());
        assertTrue(compilation.generatedSourceFile("prefix.some.other.pck.SomePojoRecorded").isPresent());
    }

    @Test
    @DisplayName("Every backend runs for every configured type, whatever language it generates")
    void everyBackendRunsForEveryConfiguredType() {
        final JavaFileObject first = JavaFileObjects.forSourceLines(
            "some.other.pck.First",
            "package some.other.pck;",
            "public class First {",
            "}"
        );
        final JavaFileObject second = JavaFileObjects.forSourceLines(
            "some.other.pck.Second",
            "package some.other.pck;",
            "public class Second {",
            "}"
        );

        final Compilation compilation = compile(
            configuration("{\"some.other.pck.First\", \"some.other.pck.Second\"}"), first, second);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
            List.of("some.other.pck.First", "some.other.pck.Second"),
            RecordingBackend.models().stream().map(TypeModel::qualifiedName).collect(Collectors.toList()));
        for (final String name : List.of("First", "Second")) {
            assertTrue(compilation.generatedSourceFile("some.other.pck." + name + "Recorded").isPresent(), name);
            assertTrue(compilation.generatedSourceFile("some.other.pck." + name + "Second").isPresent(), name);
            assertTrue(Files.isRegularFile(kotlinOutput.resolve("some/other/pck/" + name + "Dsl.kt")), name);
        }
    }

    @Test
    @DisplayName("Kotlin sources go to the directory KAPT names")
    void kotlinSourcesGoToTheDirectoryKaptNames() throws IOException {
        final JavaFileObject bean = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo",
            "package some.other.pck;",
            "public class SomePojo {",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.SomePojo\""), bean);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
            "package some.other.pck\n\nclass SomePojoDsl\n",
            Files.readString(kotlinOutput.resolve("some/other/pck/SomePojoDsl.kt")));
        assertFalse(compilation.generatedFile(StandardLocation.SOURCE_OUTPUT, "some.other.pck", "SomePojoDsl.kt")
            .isPresent());
    }

    @Test
    @DisplayName("Kotlin output without the KAPT directory is an error naming backend and type")
    void kotlinOutputWithoutTheKaptDirectoryIsAnError() {
        final JavaFileObject bean = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo",
            "package some.other.pck;",
            "public class SomePojo {",
            "}"
        );

        final Compilation compilation = compile(Compiler.javac(), configuration("\"some.other.pck.SomePojo\""), bean);

        CompilationSubject.assertThat(compilation).failed();
        CompilationSubject.assertThat(compilation).hadErrorCount(1);
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("io.github.marmer.matchergenerator.core.FileOnlyBackends$KotlinBackend");
        CompilationSubject.assertThat(compilation).hadErrorContaining("some.other.pck.SomePojo");
        CompilationSubject.assertThat(compilation).hadErrorContaining("kapt.kotlin.generated");
    }

    @Test
    @DisplayName("Backends get the generator name and the generation time")
    void backendsGetTheGeneratorNameAndTheGenerationTime() {
        final JavaFileObject bean = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo",
            "package some.other.pck;",
            "public class SomePojo {",
            "}"
        );

        compile(configuration("\"some.other.pck.SomePojo\""), bean);

        assertEquals(1, RecordingBackend.contexts().size());
        assertEquals(
            "io.github.marmer.matchergenerator.core.MatcherGenerationProcessor",
            RecordingBackend.contexts().get(0).generatorName());
        assertEquals(NOW, RecordingBackend.contexts().get(0).generationTime());
    }

    @Test
    @DisplayName("Types already generated by this processor are skipped")
    void typesAlreadyGeneratedByThisProcessorAreSkipped() {
        final JavaFileObject generated = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo",
            "package some.other.pck;",
            "@javax.annotation.processing.Generated(\"io.github.marmer.matchergenerator.core.MatcherGenerationProcessor\")",
            "public class SomePojo {",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.SomePojo\""), generated);

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
    @DisplayName("An unresolvable name of GenerateMatchers is one error at the annotation value, no follow-up")
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
    @DisplayName("Every unresolvable GenerateMatchers name yields exactly one error and nothing else is reported")
    void everyUnresolvableNameYieldsExactlyOneError() {
        final JavaFileObject configuration = generateMatchersConfiguration(
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
        assertEquals(List.of(), compilation.warnings().stream().map(d -> d.getMessage(null)).toList());
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName("Every unresolvable name is reported at its own annotation value")
    void unresolvableNamesAreReportedAtTheirOwnValue() {
        final JavaFileObject configuration = generateMatchersConfiguration("{\"not.existing.First\", \"not.existing.Second\"}");

        final Compilation compilation = compile(configuration);

        CompilationSubject.assertThat(compilation).failed();
        assertEquals(2, compilation.errors().size(), () -> "Errors: " + compilation.errors());
        final long firstColumn = columnOf(compilation, "'not.existing.First'");
        final long secondColumn = columnOf(compilation, "'not.existing.Second'");
        assertTrue(firstColumn > 0 && secondColumn > firstColumn, firstColumn + " / " + secondColumn);
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("'not.existing.First'").inFile(configuration).onLine(3).atColumn((int) firstColumn);
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("'not.existing.Second'").inFile(configuration).onLine(3).atColumn((int) secondColumn);
    }

    private static long columnOf(final Compilation compilation, final String messagePart) {
        return compilation.errors().stream()
            .filter(diagnostic -> diagnostic.getMessage(null).contains(messagePart))
            .mapToLong(javax.tools.Diagnostic::getColumnNumber)
            .findFirst()
            .orElseThrow();
    }

    @Test
    @DisplayName("Every unresolvable name of the deprecated alias yields exactly one warning")
    void everyUnresolvableAliasNameYieldsExactlyOneWarning() {
        final JavaFileObject configuration = configuration(
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
        for (final String name : List.of("\"\"", "\"some.other.pck.\"", "\"1abc\"", "\"some..pck\"", "\"*\"",
            "\"some.other.pck.SomePojo.Missing\"", "\" \"")) {
            final Compilation compilation = compile(generateMatchersConfiguration(name), SOME_POJO);

            CompilationSubject.assertThat(compilation).failed();
            assertEquals(1, compilation.errors().size(), () -> name + " errors: " + compilation.errors());
            assertEquals(List.of(), RecordingBackend.models());
            RecordingBackend.reset();
        }
    }

    @Test
    @DisplayName("A configured type without properties is generated and warned about")
    void aConfiguredTypeWithoutPropertiesIsWarnedAbout() {
        final JavaFileObject empty = JavaFileObjects.forSourceLines(
            "some.other.pck.EmptyPojo",
            "package some.other.pck;",
            "public class EmptyPojo {",
            "    public void doSomething() { }",
            "}"
        );
        final JavaFileObject emptyInterface = JavaFileObjects.forSourceLines(
            "some.other.pck.EmptyInterface",
            "package some.other.pck;",
            "public interface EmptyInterface {",
            "    void doSomething();",
            "}"
        );

        final Compilation compilation = compile(
            configuration("{\"some.other.pck.EmptyPojo\", \"some.other.pck.EmptyInterface\"}"), empty, emptyInterface);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Type 'some.other.pck.EmptyPojo' has no properties");
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Type 'some.other.pck.EmptyInterface' has no properties");
        assertEquals(2, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(
            List.of("some.other.pck.EmptyPojo", "some.other.pck.EmptyInterface"),
            RecordingBackend.models().stream().map(TypeModel::qualifiedName).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("A configured nested type without properties is warned about, not its unconfigured container")
    void aConfiguredNestedTypeWithoutPropertiesIsWarnedAbout() {
        final JavaFileObject outer = JavaFileObjects.forSourceLines(
            "some.other.pck.Outer",
            "package some.other.pck;",
            "public class Outer {",
            "    public String getOuterProperty() { return \"\"; }",
            "    public static class Inner {",
            "        public void doSomething() { }",
            "    }",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.Outer.Inner\""), outer);

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Type 'some.other.pck.Outer.Inner' has no properties");
        assertEquals(1, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(1, RecordingBackend.models().size());
    }

    @Test
    @DisplayName("A configured nested type with properties is not warned about, whatever its container holds")
    void aConfiguredNestedTypeWithPropertiesIsNotWarnedAbout() {
        final JavaFileObject outer = JavaFileObjects.forSourceLines(
            "some.other.pck.Outer",
            "package some.other.pck;",
            "public class Outer {",
            "    public static class Inner {",
            "        public String getInnerProperty() { return \"\"; }",
            "    }",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.Outer.Inner\""), outer);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(1, RecordingBackend.models().size());
    }

    @Test
    @DisplayName("A Kotlin file facade without properties is not warned about, it holds only top level functions")
    void aKotlinFileFacadeIsNotWarnedAbout() {
        final JavaFileObject metadata = JavaFileObjects.forSourceLines(
            "kotlin.Metadata",
            "package kotlin;",
            "public @interface Metadata {",
            "    int k() default 1;",
            "}"
        );
        final JavaFileObject facade = JavaFileObjects.forSourceLines(
            "some.other.pck.HelloKt",
            "package some.other.pck;",
            "@kotlin.Metadata(k = 2)",
            "public final class HelloKt {",
            "    public static String hello() { return \"\"; }",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.HelloKt\""), metadata, facade);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
        assertEquals(1, RecordingBackend.models().size());
    }

    @Test
    @DisplayName("A type with at least one property is not warned about")
    void aTypeWithPropertiesIsNotWarnedAbout() {
        final Compilation compilation = compile(configuration("\"some.other.pck.SomePojo\""), SOME_POJO);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
    }

    @Test
    @DisplayName("Without the verbose option no notes are emitted at all")
    void withoutTheVerboseOptionNoNotesAreEmitted() {
        final Compilation compilation = compile(configurationWith(
            "io.github.marmer.matchergenerator.annotations.GenerateMatchers",
            "@GenerateMatchers(\"some.other.pck.SkippingPojo\")"), skippingPojo());

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of(), compilation.notes().stream().map(d -> d.getMessage(null)).toList());
    }

    @Test
    @DisplayName("The verbose option emits one note per skipped property with its reason")
    void theVerboseOptionEmitsOneNotePerSkippedProperty() {
        final Compilation compilation = compileVerbose(configuration("\"some.other.pck.SkippingPojo\""), skippingPojo());

        CompilationSubject.assertThat(compilation).succeeded();
        final List<String> skipNotes = compilation.notes().stream()
            .map(d -> d.getMessage(null))
            .filter(message -> message.startsWith("Skipped property"))
            .collect(Collectors.toList());
        assertEquals(
            List.of(
                "Skipped property 'getProtected' of some.other.pck.SkippingPojo: method is not public",
                "Skipped property 'getPrivate' of some.other.pck.SkippingPojo: method is not public",
                "Skipped property 'getStatic' of some.other.pck.SkippingPojo: method is static",
                "Skipped property 'getWithParameter' of some.other.pck.SkippingPojo: method takes parameters",
                "Skipped property 'getVoid' of some.other.pck.SkippingPojo: method returns void",
                "Skipped property 'getBooleanWithGet' of some.other.pck.SkippingPojo: "
                    + "a boolean property must use the 'is' prefix",
                "Skipped property 'isNotBoolean' of some.other.pck.SkippingPojo: "
                    + "the 'is' prefix requires a boolean return type",
                "Skipped property 'get' of some.other.pck.SkippingPojo: no property name follows the 'get' prefix"
            ),
            skipNotes);
        assertEquals(
            List.of("name:java.lang.String:getName", CLASS_PROPERTY),
            describe(onlyModel().properties()));
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
            "}"
        );
    }

    @Test
    @DisplayName("Public non static inner classes are nested models as well")
    void publicInnerClassesAreNestedModels() {
        final JavaFileObject outer = JavaFileObjects.forSourceLines(
            "some.other.pck.Holder",
            "package some.other.pck;",
            "public class Holder {",
            "    public String getHolderProperty() { return \"\"; }",
            "    public class Inner {",
            "        public String getInnerProperty() { return \"\"; }",
            "    }",
            "}"
        );

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
        final JavaFileObject node = JavaFileObjects.forSourceLines(
            "some.other.pck.Node",
            "package some.other.pck;",
            "public class Node<T extends Node<T>> {",
            "    public T getSelf() { return null; }",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.Node\""), node);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of("T"), onlyModel().typeParameters());
        assertEquals(List.of("self:T:getSelf", CLASS_PROPERTY), describe(onlyModel().properties()));
    }

    @Test
    @DisplayName("Cyclic references between configured types are modelled for both types")
    void cyclicReferencesBetweenConfiguredTypesAreModelled() {
        final JavaFileObject parent = JavaFileObjects.forSourceLines(
            "some.other.pck.Parent",
            "package some.other.pck;",
            "public class Parent {",
            "    public Child getChild() { return null; }",
            "}"
        );
        final JavaFileObject child = JavaFileObjects.forSourceLines(
            "some.other.pck.Child",
            "package some.other.pck;",
            "public class Child {",
            "    public Parent getParent() { return null; }",
            "}"
        );

        final Compilation compilation = compile(
            configuration("{\"some.other.pck.Parent\", \"some.other.pck.Child\"}"), parent, child);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(
            List.of("some.other.pck.Parent", "some.other.pck.Child"),
            RecordingBackend.models().stream().map(TypeModel::qualifiedName).collect(Collectors.toList()));
        assertEquals(0, compilation.warnings().size(), () -> "Warnings: " + compilation.warnings());
    }

    @Test
    @DisplayName("Cyclic inheritance is reported by the compiler without a processor crash")
    void cyclicInheritanceDoesNotCrashTheProcessor() {
        final JavaFileObject first = JavaFileObjects.forSourceLines(
            "some.other.pck.First",
            "package some.other.pck;",
            "public class First extends Second {",
            "}"
        );
        final JavaFileObject second = JavaFileObjects.forSourceLines(
            "some.other.pck.Second",
            "package some.other.pck;",
            "public class Second extends First {",
            "    public String getName() { return \"\"; }",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.First\""), first, second);

        CompilationSubject.assertThat(compilation).failed();
        assertEquals(List.of(), compilation.errors().stream()
            .map(d -> d.getMessage(null))
            .filter(message -> !message.contains("cyclic inheritance"))
            .toList());
    }

    private static JavaFileObject configurationWith(final String annotationImport, final String annotation) {
        return JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            "package some.pck;",
            "import " + annotationImport + ";",
            annotation,
            "public final class SomeConfiguration {",
            "}"
        );
    }

    private static String summary(final TypeModel model) {
        return model.qualifiedName() + "|" + model.targetPackage() + "|" + model.kind() + "|"
            + model.typeParameters() + "|" + model.type() + "|" + describe(model.properties()) + "|"
            + model.nestedTypes().stream().map(CoreProcessorIT::summary).collect(Collectors.toList());
    }

    private static byte[] classFile(final Compilation compilation, final String name) throws IOException {
        return compilation.generatedFile(StandardLocation.CLASS_OUTPUT, name.replace('.', '/') + ".class")
            .orElseThrow()
            .openInputStream()
            .readAllBytes();
    }

    private static boolean contains(final byte[] bytes, final String text) {
        return new String(bytes, StandardCharsets.ISO_8859_1).contains(text);
    }

    /**
     * A stand-in for {@code kotlin.Metadata}, which marks the classes that Kotlin compiled. The core only checks the name.
     */
    private static final JavaFileObject KOTLIN_METADATA = JavaFileObjects.forSourceLines(
        "kotlin.Metadata",
        "package kotlin;",
        "public @interface Metadata {",
        "}"
    );

    private static final JavaFileObject SOME_POJO = JavaFileObjects.forSourceLines(
        "some.other.pck.SomePojo",
        "package some.other.pck;",
        "public class SomePojo {",
        "    public String getName() { return \"\"; }",
        "    public boolean isActive() { return true; }",
        "}"
    );

    @Test
    @DisplayName("GenerateMatchers and the deprecated MatcherConfiguration produce identical models")
    void generateMatchersAndTheDeprecatedAliasProduceIdenticalModels() {
        final Compilation current = compile(configurationWith(
            "io.github.marmer.matchergenerator.annotations.GenerateMatchers",
            "@GenerateMatchers(value = \"some.other.pck.SomePojo\", generation = "
                + "@GenerateMatchers.GenerationConfiguration(packageConfig = "
                + "@GenerateMatchers.GenerationConfiguration.PackageConfiguration(\"prefix.\")))"), SOME_POJO);
        CompilationSubject.assertThat(current).succeeded();
        final List<String> currentSummary = RecordingBackend.models().stream()
            .map(CoreProcessorIT::summary).collect(Collectors.toList());
        final List<String> currentGenerated = current.generatedSourceFiles().stream()
            .map(JavaFileObject::getName).collect(Collectors.toList());
        RecordingBackend.reset();

        final Compilation alias = compile(configurationWith(
            "io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration",
            "@MatcherConfiguration(value = \"some.other.pck.SomePojo\", generation = "
                + "@MatcherConfiguration.GenerationConfiguration(packageConfig = "
                + "@MatcherConfiguration.GenerationConfiguration.PackageConfiguration(\"prefix.\")))"), SOME_POJO);
        CompilationSubject.assertThat(alias).succeeded();

        assertEquals(1, currentSummary.size());
        assertEquals(currentSummary, RecordingBackend.models().stream()
            .map(CoreProcessorIT::summary).collect(Collectors.toList()));
        assertEquals(currentGenerated, alias.generatedSourceFiles().stream()
            .map(JavaFileObject::getName).collect(Collectors.toList()));
        assertEquals("prefix.some.other.pck", RecordingBackend.models().get(0).targetPackage());
    }

    @Test
    @DisplayName("GenerateMatchers keeps its defaults like the alias does")
    void generateMatchersKeepsItsDefaults() {
        compile(configurationWith(
            "io.github.marmer.matchergenerator.annotations.GenerateMatchers",
            "@GenerateMatchers(\"some.other.pck\")"), SOME_POJO);

        assertEquals("some.other.pck", onlyModel().targetPackage());
    }

    @Test
    @DisplayName("The deprecated MatcherConfiguration leaves no trace in compiled class files (QS-N1)")
    void theDeprecatedAliasLeavesNoTraceInCompiledClassFiles() throws IOException {
        final Compilation alias = compile(configurationWith(
            "io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration",
            "@MatcherConfiguration(\"some.other.pck.SomePojo\")"), SOME_POJO);
        CompilationSubject.assertThat(alias).succeeded();
        final Compilation current = compile(configurationWith(
            "io.github.marmer.matchergenerator.annotations.GenerateMatchers",
            "@GenerateMatchers(\"some.other.pck.SomePojo\")"), SOME_POJO);
        CompilationSubject.assertThat(current).succeeded();

        final byte[] aliasClass = classFile(alias, "some.pck.SomeConfiguration");
        assertFalse(contains(aliasClass, "MatcherConfiguration"));
        assertFalse(contains(aliasClass, "beanmatcher"));
        // proves the check above can fail: the CLASS retained annotation is visible in the class file
        assertTrue(contains(classFile(current, "some.pck.SomeConfiguration"), "GenerateMatchers"));
    }

    @Test
    @DisplayName("Types generated by the 5.x processor are skipped as well")
    void typesGeneratedByTheFiveXProcessorAreSkipped() {
        final JavaFileObject generated = JavaFileObjects.forSourceLines(
            "some.other.pck.SomePojo",
            "package some.other.pck;",
            "@javax.annotation.processing.Generated(\"io.github.marmer.annotationprocessing.MatcherGenerationProcessor\")",
            "public class SomePojo {",
            "}"
        );

        final Compilation compilation = compile(configuration("\"some.other.pck.SomePojo\""), generated);

        CompilationSubject.assertThat(compilation).succeeded();
        assertEquals(List.of(), RecordingBackend.models());
    }

    @Test
    @DisplayName("Both configuration annotations on one type are an error at the type")
    void bothConfigurationAnnotationsOnOneTypeAreAnError() {
        final JavaFileObject configuration = JavaFileObjects.forSourceLines(
            "some.pck.SomeConfiguration",
            "package some.pck;",
            "@io.github.marmer.matchergenerator.annotations.GenerateMatchers(\"some.other.pck.SomePojo\")",
            "@io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration("
                + "\"some.other.pck.SomePojo\")",
            "public final class SomeConfiguration {",
            "}"
        );

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
        final JavaFileObject bean = JavaFileObjects.forSourceLines(
            "some.other.pck.SimplePojo",
            "package some.other.pck;",
            "public class SimplePojo {",
            "    public String getName() { return \"\"; }",
            "}"
        );
        compile(configuration("\"some.other.pck.SimplePojo\""), bean);

        final TypeModel model = onlyModel();

        assertThrows(UnsupportedOperationException.class, () -> model.properties().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.nestedTypes().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.typeParameters().clear());
    }
}
