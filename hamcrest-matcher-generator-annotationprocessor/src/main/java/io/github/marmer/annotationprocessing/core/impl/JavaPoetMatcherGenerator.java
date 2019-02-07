package io.github.marmer.annotationprocessing.core.impl;

import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;

public class JavaPoetMatcherGenerator implements MatcherGenerator {
    @Override
    public MatcherSourceDescriptor generateMatcherFor(final MatcherBaseDescriptor descriptor) {
        return MatcherSourceDescriptor.builder()
                .type(TypeDescriptor.builder()
                        .packageName(descriptor.getBase().getPackageName())
                        .typeName(descriptor.getBase().getTypeName() + "Matcher").build())
                .source("package some.other.pck;\n" +
                        "\n" +
                        "public class SimplePojoMatcher{\n" +
                        "}").build();
    }
}
