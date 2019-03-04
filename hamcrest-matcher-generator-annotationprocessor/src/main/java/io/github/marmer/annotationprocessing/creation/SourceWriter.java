package io.github.marmer.annotationprocessing.creation;

import io.github.marmer.annotationprocessing.core.Logger;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

public class SourceWriter {
    private final Filer filer;
    private final Logger logger;

    public SourceWriter(final Filer filer, final Logger logger) {
        this.filer = filer;
        this.logger = logger;
    }

    public void create(final MatcherSourceDescriptor matcherSourceDescriptor) {
        try {
            final JavaFileObject sourceFile = filer.createSourceFile(matcherSourceDescriptor.getType().getFullQualifiedName());
            try (final Writer writer = sourceFile
                    .openWriter()) {
                writer.write(matcherSourceDescriptor.getSource());
                writer.flush();
            }
        } catch (final IOException e) {
            logger.fatal("Cannot write file for: " + matcherSourceDescriptor.getType().getFullQualifiedName());
        }
    }
}
