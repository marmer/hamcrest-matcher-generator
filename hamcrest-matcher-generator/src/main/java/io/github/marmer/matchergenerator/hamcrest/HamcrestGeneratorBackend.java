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
            withoutSpaceAfterQualifier(
                JavaFile.builder(model.targetPackage(), generator.getTopLevelTypeSpec()).build().toString())
        );
    }

    /**
     * JavaPoet writes a type use annotation on a nested type as {@code Map. @Nullable Entry}. Both spellings are the
     * same Java, the one without the space is how the annotation is usually written.
     */
    private static String withoutSpaceAfterQualifier(final String source) {
        return source.replaceAll("(\\w)\\. (@Nullable )", "$1.$2");
    }
}
