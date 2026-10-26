package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.model.TypeModel;
import io.github.marmer.annotationprocessing.spi.GeneratedSource;
import io.github.marmer.annotationprocessing.spi.GenerationContext;
import io.github.marmer.annotationprocessing.spi.GeneratorBackend;

/**
 * Additional test backends proving that every backend runs for every type and that a backend may emit Kotlin.
 */
final class FileOnlyBackends {

    private FileOnlyBackends() {
    }

    /**
     * Generates a second Java file per type.
     */
    public static class SecondJavaBackend implements GeneratorBackend {
        @Override
        public GeneratedSource generate(final TypeModel model, final GenerationContext context) {
            return StubSources.emptyClass(model, "Second");
        }
    }

    /**
     * Generates a Kotlin file per type.
     */
    public static class KotlinBackend implements GeneratorBackend {
        @Override
        public GeneratedSource generate(final TypeModel model, final GenerationContext context) {
            return new GeneratedSource(
                model.targetPackage(),
                model.simpleName() + "Dsl",
                GeneratedSource.Language.KOTLIN,
                "package " + model.targetPackage() + "\n\nclass " + model.simpleName() + "Dsl\n"
            );
        }
    }
}
