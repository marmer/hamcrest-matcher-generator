package io.github.marmer.annotationprocessing.core.impl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;

import javax.lang.model.element.Modifier;

public class JavaPoetMatcherGenerator implements MatcherGenerator {
    @Override
    public MatcherSourceDescriptor generateMatcherFor(final MatcherBaseDescriptor descriptor) {

        final JavaFile javaFile = matcherFileFor(descriptor);

        return MatcherSourceDescriptor.builder()
                .type(TypeDescriptor.builder()
                        .packageName(packageFrom(descriptor))
                        .typeName(matcherNameFrom(descriptor)).build())
                .source(javaFile.toString()).build();
    }

    private JavaFile matcherFileFor(final MatcherBaseDescriptor descriptor) {
        final ClassName className = ClassName.get(packageFrom(descriptor), matcherNameFrom(descriptor));
        final TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .build();

        return JavaFile.builder(packageFrom(descriptor), typeSpec).skipJavaLangImports(true).indent("    ").build();
    }

    private String matcherNameFrom(final MatcherBaseDescriptor descriptor) {
        return descriptor.getBase().getTypeName() + "Matcher";
    }

    private String packageFrom(final MatcherBaseDescriptor descriptor) {
        return descriptor.getBase().getPackageName();
    }
}
