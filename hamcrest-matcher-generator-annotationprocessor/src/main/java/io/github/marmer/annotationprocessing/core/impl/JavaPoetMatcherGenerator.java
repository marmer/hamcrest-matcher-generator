package io.github.marmer.annotationprocessing.core.impl;

import com.squareup.javapoet.*;
import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;
import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import java.time.LocalDate;

public class JavaPoetMatcherGenerator implements MatcherGenerator {

    private static final String INNER_FIELD_NAME = "beanPropertyMatcher";

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
                .addField(innerMatcherField(descriptor))
                .addMethod(constructor(descriptor))
                .addAnnotation(generatedAnnotationFor())
                .build();

        return JavaFile.builder(packageFrom(descriptor), typeSpec).skipJavaLangImports(true).indent("    ").build();
    }

    private FieldSpec innerMatcherField(final MatcherBaseDescriptor descriptor) {
        final ParameterizedTypeName fieldType = ParameterizedTypeName.get(ClassName.get(BeanPropertyMatcher.class), ClassName.get(descriptor.getBase().getPackageName(), descriptor.getBase().getTypeName()));
        return FieldSpec.builder(fieldType, INNER_FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private MethodSpec constructor(final MatcherBaseDescriptor descriptor) {
        final TypeDescriptor base = descriptor.getBase();
        return MethodSpec.constructorBuilder()
                .addStatement("$L = new BeanPropertyMatcher<$T>($T.class)", INNER_FIELD_NAME, ClassName.get(base.getPackageName(), base.getTypeName()), ClassName.get(base.getPackageName(), base.getTypeName()))
                .addModifiers(
                        Modifier.PUBLIC)
                .build();
    }

    private AnnotationSpec generatedAnnotationFor() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", getClass().getName())
                .addMember("date", "$S", LocalDate.now())
                .build();
    }

    private String matcherNameFrom(final MatcherBaseDescriptor descriptor) {
        return descriptor.getBase().getTypeName() + "Matcher";
    }

    private String packageFrom(final MatcherBaseDescriptor descriptor) {
        return descriptor.getBase().getPackageName();
    }
}
