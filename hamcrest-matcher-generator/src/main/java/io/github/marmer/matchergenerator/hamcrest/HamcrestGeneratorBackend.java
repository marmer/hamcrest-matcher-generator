package io.github.marmer.matchergenerator.hamcrest;

import com.squareup.javapoet.JavaFile;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;

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
            JavaFile.builder(model.targetPackage(), generator.getPreparedTypeSpecBuilder()
                .addAnnotations(generator.getNullMarkedAnnotations())
                .build()).build().toString()
        );
    }
}
