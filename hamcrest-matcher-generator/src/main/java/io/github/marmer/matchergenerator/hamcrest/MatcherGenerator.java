/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static io.github.marmer.matchergenerator.core.model.TypeHierarchy.asTypeElement;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jspecify.annotations.Nullable;

class MatcherGenerator {
    private static final String RETURN_THIS = "return this";

    private static final String BUILDER_FIELD_NAME = "beanPropertyMatcher";
    private static final String BEAN_PROPERTY_MATCHER_METHOD = "beanPropertyMatcher";
    private static final String DESCRIPTION = "description";
    private static final AnnotationSpec NULL_MARKED =
            AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "NullMarked")).build();
    private static final AnnotationSpec NULLABLE =
            AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "Nullable")).build();

    private final TypeModel model;
    private final GenerationContext context;
    private final String simpleMatcherName;
    private final List<PropertyModel> presetProperties;
    private final ReferencePresetPlanner referencePresets;

    MatcherGenerator(final TypeModel model, final GenerationContext context) {
        this.model = model;
        this.context = context;
        this.simpleMatcherName = MatcherNaming.matcherName(model);
        this.presetProperties = model.accessorProperties();
        this.referencePresets =
                new ReferencePresetPlanner(
                        model, context.generatedTypes(), context::reportVerboseNote);
    }

    String simpleMatcherName() {
        return simpleMatcherName;
    }

    /**
     * The matcher type of the configured type itself. Unlike the nested ones it is the null marked
     * scope, unless JSpecify is not on the class path.
     */
    TypeSpec getTopLevelTypeSpec() {
        final TypeSpec.Builder builder = getPreparedTypeSpecBuilder();
        if (context.jspecifyAvailable()) {
            builder.addAnnotation(NULL_MARKED);
        }
        return builder.build();
    }

    TypeSpec.Builder getPreparedTypeSpecBuilder() {
        return TypeSpec.classBuilder(simpleMatcherName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(getGeneratedAnnotation())
                .superclass(getSuperClass())
                .addSuperinterface(GeneratedBeanMatcher.class)
                .addFields(getFields())
                .addMethod(getConstructor())
                .addMethods(getPropertyHamcrestMatcherMethods())
                .addMethods(getPropertyHamcrestMatcherResetMethods())
                .addMethods(getPropertyEqualsMatcherMethods())
                .addMethods(getPropertyIgnoreMethods())
                .addMethod(getCheckingAllPropertiesMethod())
                .addMethod(getReferenceInitializer())
                .addMethod(getPresetMethod())
                .addMethods(getMatcherMethods())
                .addMethod(getApiInitializer())
                .addTypes(getInnerMatchers());
    }

    private List<MethodSpec> getPropertyHamcrestMatcherMethods() {
        return PropertyModel.distinctByName(model.properties()).stream()
                .map(this::toHamcrestMatcher)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<MethodSpec> getPropertyHamcrestMatcherResetMethods() {
        return PropertyModel.distinctByName(model.properties()).stream()
                .map(this::toHamcrestMatcherReset)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<MethodSpec> getPropertyEqualsMatcherMethods() {
        return model.properties().stream().map(this::toEqualsMatcher).toList();
    }

    private @Nullable MethodSpec toHamcrestMatcher(final PropertyModel property) {
        if (MatcherNaming.isMatcherType(property.type())) {
            return null;
        }
        return MethodSpec.methodBuilder(MatcherNaming.withMethodName(property))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(toParameterizedMatcherType(property), "matcher", Modifier.FINAL)
                .addStatement("$L.with($S, matcher)", BUILDER_FIELD_NAME, property.name())
                .addStatement(RETURN_THIS)
                .returns(getGeneratedTypeName())
                .build();
    }

    private @Nullable MethodSpec toHamcrestMatcherReset(final PropertyModel property) {
        if (MatcherNaming.isMatcherType(property.type())) {
            return null;
        }
        return MethodSpec.methodBuilder(MatcherNaming.resetMethodName(property))
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$L.reset($S)", BUILDER_FIELD_NAME, property.name())
                .addStatement(RETURN_THIS)
                .returns(getGeneratedTypeName())
                .build();
    }

    private TypeName toParameterizedMatcherType(final PropertyModel property) {
        return ParameterizedTypeName.get(
                ClassName.get(Matcher.class),
                model.isNameShared(property)
                        ? WildcardTypeName.subtypeOf(TypeName.OBJECT)
                        : WildcardTypeName.supertypeOf(
                                nullableIf(
                                        property.nullability() == Nullability.NULLABLE,
                                        typeVarsToWildcards(property.type(), true))));
    }

    private TypeName nullableIf(final boolean nullable, final TypeName type) {
        return nullable ? nullable(type) : type;
    }

    /** Marks the type as nullable when JSpecify is available. Primitives cannot be annotated. */
    private TypeName nullable(final TypeName type) {
        return context.jspecifyAvailable() && !type.isPrimitive() ? type.annotated(NULLABLE) : type;
    }

    private List<MethodSpec> getPropertyIgnoreMethods() {
        return PropertyModel.distinctByName(model.properties()).stream()
                .map(
                        property ->
                                MethodSpec.methodBuilder(MatcherNaming.ignoringMethodName(property))
                                        .addModifiers(Modifier.PUBLIC)
                                        .addStatement(
                                                "$L.ignore($S)",
                                                BUILDER_FIELD_NAME,
                                                property.name())
                                        .addStatement(RETURN_THIS)
                                        .returns(getGeneratedTypeName())
                                        .build())
                .toList();
    }

    private MethodSpec getCheckingAllPropertiesMethod() {
        return MethodSpec.methodBuilder(MatcherNaming.CHECKING_ALL_PROPERTIES)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$L.checkingAllProperties()", BUILDER_FIELD_NAME)
                .addStatement(RETURN_THIS)
                .returns(getGeneratedTypeName())
                .build();
    }

    private MethodSpec toEqualsMatcher(final PropertyModel property) {
        return MethodSpec.methodBuilder(MatcherNaming.withMethodName(property))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(toEqualsMatcherParameterType(property), "value", Modifier.FINAL)
                .addStatement(
                        "$L.with($S, $T.equalTo(value))",
                        BUILDER_FIELD_NAME,
                        property.name(),
                        Matchers.class)
                .addStatement(RETURN_THIS)
                .returns(getGeneratedTypeName())
                .build();
    }

    private TypeName toEqualsMatcherParameterType(final PropertyModel property) {
        if (property.type().getKind() == TypeKind.TYPEVAR) {
            return nullable(TypeName.get(Object.class));
        }
        return nullable(typeVarsToWildcards(property.type(), false));
    }

    private TypeName typeVarsToWildcards(final TypeMirror type, final boolean boxPrimitives) {
        if (type instanceof DeclaredType declared && !declared.getTypeArguments().isEmpty()) {
            final TypeName[] typeArguments =
                    declared.getTypeArguments().stream()
                            .map(argument -> typeArgumentToWildcard(argument, boxPrimitives))
                            .toArray(TypeName[]::new);
            return ParameterizedTypeName.get(ClassName.get(asTypeElement(type)), typeArguments);
        }
        if (type instanceof TypeVariable) {
            return TypeName.get(Object.class);
        }
        if (type instanceof PrimitiveType) {
            return boxPrimitives ? TypeName.get(type).box() : TypeName.get(type);
        }
        return TypeName.get(type);
    }

    private TypeName typeArgumentToWildcard(
            final TypeMirror argument, final boolean boxPrimitives) {
        if (argument instanceof TypeVariable) {
            return WildcardTypeName.subtypeOf(Object.class);
        }
        if (argument instanceof WildcardType wildcard) {
            if (wildcard.getExtendsBound() != null) {
                return WildcardTypeName.subtypeOf(
                        typeVarsToWildcards(wildcard.getExtendsBound(), boxPrimitives));
            }
            if (wildcard.getSuperBound() != null) {
                return WildcardTypeName.supertypeOf(
                        typeVarsToWildcards(wildcard.getSuperBound(), boxPrimitives));
            }
            return WildcardTypeName.subtypeOf(Object.class);
        }
        return WildcardTypeName.subtypeOf(typeVarsToWildcards(argument, boxPrimitives));
    }

    private MethodSpec getApiInitializer() {
        return MethodSpec.methodBuilder(MatcherNaming.initializerName(model))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return new $T()", getGeneratedTypeName())
                .returns(getGeneratedTypeName())
                .build();
    }

    private MethodSpec getReferenceInitializer() {
        return MethodSpec.methodBuilder(MatcherNaming.referenceInitializerName(model))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(
                        typeNameWithWildCards(), ReferencePresetPlanner.REFERENCE, Modifier.FINAL)
                .addStatement(
                        "return $T.matcherFor($L, $T.class, $T::new, matcher -> matcher.presetFrom($L))",
                        ReferenceMatching.class,
                        ReferencePresetPlanner.REFERENCE,
                        getGeneratedTypeName(),
                        getGeneratedTypeName(),
                        presetProperties.isEmpty() ? "" : ReferencePresetPlanner.REFERENCE)
                .returns(getGeneratedTypeName())
                .build();
    }

    /** Turns on the completeness check and presets every property from the reference object. */
    private MethodSpec getPresetMethod() {
        final MethodSpec.Builder method =
                MethodSpec.methodBuilder("presetFrom")
                        .addModifiers(Modifier.PRIVATE)
                        .addStatement("$L.checkingAllProperties()", BUILDER_FIELD_NAME);
        if (!presetProperties.isEmpty()) {
            method.addParameter(
                    typeNameWithWildCards(), ReferencePresetPlanner.REFERENCE, Modifier.FINAL);
        }
        presetProperties.forEach(
                property ->
                        method.addStatement(
                                "$L.preset($S, $L)",
                                BUILDER_FIELD_NAME,
                                property.name(),
                                referencePresets.presetOf(property)));
        return method.build();
    }

    private ClassName getGeneratedTypeName() {
        return ClassName.get("", simpleMatcherName);
    }

    private MethodSpec getConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement(
                        "$L = new $T($L)",
                        BUILDER_FIELD_NAME,
                        getBuilderFieldType(),
                        constructorArguments())
                .build();
    }

    /** The type, then every property with the method reference that reads it, in order. */
    private CodeBlock constructorArguments() {
        final CodeBlock.Builder arguments = CodeBlock.builder().add("$T.class", rawTypeName());
        PropertyModel.distinctByName(model.properties())
                .forEach(
                        property ->
                                arguments.add(
                                        ", $T.property($S, $T::$L)",
                                        BeanPropertyMatcher.class,
                                        property.name(),
                                        rawTypeName(),
                                        property.accessor()));
        return arguments.build();
    }

    private List<MethodSpec> getMatcherMethods() {
        return List.of(
                getBeanPropertyMatcherMethod(),
                getDescribeToMethod(),
                getMatchesSafelyMethod(),
                getDescribeMissmatchSafelyMethod());
    }

    private MethodSpec getBeanPropertyMatcherMethod() {
        return MethodSpec.methodBuilder(BEAN_PROPERTY_MATCHER_METHOD)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addStatement("return $L", BUILDER_FIELD_NAME)
                .returns(getBuilderFieldType())
                .build();
    }

    private MethodSpec getDescribeToMethod() {
        return MethodSpec.methodBuilder("describeTo")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(Description.class, DESCRIPTION, Modifier.FINAL)
                .addStatement("$L.describeTo($L)", BUILDER_FIELD_NAME, DESCRIPTION)
                .build();
    }

    private MethodSpec getMatchesSafelyMethod() {
        return MethodSpec.methodBuilder("matchesSafely")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(typeNameWithWildCards(), "item", Modifier.FINAL)
                .addStatement("return $L.matches($L)", BUILDER_FIELD_NAME, "item")
                .returns(TypeName.BOOLEAN)
                .build();
    }

    private MethodSpec getDescribeMissmatchSafelyMethod() {
        return MethodSpec.methodBuilder("describeMismatchSafely")
                .addAnnotation(Override.class)
                .addParameter(typeNameWithWildCards(), "item", Modifier.FINAL)
                .addParameter(Description.class, DESCRIPTION, Modifier.FINAL)
                .addStatement(
                        "$L.describeMismatch($L, $L)", BUILDER_FIELD_NAME, "item", DESCRIPTION)
                .addModifiers(Modifier.PROTECTED)
                .build();
    }

    private List<FieldSpec> getFields() {
        return List.of(
                FieldSpec.builder(
                                getBuilderFieldType(),
                                BUILDER_FIELD_NAME,
                                Modifier.PRIVATE,
                                Modifier.FINAL)
                        .build());
    }

    private TypeName getBuilderFieldType() {
        return ParameterizedTypeName.get(
                ClassName.get(BeanPropertyMatcher.class), typeNameWithWildCards());
    }

    private TypeName getSuperClass() {
        return ParameterizedTypeName.get(
                ClassName.get(TypeSafeMatcher.class), typeNameWithWildCards());
    }

    private List<TypeSpec> getInnerMatchers() {
        return model.nestedTypes().stream()
                .map(
                        innerType ->
                                new MatcherGenerator(innerType, context)
                                        .getPreparedTypeSpecBuilder()
                                        .addModifiers(Modifier.STATIC)
                                        .build())
                .toList();
    }

    private AnnotationSpec getGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", context.generatorName())
                .addMember("date", "$S", context.generationTime())
                .build();
    }

    private TypeName rawTypeName() {
        return model.typeParameters().isEmpty()
                ? TypeName.get(model.type())
                : ClassName.get(asTypeElement(model.type()));
    }

    private TypeName typeNameWithWildCards() {
        if (model.typeParameters().isEmpty()) {
            return TypeName.get(model.type());
        }
        return ParameterizedTypeName.get(
                ClassName.get(asTypeElement(model.type())),
                model.typeParameters().stream()
                        .map(parameter -> WildcardTypeName.subtypeOf(Object.class))
                        .toArray(TypeName[]::new));
    }
}
