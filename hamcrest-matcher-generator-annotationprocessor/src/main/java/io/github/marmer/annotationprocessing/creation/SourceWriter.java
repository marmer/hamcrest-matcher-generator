package io.github.marmer.annotationprocessing.creation;

import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

public class SourceWriter {
    public void create(final Filer filer, final MatcherSourceDescriptor matcherSourceDescriptor) {
        try {
            final JavaFileObject sourceFile = filer.createSourceFile(matcherSourceDescriptor.getType().getPackageName() + "." + matcherSourceDescriptor.getType().getTypeName());
            try (final Writer writer = sourceFile
                    .openWriter()) {
                writer.write(matcherSourceDescriptor.getSource());
                writer.flush();
            }
        } catch (final IOException e) {
            // TODO: marmer 07.02.2019 don't forget to log (somehow)
            e.printStackTrace();
        }
    }
}
