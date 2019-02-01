package io.github.marmer.annotationprocessing.creation;

import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;

public class SourceWriter {
    public void create(final Filer filer, final MatcherSourceDescriptor matcherSourceDescriptor) {
        // TODO: marmer 31.01.2019 implement me.

        // TODO: marmer 31.01.2019 implement me. This is just some dummy code
        try {
            final JavaFileObject sourceFile = filer.createSourceFile("some.other.pck.SimplePojoMatcher");
            try (final Writer writer = sourceFile
                    .openWriter()) {
                writer.write("package some.other.pck;\n" +
                        "\n" +
                        "public class SimplePojoMatcher{\n" +
                        "}");
                writer.flush();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
