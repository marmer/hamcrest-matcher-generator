package io.github.marmer.annotationprocessing.core.impl;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;
import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.time.LocalDate;
import java.util.Arrays;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;

public class JavaPoetMatcherGenerator implements MatcherGenerator {
    private static final String INNER_MATCHER_FIELD_NAME = "beanPropertyMatcher";
    private static final String PARAMETER_NAME_DESCRIPTION = "description";
    private static final String PARAMETER_NAME_ITEM = "item";

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
                .superclass(parameterizedTypesafeMatchertype(descriptor))
                .addField(innerMatcherField(descriptor))
                .addMethod(constructor(descriptor))
                .addMethods(typesafeMatcherMethods(descriptor))
                .addMethod(factoryMethod(descriptor))
                .addAnnotation(generatedAnnotationFor())
                .build();

        return JavaFile.builder(packageFrom(descriptor), typeSpec).skipJavaLangImports(true).indent("    ").build();
    }

    private ParameterizedTypeName parameterizedTypesafeMatchertype(final MatcherBaseDescriptor descriptor) {
        return ParameterizedTypeName.get(ClassName.get(TypeSafeMatcher.class), getClassNameFor(descriptor.getBase()));
    }

    private Iterable<MethodSpec> typesafeMatcherMethods(final MatcherBaseDescriptor descriptor) {
        return Arrays.asList(describeToMethod(), matchesSafelyMathod(descriptor), describeMismatchSafelyMethod(descriptor));
    }

    private MethodSpec describeToMethod() {
        final String parameterName = PARAMETER_NAME_DESCRIPTION;
        return MethodSpec.methodBuilder("describeTo")
                .addAnnotation(Override.class)
                .addParameter(Description.class, parameterName, Modifier.FINAL)
                .addStatement("$L.describeTo($L)", INNER_MATCHER_FIELD_NAME, parameterName)
                .addModifiers(Modifier.PUBLIC).build();
    }

    private MethodSpec matchesSafelyMathod(final MatcherBaseDescriptor type) {
        return MethodSpec.methodBuilder("matchesSafely")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(Boolean.TYPE)
                .addParameter(getClassNameFor(type.getBase()), PARAMETER_NAME_ITEM, Modifier.FINAL)
                .addStatement("return $L.matches($L)", INNER_MATCHER_FIELD_NAME, PARAMETER_NAME_ITEM).build();
    }

    private ClassName getClassNameFor(final TypeDescriptor base) {
        return ClassName.get(base.getPackageName(), base.getTypeName());
    }

    private MethodSpec describeMismatchSafelyMethod(final MatcherBaseDescriptor type) {
        return MethodSpec.methodBuilder("describeMismatchSafely")
                .addAnnotation(Override.class)
                .addParameter(getClassNameFor(type.getBase()), PARAMETER_NAME_ITEM, Modifier.FINAL)
                .addStatement("$L.describeMismatch($L, $L)", INNER_MATCHER_FIELD_NAME, PARAMETER_NAME_ITEM, PARAMETER_NAME_DESCRIPTION)
                .addParameter(Description.class,
                        PARAMETER_NAME_DESCRIPTION, Modifier.FINAL)
                .addModifiers(Modifier.PROTECTED).build();
    }

    private MethodSpec factoryMethod(final MatcherBaseDescriptor descriptor) {
        return MethodSpec.methodBuilder("is" + descriptor.getBase().getTypeName())
                .addStatement("return new $L()",
                        matcherNameFrom(descriptor))
                .returns(classNameOfGeneratedTypeFor(descriptor))
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC).build();
    }

    private ClassName classNameOfGeneratedTypeFor(final MatcherBaseDescriptor descriptor) {
        return ClassName.get(descriptor.getBase().getPackageName(), matcherNameFrom(descriptor));
    }

    private FieldSpec innerMatcherField(final MatcherBaseDescriptor descriptor) {
        final ParameterizedTypeName fieldType = ParameterizedTypeName.get(ClassName.get(BeanPropertyMatcher.class), getClassNameFor(descriptor.getBase()));
        return FieldSpec.builder(fieldType, INNER_MATCHER_FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private MethodSpec constructor(final MatcherBaseDescriptor descriptor) {
        final TypeDescriptor base = descriptor.getBase();
        return MethodSpec.constructorBuilder()
                .addStatement("$L = new BeanPropertyMatcher<$T>($T.class)", INNER_MATCHER_FIELD_NAME, getClassNameFor(base), getClassNameFor(base))
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
