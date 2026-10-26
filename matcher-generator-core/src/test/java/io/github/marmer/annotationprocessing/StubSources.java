package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.model.TypeModel;
import io.github.marmer.annotationprocessing.spi.GeneratedSource;

/**
 * The trivial Java source the test backends generate.
 */
final class StubSources {

    private StubSources() {
    }

    /**
     * An empty public class named like the model's type plus the given suffix, in the model's target package.
     */
    static GeneratedSource emptyClass(final TypeModel model, final String suffix) {
        final String simpleName = model.simpleName() + suffix;
        return new GeneratedSource(
            model.targetPackage(),
            simpleName,
            GeneratedSource.Language.JAVA,
            "package " + model.targetPackage() + ";\npublic class " + simpleName + " {\n}\n"
        );
    }
}
