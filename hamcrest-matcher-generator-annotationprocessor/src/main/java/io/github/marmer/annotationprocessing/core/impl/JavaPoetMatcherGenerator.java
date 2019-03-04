package io.github.marmer.annotationprocessing.core.impl;

import com.squareup.javapoet.*;
import io.github.marmer.annotationprocessing.core.MatcherGenerator;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.MatcherSourceDescriptor;
import io.github.marmer.annotationprocessing.core.model.PropertyDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;
import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                        .typeName(matcherNameFrom(descriptor.getBase()))
                        .fullQualifiedName(fullQualifiedMatcherNameFrom(descriptor)).build())
                .source(javaFile.toString()).build();
    }

    private String fullQualifiedMatcherNameFrom(final MatcherBaseDescriptor descriptor) {
        return packageFrom(descriptor) +
                "." +
                matcherNameFrom(descriptor.getBase());
    }

    private JavaFile matcherFileFor(final MatcherBaseDescriptor descriptor) {
        return JavaFile.builder(packageFrom(descriptor), matcherTypeFor(descriptor).build()).skipJavaLangImports(true).indent("    ").build();
    }

    private TypeSpec.Builder matcherTypeFor(final MatcherBaseDescriptor descriptor) {
        final ClassName className = classNameOfGeneratedTypeFor(descriptor);
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(parameterizedTypesafeMatchertype(descriptor))
                .addField(innerMatcherField(descriptor))
                .addMethod(constructor(descriptor))
                .addMethods(propertyMethods(descriptor))
                .addMethods(typesafeMatcherMethods(descriptor))
                .addMethod(factoryMethod(descriptor))
                .addAnnotation(generatedAnnotationFor())
                .addTypes(innerMatchersFor(descriptor));
    }

    private List<TypeSpec> innerMatchersFor(final MatcherBaseDescriptor descriptor) {
        return descriptor.getInnerMatchers()
                .stream()
                .map(this::matcherTypeFor)
                .map(type -> type.addModifiers(Modifier.STATIC))
                .map(TypeSpec.Builder::build)
                .collect(Collectors.toList());
    }

    private List<MethodSpec> propertyMethods(final MatcherBaseDescriptor descriptor) {
        final List<PropertyDescriptor> properties = descriptor.getProperties();
        return properties.stream()
                .flatMap(property ->
                        Stream.of(propertyMatcherMethodFor(property, descriptor),
                                propertyMethodFor(property, descriptor)))
                .collect(Collectors.toList());
    }

    private MethodSpec propertyMatcherMethodFor(final PropertyDescriptor property, final MatcherBaseDescriptor descriptor) {
        return MethodSpec.methodBuilder(methodNameToGenerateFor(property))
                .returns(classNameOfGeneratedTypeFor(descriptor))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterizedMatcherType(),
                        "matcher",
                        Modifier.FINAL)
                .addStatement("$L.with($S, matcher)",
                        INNER_MATCHER_FIELD_NAME,
                        property.getProperty())
                .addStatement(
                        "return this")
                .build();
    }

    private MethodSpec propertyMethodFor(final PropertyDescriptor property, final MatcherBaseDescriptor descriptor) {
        return MethodSpec.methodBuilder(methodNameToGenerateFor(property))
                .returns(classNameOfGeneratedTypeFor(descriptor))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(getClassNameFor(property.getReturnValue()), "value", Modifier.FINAL)
                .addStatement("$L.with($S, $T.equalTo(value))", INNER_MATCHER_FIELD_NAME, property.getProperty(), Matchers.class)
                .addStatement("return this")
                .build();
    }

    private ParameterizedTypeName parameterizedMatcherType() {
        return ParameterizedTypeName.get(ClassName.get(Matcher.class),
                WildcardTypeName.subtypeOf(TypeName.OBJECT));
    }

    private String methodNameToGenerateFor(final PropertyDescriptor propertyName) {
        return "with" + StringUtils.capitalize(propertyName.getProperty());
    }

    private ParameterizedTypeName parameterizedTypesafeMatchertype(final MatcherBaseDescriptor descriptor) {
        return ParameterizedTypeName.get(ClassName.get(TypeSafeMatcher.class), getClassNameFor(descriptor.getBase()));
    }

    private Iterable<MethodSpec> typesafeMatcherMethods(final MatcherBaseDescriptor descriptor) {
        return Arrays.asList(describeToMethod(), matchesSafelyMathod(descriptor), describeMismatchSafelyMethod(descriptor));
    }

    private MethodSpec describeToMethod() {
        return MethodSpec.methodBuilder("describeTo")
                .addAnnotation(Override.class)
                .addParameter(Description.class,
                        PARAMETER_NAME_DESCRIPTION,
                        Modifier.FINAL)
                .addStatement("$L.describeTo($L)",
                        INNER_MATCHER_FIELD_NAME,
                        PARAMETER_NAME_DESCRIPTION)
                .addModifiers(Modifier.PUBLIC).build();
    }

    private MethodSpec matchesSafelyMathod(final MatcherBaseDescriptor type) {
        return MethodSpec.methodBuilder("matchesSafely")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(Boolean.TYPE)
                .addParameter(getClassNameFor(type.getBase()),
                        PARAMETER_NAME_ITEM,
                        Modifier.FINAL)
                .addStatement("return $L.matches($L)",
                        INNER_MATCHER_FIELD_NAME,
                        PARAMETER_NAME_ITEM)
                .build();
    }

    private ClassName getClassNameFor(final TypeDescriptor base) {
        final List<String> parentNames = base.getParentNames();
        if (parentNames.isEmpty()) {
            return ClassName.get(base.isPrimitive() ? "" : base.getPackageName(), base.getTypeName());
        } else {
            return ClassName.get(base.getPackageName(),
                    parentNames.get(0),
                    concat(parentNames.stream().skip(1).toArray(String[]::new), base.getTypeName()));
        }
    }

    private String[] concat(final String[] outerTypes, final String... type) {
        return Stream.concat(Stream.of(outerTypes), Stream.of(type)).toArray(String[]::new);
    }

    private MethodSpec describeMismatchSafelyMethod(final MatcherBaseDescriptor type) {
        return MethodSpec.methodBuilder("describeMismatchSafely")
                .addAnnotation(Override.class)
                .addParameter(getClassNameFor(type.getBase()),
                        PARAMETER_NAME_ITEM,
                        Modifier.FINAL)
                .addStatement("$L.describeMismatch($L, $L)",
                        INNER_MATCHER_FIELD_NAME,
                        PARAMETER_NAME_ITEM,
                        PARAMETER_NAME_DESCRIPTION)
                .addParameter(Description.class,
                        PARAMETER_NAME_DESCRIPTION, Modifier.FINAL)
                .addModifiers(Modifier.PROTECTED).build();
    }

    private MethodSpec factoryMethod(final MatcherBaseDescriptor descriptor) {
        return MethodSpec.methodBuilder("is" + descriptor.getBase().getTypeName())
                .addStatement("return new $L()",
                        matcherNameFrom(descriptor.getBase()))
                .returns(classNameOfGeneratedTypeFor(descriptor))
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC).build();
    }

    private ClassName classNameOfGeneratedTypeFor(final MatcherBaseDescriptor descriptor) {
        final TypeDescriptor base = descriptor.getBase();
        final List<String> parentNames = base.getParentNames();
        return parentNames.isEmpty() ?
                ClassName.get(base.getPackageName(),
                        matcherNameFrom(descriptor.getBase())) :
                ClassName.get(base.getPackageName(),
                        matcherNameFrom(parentNames.get(0)),
                        Stream.concat(parentNames.stream().skip(1), Stream.of(base.getTypeName()))
                                .map(this::matcherNameFrom)
                                .toArray(String[]::new));
    }

    private FieldSpec innerMatcherField(final MatcherBaseDescriptor descriptor) {
        final ParameterizedTypeName fieldType = ParameterizedTypeName.get(ClassName.get(BeanPropertyMatcher.class), getClassNameFor(descriptor.getBase()));
        return FieldSpec.builder(fieldType, INNER_MATCHER_FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private MethodSpec constructor(final MatcherBaseDescriptor descriptor) {
        final TypeDescriptor base = descriptor.getBase();
        return MethodSpec.constructorBuilder()
                .addStatement("$L = new BeanPropertyMatcher<$T>($T.class)",
                        INNER_MATCHER_FIELD_NAME,
                        getClassNameFor(base),
                        getClassNameFor(base))
                .addModifiers(Modifier.PUBLIC)
                .build();
    }

    private AnnotationSpec generatedAnnotationFor() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", getClass().getName())
                .addMember("date", "$S", LocalDate.now())
                .build();
    }

    private String matcherNameFrom(final TypeDescriptor base) {
        return matcherNameFrom(base.getTypeName());
    }

    private String matcherNameFrom(final String simpleTypeName) {
        return simpleTypeName + "Matcher";
    }

    private String packageFrom(final MatcherBaseDescriptor descriptor) {
        return descriptor.getBase().getPackageName();
    }
}
