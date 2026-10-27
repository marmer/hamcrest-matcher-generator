package io.github.marmer.matchergenerator.hamcrest;

import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatcherGeneratorTest {

    @Test
    void aPropertyWithoutNameStillGetsItsMethodsWithoutAName() {
        final TypeMirror string = stringType();
        final TypeModel model = new TypeModel(
            "some.Pojo",
            "Pojo",
            "some",
            "some",
            List.of(),
            TypeModel.Kind.POJO,
            string,
            List.of(new PropertyModel("", string, "get", Nullability.UNSPECIFIED)),
            List.of());

        final List<String> methodNames = new MatcherGenerator(
            model,
            new GenerationContext("generator", LocalDateTime.of(2020, 1, 1, 0, 0), false))
            .getPreparedTypeSpecBuilder()
            .build()
            .methodSpecs.stream()
            .map(method -> method.name)
            .toList();

        assertTrue(methodNames.contains("with"), () -> "Methods: " + methodNames);
        assertTrue(methodNames.contains("reset"), () -> "Methods: " + methodNames);
    }

    /**
     * A real type mirror, since the generator inspects the kind and the element of the types it is given.
     */
    private static TypeMirror stringType() {
        final AtomicReference<@Nullable TypeMirror> string = new AtomicReference<>();
        CompilationSubject.assertThat(Compiler.javac()
            .withProcessors(new AbstractProcessor() {
                @Override
                public Set<String> getSupportedAnnotationTypes() {
                    return Set.of("*");
                }

                @Override
                public SourceVersion getSupportedSourceVersion() {
                    return SourceVersion.latestSupported();
                }

                @Override
                public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment round) {
                    string.compareAndSet(null, processingEnv.getElementUtils().getTypeElement("java.lang.String").asType());
                    return false;
                }
            })
            .compile(JavaFileObjects.forSourceLines("some.Empty", "package some;", "class Empty {}")))
            .succeeded();
        return Objects.requireNonNull(string.get());
    }
}
