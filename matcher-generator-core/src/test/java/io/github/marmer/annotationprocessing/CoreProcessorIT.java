package io.github.marmer.annotationprocessing;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.marmer.annotationprocessing.model.Nullability;
import io.github.marmer.annotationprocessing.model.PropertyModel;
import io.github.marmer.annotationprocessing.model.TypeModel;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @BeforeEach
    void resetRecording() {
        RecordingBackend.reset();
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

    private static Compilation compile(final JavaFileObject... sources) {
        return Compiler.javac()
            .withProcessors(new MatcherGenerationProcessor(() -> NOW))
            .compile(Arrays.asList(sources));
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

        final Compilation compilation = compile(configuration("\"some.other.pck.Outer\""), outer);

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
            assertTrue(
                compilation.generatedFile(StandardLocation.SOURCE_OUTPUT, "some.other.pck", name + "Dsl.kt")
                    .isPresent(),
                name);
        }
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
            "io.github.marmer.annotationprocessing.MatcherGenerationProcessor",
            RecordingBackend.contexts().get(0).generatorName());
        assertEquals(NOW, RecordingBackend.contexts().get(0).generationTime());
    }

    @Test
    @DisplayName("Types already generated by this processor are skipped")
    void typesAlreadyGeneratedByThisProcessorAreSkipped() {
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
    @DisplayName("Unresolvable configured names are warned about")
    void unresolvableConfiguredNamesAreWarnedAbout() {
        final Compilation compilation = compile(configuration("\"not.existing.Type\""));

        CompilationSubject.assertThat(compilation).succeeded();
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Neither a type nor a type exists for 'not.existing.Type'");
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
