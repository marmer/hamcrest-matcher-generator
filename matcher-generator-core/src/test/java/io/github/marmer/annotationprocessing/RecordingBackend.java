package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.model.TypeModel;
import io.github.marmer.annotationprocessing.spi.GeneratedSource;
import io.github.marmer.annotationprocessing.spi.GenerationContext;
import io.github.marmer.annotationprocessing.spi.GeneratorBackend;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test backend registered via {@link java.util.ServiceLoader} that records every model it receives. It is the seam
 * for everything the core decides.
 */
public class RecordingBackend implements GeneratorBackend {
    private static final List<TypeModel> MODELS = new CopyOnWriteArrayList<>();
    private static final List<GenerationContext> CONTEXTS = new CopyOnWriteArrayList<>();

    static List<TypeModel> models() {
        return List.copyOf(MODELS);
    }

    static List<GenerationContext> contexts() {
        return List.copyOf(CONTEXTS);
    }

    static void reset() {
        MODELS.clear();
        CONTEXTS.clear();
    }

    @Override
    public GeneratedSource generate(final TypeModel model, final GenerationContext context) {
        MODELS.add(model);
        CONTEXTS.add(context);
        final String simpleName = model.simpleName() + "Recorded";
        return new GeneratedSource(
            model.targetPackage(),
            simpleName,
            GeneratedSource.Language.JAVA,
            "package " + model.targetPackage() + ";\npublic class " + simpleName + " {\n}\n"
        );
    }
}
