/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
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
    private static final String REFERENCE = "reference";
    private static final String OBJECT_CLASS_ACCESSOR = "getClass";
    private static final String COLLECTION = "java.util.Collection";
    private static final String LIST = "java.util.List";
    private static final String MAP = "java.util.Map";
    private static final String BUILDER_FIELD_NAME = "beanPropertyMatcher";
    private static final AnnotationSpec NULL_MARKED =
            AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "NullMarked")).build();
    private static final AnnotationSpec NULLABLE =
            AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "Nullable")).build();

    private final TypeModel model;
    private final GenerationContext context;
    private final String simpleMatcherName;
    private final List<PropertyModel> presetProperties;
    private @Nullable Map<String, MatcherType> referenceTypes;

    MatcherGenerator(final TypeModel model, final GenerationContext context) {
        this.model = model;
        this.context = context;
        this.simpleMatcherName = MatcherNaming.matcherName(model);
        this.presetProperties =
                PropertyModel.distinctByName(model.properties()).stream()
                        .filter(property -> !OBJECT_CLASS_ACCESSOR.equals(property.accessor()))
                        .collect(Collectors.toList());
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
        final TypeSpec.Builder classBuilder =
                TypeSpec.classBuilder(simpleMatcherName)
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

        return classBuilder;
    }

    private List<MethodSpec> getPropertyHamcrestMatcherMethods() {
        return PropertyModel.distinctByName(model.properties()).stream()
                .map(this::toHamcrestMatcher)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isConflictingProperty(final String propertyName) {
        return model.properties().stream()
                        .filter(property -> property.name().equals(propertyName))
                        .count()
                > 1;
    }

    private List<MethodSpec> getPropertyHamcrestMatcherResetMethods() {
        return PropertyModel.distinctByName(model.properties()).stream()
                .map(this::toHamcrestMatcherReset)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<MethodSpec> getPropertyEqualsMatcherMethods() {
        return model.properties().stream().map(this::toEqualsMatcher).collect(Collectors.toList());
    }

    private @Nullable MethodSpec toHamcrestMatcher(final PropertyModel property) {
        if (isMatcher(property.type())) {
            return null;
        }
        return MethodSpec.methodBuilder(MatcherNaming.withMethodName(property.name()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(toParameterizedMatcherType(property), "matcher", Modifier.FINAL)
                .addStatement("$L.with($S, matcher)", BUILDER_FIELD_NAME, property.name())
                .addStatement(RETURN_THIS)
                .returns(getGeneratedTypeName())
                .build();
    }

    private @Nullable MethodSpec toHamcrestMatcherReset(final PropertyModel property) {
        if (isMatcher(property.type())) {
            return null;
        }
        return MethodSpec.methodBuilder(MatcherNaming.resetMethodName(property.name()))
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$L.reset($S)", BUILDER_FIELD_NAME, property.name())
                .addStatement(RETURN_THIS)
                .returns(getGeneratedTypeName())
                .build();
    }

    private TypeName toParameterizedMatcherType(final PropertyModel property) {
        return ParameterizedTypeName.get(
                ClassName.get(Matcher.class),
                isConflictingProperty(property.name())
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
                                MethodSpec.methodBuilder(
                                                MatcherNaming.ignoringMethodName(property.name()))
                                        .addModifiers(Modifier.PUBLIC)
                                        .addStatement(
                                                "$L.ignore($S)",
                                                BUILDER_FIELD_NAME,
                                                property.name())
                                        .addStatement(RETURN_THIS)
                                        .returns(getGeneratedTypeName())
                                        .build())
                .collect(Collectors.toList());
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
        return MethodSpec.methodBuilder(MatcherNaming.withMethodName(property.name()))
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
        if (type instanceof DeclaredType && !((DeclaredType) type).getTypeArguments().isEmpty()) {
            final TypeName[] typeArguments =
                    ((DeclaredType) type)
                            .getTypeArguments().stream()
                                    .map(
                                            argument ->
                                                    typeArgumentToWildcard(argument, boxPrimitives))
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
        if (argument instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) argument;
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
                .addParameter(typeNameWithWildCards(), REFERENCE, Modifier.FINAL)
                .addStatement(
                        "return $T.matcherFor($L, $T.class, $T::new, matcher -> matcher.presetFrom($L))",
                        ReferenceMatching.class,
                        REFERENCE,
                        getGeneratedTypeName(),
                        getGeneratedTypeName(),
                        presetProperties.isEmpty() ? "" : REFERENCE)
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
            method.addParameter(typeNameWithWildCards(), REFERENCE, Modifier.FINAL);
        }
        presetProperties.forEach(
                property ->
                        method.addStatement(
                                "$L.preset($S, $L)",
                                BUILDER_FIELD_NAME,
                                property.name(),
                                presetOf(property)));
        return method.build();
    }

    private CodeBlock presetOf(final PropertyModel property) {
        final CodeBlock value = CodeBlock.of("$L.$L()", REFERENCE, property.accessor());
        final TypeMirror type = property.type();
        final MatcherType direct = matcherTypeOf(type);
        if (direct != null) {
            return referencePreset("nested", value, direct);
        }
        if (type.getKind() == TypeKind.ARRAY) {
            final MatcherType item = matcherTypeOf(((ArrayType) type).getComponentType());
            return item == null ? valuePreset(value) : referencePreset("array", value, item);
        }
        final @Nullable List<TypeMirror> collectionItem = argumentsAs(type, COLLECTION);
        if (collectionItem != null && collectionItem.size() == 1) {
            final MatcherType item = matcherTypeOf(collectionItem.get(0));
            if (item != null) {
                return referencePreset(
                        argumentsAs(type, LIST) != null ? "list" : "collection", value, item);
            }
        }
        final @Nullable List<TypeMirror> mapTypes = argumentsAs(type, MAP);
        if (mapTypes != null && mapTypes.size() == 2) {
            final MatcherType item = matcherTypeOf(mapTypes.get(1));
            if (item != null) {
                return referencePreset("map", value, item);
            }
        }
        return valuePreset(value);
    }

    private CodeBlock valuePreset(final CodeBlock value) {
        return CodeBlock.of("$T.value($L)", ReferenceMatching.class, value);
    }

    private CodeBlock referencePreset(
            final String helper, final CodeBlock value, final MatcherType matcherType) {
        return CodeBlock.of(
                "$T.$L($L, $T::$L)",
                ReferenceMatching.class,
                helper,
                value,
                matcherType.className(),
                matcherType.likeMethod());
    }

    /**
     * @return the matcher a value of the type is compared with deeply, or {@code null} if it is
     *     compared by {@code equals}. That is the case for types without a matcher generated in
     *     this run, for enums and for types without properties, which tell nothing apart.
     */
    private @Nullable MatcherType matcherTypeOf(final TypeMirror type) {
        if (type instanceof WildcardType) {
            final TypeMirror bound = ((WildcardType) type).getExtendsBound();
            return bound == null ? null : matcherTypeOf(bound);
        }
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        return referenceTypes().get(asTypeElement(type).getQualifiedName().toString());
    }

    private Map<String, MatcherType> referenceTypes() {
        if (referenceTypes == null) {
            referenceTypes = collectReferenceTypes();
        }
        return referenceTypes;
    }

    private Map<String, MatcherType> collectReferenceTypes() {
        final Map<String, MatcherType> types = new HashMap<>();
        context.generatedTypes()
                .forEach(
                        generated ->
                                collectInto(
                                        generated,
                                        ClassName.get(
                                                generated.targetPackage(),
                                                MatcherNaming.matcherName(generated)),
                                        types));
        return types;
    }

    private void collectInto(
            final TypeModel generated,
            final ClassName className,
            final Map<String, MatcherType> types) {
        if (isComparedDeeply(generated)) {
            types.put(
                    generated.qualifiedName(),
                    new MatcherType(className, MatcherNaming.referenceInitializerName(generated)));
        }
        generated
                .nestedTypes()
                .forEach(
                        nested ->
                                collectInto(
                                        nested,
                                        className.nestedClass(MatcherNaming.matcherName(nested)),
                                        types));
    }

    private boolean isComparedDeeply(final TypeModel generated) {
        return asTypeElement(generated.type()).getKind() != ElementKind.ENUM
                && generated.properties().stream()
                        .anyMatch(property -> !OBJECT_CLASS_ACCESSOR.equals(property.accessor()));
    }

    /**
     * The type arguments the type gives to one of its supertypes, as in {@code Lines extends
     * ArrayList<Line>} giving {@code Line} to {@code Collection}.
     *
     * @return the arguments, or {@code null} if the type has no such supertype
     */
    private @Nullable List<TypeMirror> argumentsAs(
            final TypeMirror type, final String qualifiedName) {
        return type.getKind() == TypeKind.DECLARED
                ? argumentsAs(
                        asTypeElement(type),
                        ((DeclaredType) type).getTypeArguments(),
                        qualifiedName)
                : null;
    }

    private @Nullable List<TypeMirror> argumentsAs(
            final TypeElement type,
            final List<? extends TypeMirror> arguments,
            final String qualifiedName) {
        if (type.getQualifiedName().contentEquals(qualifiedName)) {
            return new ArrayList<>(arguments);
        }
        final Map<String, TypeMirror> bindings = new HashMap<>();
        for (var i = 0; i < Math.min(arguments.size(), type.getTypeParameters().size()); i++) {
            bindings.put(
                    type.getTypeParameters().get(i).getSimpleName().toString(), arguments.get(i));
        }
        return Stream.concat(Stream.of(type.getSuperclass()), type.getInterfaces().stream())
                .filter(supertype -> supertype.getKind() == TypeKind.DECLARED)
                .map(
                        supertype ->
                                argumentsAs(
                                        asTypeElement(supertype),
                                        boundArguments((DeclaredType) supertype, bindings),
                                        qualifiedName))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** The arguments of a supertype with the type variables of the subtype replaced. */
    private static List<TypeMirror> boundArguments(
            final DeclaredType supertype, final Map<String, TypeMirror> bindings) {
        return supertype.getTypeArguments().stream()
                .map(
                        argument ->
                                argument.getKind() == TypeKind.TYPEVAR
                                        ? bindings.getOrDefault(
                                                ((TypeVariable) argument)
                                                        .asElement()
                                                        .getSimpleName()
                                                        .toString(),
                                                argument)
                                        : argument)
                .collect(Collectors.toList());
    }

    /** The matcher generated for a type and the name of its method for reference objects. */
    private record MatcherType(ClassName className, String likeMethod) {}

    private ClassName getGeneratedTypeName() {
        return ClassName.get("", simpleMatcherName);
    }

    private MethodSpec getConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement(
                        "$L = new $T($T.class$L)",
                        BUILDER_FIELD_NAME,
                        getBuilderFieldType(),
                        rawTypeName(),
                        propertyNameArguments())
                .build();
    }

    private String propertyNameArguments() {
        return PropertyModel.distinctByName(model.properties()).stream()
                .map(property -> ", \"" + property.name() + "\"")
                .collect(Collectors.joining());
    }

    private List<MethodSpec> getMatcherMethods() {
        return List.of(
                getBeanPropertyMatcherMethod(),
                getDescribeToMethod(),
                getMatchesSafelyMethod(),
                getDescribeMissmatchSafelyMethod());
    }

    private MethodSpec getBeanPropertyMatcherMethod() {
        return MethodSpec.methodBuilder("beanPropertyMatcher")
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
                .addParameter(Description.class, "description", Modifier.FINAL)
                .addStatement("$L.describeTo($L)", BUILDER_FIELD_NAME, "description")
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
                .addParameter(Description.class, "description", Modifier.FINAL)
                .addStatement(
                        "$L.describeMismatch($L, $L)", BUILDER_FIELD_NAME, "item", "description")
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
                .collect(Collectors.toList());
    }

    private AnnotationSpec getGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", context.generatorName())
                .addMember("date", "$S", context.generationTime())
                .build();
    }

    private TypeElement asTypeElement(final TypeMirror type) {
        return (TypeElement) ((DeclaredType) type).asElement();
    }

    private boolean isMatcher(final TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED
                && Matcher.class
                        .getCanonicalName()
                        .equals(asTypeElement(type).getQualifiedName().toString());
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
