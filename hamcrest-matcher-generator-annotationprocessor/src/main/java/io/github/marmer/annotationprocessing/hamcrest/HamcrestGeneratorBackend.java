package io.github.marmer.annotationprocessing.hamcrest;

import com.squareup.javapoet.JavaFile;
import io.github.marmer.annotationprocessing.model.TypeModel;
import io.github.marmer.annotationprocessing.spi.GeneratedSource;
import io.github.marmer.annotationprocessing.spi.GenerationContext;
import io.github.marmer.annotationprocessing.spi.GeneratorBackend;

/**
 * Generates Hamcrest matchers.
 */
public class HamcrestGeneratorBackend implements GeneratorBackend {

    @Override
    public GeneratedSource generate(final TypeModel model, final GenerationContext context) {
        final MatcherGenerator generator = new MatcherGenerator(model, context);
        return new GeneratedSource(
            model.targetPackage(),
            generator.simpleMatcherName(),
            GeneratedSource.Language.JAVA,
            JavaFile.builder(model.targetPackage(), generator.getPreparedTypeSpecBuilder().build()).build().toString()
        );
    }
}
