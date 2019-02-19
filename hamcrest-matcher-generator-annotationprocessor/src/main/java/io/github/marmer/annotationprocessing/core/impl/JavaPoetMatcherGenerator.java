package io.github.marmer.annotationprocessing.core.impl;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return JavaFile.builder(packageFrom(descriptor), matcherTypeFor(descriptor).build()).skipJavaLangImports(true).indent("    ").build();
    }

    private TypeSpec.Builder matcherTypeFor(final MatcherBaseDescriptor descriptor, final TypeDescriptor... outerTypes) {
        final ClassName className = ClassName.get(packageFrom(descriptor), matcherNameFrom(descriptor));
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(parameterizedTypesafeMatchertype(descriptor))
                .addField(innerMatcherField(descriptor))
                .addMethod(constructor(descriptor))
                .addMethods(propertyMethods(descriptor, outerTypes))
                .addMethods(typesafeMatcherMethods(descriptor))
                .addMethod(factoryMethod(descriptor, outerTypes))
                .addAnnotation(generatedAnnotationFor())
                .addTypes(innerMatchersFor(descriptor));
    }

    private List<TypeSpec> innerMatchersFor(final MatcherBaseDescriptor descriptor, final TypeDescriptor... outerTypes) {
        return descriptor.getInnerMatchers()
                .stream()
                .map(innerMatcher -> matcherTypeFor(innerMatcher, asArray(outerTypes, descriptor.getBase())))
                .peek(type -> type.addModifiers(Modifier.STATIC))
                .map(TypeSpec.Builder::build)
                .collect(Collectors.toList());
    }

    private List<MethodSpec> propertyMethods(final MatcherBaseDescriptor descriptor, final TypeDescriptor... outerTypes) {
        final List<PropertyDescriptor> properties = descriptor.getProperties();
        return properties.stream()
                .flatMap(property -> Stream.of(propertyMatcherMethodFor(property, descriptor, outerTypes),
                        propertyMethodFor(property, descriptor, outerTypes))).collect(Collectors.toList());
    }

    private MethodSpec propertyMatcherMethodFor(final PropertyDescriptor property, final MatcherBaseDescriptor descriptor, final TypeDescriptor... outerTypes) {
        return MethodSpec.methodBuilder(methodNameToGenerateFor(property))
                .returns(classNameOfGeneratedTypeFor(descriptor, outerTypes))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterizedMatchertype(), "matcher", Modifier.FINAL)
                .addStatement("$L.with($S, matcher)", INNER_MATCHER_FIELD_NAME, property.getProperty())
                .addStatement(
                        "return this")
                .build();
    }

    private MethodSpec propertyMethodFor(final PropertyDescriptor property, final MatcherBaseDescriptor descriptor, final TypeDescriptor... outerTypes) {
        return MethodSpec.methodBuilder(methodNameToGenerateFor(property))
                .returns(classNameOfGeneratedTypeFor(descriptor, outerTypes))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(getClassNameFor(property.getReturnValue()), "value", Modifier.FINAL)
                .addStatement("$L.with($S, $T.equalTo(value))", INNER_MATCHER_FIELD_NAME, property.getProperty(), Matchers.class)
                .addStatement("return this")
                .build();
    }

    private ParameterizedTypeName parameterizedMatchertype() {
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
        final List<String> parentNames = base.getParentNames();
        if (base.isPrimitive()) {
            return ClassName.get("", base.getTypeName());
        } else {
            if (parentNames.isEmpty()) {
                return ClassName.get(base.getPackageName(), base.getTypeName());
            } else {
                return ClassName.get(base.getPackageName(),
                                    parentNames.get(0),
                                    asArray(parentNames.subList(1, parentNames.size()).toArray(new String[parentNames.size() - 1]), base.getTypeName()));
            }
        }
    }

    private String[] asArray(final String[] outerTypes, final String type) {
        return Stream.concat(Stream.of(outerTypes), Stream.of(type)).toArray(String[]::new);
    }

    private TypeDescriptor[] asArray(final TypeDescriptor[] outerTypes, final TypeDescriptor type) {
        return Stream.concat(Stream.of(outerTypes), Stream.of(type)).toArray(TypeDescriptor[]::new);
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

    private MethodSpec factoryMethod(final MatcherBaseDescriptor descriptor, final TypeDescriptor... outerTypes) {
        return MethodSpec.methodBuilder("is" + descriptor.getBase().getTypeName())
                .addStatement("return new $L()",
                        matcherNameFrom(descriptor))
                .returns(classNameOfGeneratedTypeFor(descriptor, outerTypes))
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC).build();
    }

    private ClassName classNameOfGeneratedTypeFor(final MatcherBaseDescriptor descriptor, final TypeDescriptor... outerTypes) {
        return outerTypes.length == 0 ?
                ClassName.get(descriptor.getBase().getPackageName(),
                        matcherNameFrom(descriptor)) :
                ClassName.get(descriptor.getBase().getPackageName(),
                        matcherNameFrom(outerTypes[0]),
                        asArray(Stream.of(Arrays.copyOfRange(outerTypes, 1, outerTypes.length))
                                        .map(this::matcherNameFrom)
                                        .toArray(String[]::new),
                                matcherNameFrom(descriptor)));
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
        return matcherNameFrom(descriptor.getBase());
    }

    private String matcherNameFrom(final TypeDescriptor base) {
        return base.getTypeName() + "Matcher";
    }

    private String packageFrom(final MatcherBaseDescriptor descriptor) {
        return descriptor.getBase().getPackageName();
    }
}
