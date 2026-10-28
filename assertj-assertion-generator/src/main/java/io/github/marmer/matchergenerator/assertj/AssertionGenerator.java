/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * Builds the two classes generated for a configured type: {@code AbstractXAssert<SELF, ACTUAL>},
 * which extends {@code AbstractObjectAssert} and carries the generated assertions, and the
 * concrete, non-final {@code XAssert}. Nested types get nested classes in the same shape.
 */
class AssertionGenerator {
    private static final String ASSERTJ_API = "org.assertj.core.api";
    private static final String OBJECT_CLASS_ACCESSOR = "getClass";
    private static final String ACTUAL_FIELD = "actual";
    private static final String PATH = "path";
    private static final String RETURN_MYSELF = "return myself";
    private static final String IS_NOT_NULL = "isNotNull";
    private static final String CHECK_PROPERTY = "checkProperty";
    private static final ClassName ABSTRACT_OBJECT_ASSERT =
            ClassName.get(ASSERTJ_API, "AbstractObjectAssert");
    private static final Set<String> ITERABLE_OR_MAP =
            Set.of("java.lang.Iterable", "java.util.Map");
    private static final AnnotationSpec NULL_MARKED =
            AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "NullMarked")).build();
    private static final AnnotationSpec NULLABLE =
            AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "Nullable")).build();

    private static final String SELF_NAME = "SELF";
    private static final String ACTUAL_TYPE_NAME = "ACTUAL";
    private static final TypeVariableName SELF = TypeVariableName.get(SELF_NAME);
    private static final TypeVariableName ACTUAL_TYPE = TypeVariableName.get(ACTUAL_TYPE_NAME);
    private static final ClassName REPRESENTATION =
            ClassName.get("org.assertj.core.presentation", "Representation");

    private final TypeModel model;
    private final GenerationContext context;
    private final List<PropertyModel> properties;

    AssertionGenerator(final TypeModel model, final GenerationContext context) {
        this.model = model;
        this.context = context;
        this.properties =
                PropertyModel.distinctByName(model.properties()).stream()
                        .filter(property -> !OBJECT_CLASS_ACCESSOR.equals(property.accessor()))
                        .collect(Collectors.toList());
    }

    private static String capitalized(final String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    String abstractName() {
        return "Abstract" + model.simpleName() + "Assert";
    }

    String concreteName() {
        return model.simpleName() + "Assert";
    }

    /** The abstract assertion class of the configured type, which is the null marked scope. */
    TypeSpec abstractTypeSpec() {
        return topLevel(
                abstractTypeSpecBuilder(ClassName.get(model.targetPackage(), abstractName())));
    }

    /** The concrete assertion class of the configured type, which is the null marked scope. */
    TypeSpec concreteTypeSpec() {
        return topLevel(
                concreteTypeSpecBuilder(
                        ClassName.get(model.targetPackage(), abstractName()),
                        ClassName.get(model.targetPackage(), concreteName())));
    }

    private TypeSpec topLevel(final TypeSpec.Builder builder) {
        if (context.jspecifyAvailable()) {
            builder.addAnnotation(NULL_MARKED);
        }
        return builder.build();
    }

    private TypeSpec.Builder abstractTypeSpecBuilder(final ClassName abstractClass) {
        final TypeSpec.Builder builder =
                TypeSpec.classBuilder(abstractName())
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addAnnotation(generatedAnnotation())
                        .addTypeVariable(
                                TypeVariableName.get(
                                        SELF_NAME,
                                        ParameterizedTypeName.get(
                                                abstractClass, SELF, ACTUAL_TYPE)))
                        .addTypeVariable(TypeVariableName.get(ACTUAL_TYPE_NAME, assertedType()))
                        .superclass(
                                ParameterizedTypeName.get(
                                        ABSTRACT_OBJECT_ASSERT, SELF, ACTUAL_TYPE))
                        .addMethod(abstractConstructor());
        if (!properties.isEmpty()) {
            builder.addField(
                    FieldSpec.builder(String.class, PATH, Modifier.PRIVATE, Modifier.FINAL)
                            .build());
        }
        final Set<String> inherited =
                context.inheritedMethodNames(ABSTRACT_OBJECT_ASSERT.toString());
        properties.forEach(
                property -> {
                    final List<MethodSpec> methods = generatedMethods(property);
                    final List<String> clashing =
                            methods.stream()
                                    .map(method -> method.name)
                                    .filter(inherited::contains)
                                    .collect(Collectors.toList());
                    if (!clashing.isEmpty()) {
                        context.reportError(clashMessage(property, clashing));
                    }
                    methods.stream()
                            .filter(method -> !inherited.contains(method.name))
                            .forEach(builder::addMethod);
                });
        if (!properties.isEmpty()) {
            builder.addMethod(isNotNullMethod()).addMethod(checkPropertyMethod());
        }
        model.nestedTypes()
                .forEach(
                        nested -> {
                            final AssertionGenerator generator =
                                    new AssertionGenerator(nested, context);
                            builder.addType(
                                    generator
                                            .abstractTypeSpecBuilder(
                                                    abstractClass.nestedClass(
                                                            generator.abstractName()))
                                            .addModifiers(Modifier.STATIC)
                                            .build());
                        });
        return builder;
    }

    private TypeSpec.Builder concreteTypeSpecBuilder(
            final ClassName abstractClass, final ClassName concreteClass) {
        final TypeSpec.Builder builder =
                TypeSpec.classBuilder(concreteName())
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(generatedAnnotation())
                        .superclass(
                                ParameterizedTypeName.get(
                                        abstractClass, concreteClass, assertedType()))
                        .addMethod(concreteConstructor(concreteClass))
                        .addMethod(assertThatMethod(concreteClass));
        model.nestedTypes()
                .forEach(
                        nested -> {
                            final AssertionGenerator generator =
                                    new AssertionGenerator(nested, context);
                            builder.addType(
                                    generator
                                            .concreteTypeSpecBuilder(
                                                    abstractClass.nestedClass(
                                                            generator.abstractName()),
                                                    concreteClass.nestedClass(
                                                            generator.concreteName()))
                                            .addModifiers(Modifier.STATIC)
                                            .build());
                        });
        return builder;
    }

    private MethodSpec abstractConstructor() {
        final MethodSpec.Builder constructor =
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addParameter(nullable(ACTUAL_TYPE), ACTUAL_FIELD, Modifier.FINAL)
                        .addParameter(
                                ParameterizedTypeName.get(
                                        ClassName.get(Class.class),
                                        WildcardTypeName.subtypeOf(Object.class)),
                                "selfType",
                                Modifier.FINAL)
                        .addStatement("super($L, selfType)", ACTUAL_FIELD);
        if (!properties.isEmpty()) {
            constructor.addStatement(
                    "this.$L = $T.rootedAt($S)", PATH, AssertionPath.class, model.simpleName());
        }
        return constructor.build();
    }

    private MethodSpec concreteConstructor(final ClassName concreteClass) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(nullable(assertedType()), ACTUAL_FIELD, Modifier.FINAL)
                .addStatement("super($L, $T.class)", ACTUAL_FIELD, concreteClass)
                .build();
    }

    private MethodSpec assertThatMethod(final ClassName concreteClass) {
        return MethodSpec.methodBuilder("assertThat")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(nullable(assertedType()), ACTUAL_FIELD, Modifier.FINAL)
                .addStatement("return new $T($L)", concreteClass, ACTUAL_FIELD)
                .returns(concreteClass)
                .build();
    }

    /**
     * The methods generated for a property. The names of those that an inherited AssertJ method
     * already has are reported once for the property and the methods are left out, so that the only
     * compiler error is the processor's.
     */
    private List<MethodSpec> generatedMethods(final PropertyModel property) {
        final List<MethodSpec> methods = new ArrayList<>();
        methods.add(hasMethod(property));
        methods.add(hasSatisfyingMethod(property));
        if (isBoolean(property)) {
            methods.add(booleanMethod("is" + capitalized(property.name()), property, true));
            methods.add(booleanMethod("isNot" + capitalized(property.name()), property, false));
        }
        return methods;
    }

    private String clashMessage(final PropertyModel property, final List<String> methodNames) {
        return "Property '"
                + property.name()
                + "' of '"
                + model.qualifiedName()
                + "' generates "
                + methodNames.stream()
                        .map(name -> "'" + name + "'")
                        .collect(Collectors.joining(" and "))
                + ", which clashes with inherited AssertJ methods of the same names;"
                + " rename the property";
    }

    /** {@code hasFoo(value)}: deep equality of the property with the value. */
    private MethodSpec hasMethod(final PropertyModel property) {
        return MethodSpec.methodBuilder("has" + capitalized(property.name()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(valueType(property), "value", Modifier.FINAL)
                .addStatement("$L()", IS_NOT_NULL)
                .addStatement(
                        "return $L($S, $L.$L(), value)",
                        CHECK_PROPERTY,
                        property.name(),
                        ACTUAL_FIELD,
                        property.accessor())
                .returns(SELF)
                .build();
    }

    /**
     * {@code hasFooSatisfying(requirements)}: the requirements for the property value. The path
     * continues into the requirements, except for a collection or map, whose elements start at
     * their own type.
     */
    private MethodSpec hasSatisfyingMethod(final PropertyModel property) {
        final MethodSpec.Builder method =
                MethodSpec.methodBuilder("has" + capitalized(property.name()) + "Satisfying")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(requirementsType(property), "requirements", Modifier.FINAL)
                        .addStatement("$L()", IS_NOT_NULL);
        if (isIterableOrMap(property.type())) {
            method.addStatement(
                    "$T.restartingWithin($L.$L(), requirements)",
                    AssertionPath.class,
                    ACTUAL_FIELD,
                    property.accessor());
        } else {
            method.addStatement(
                    "$T.within($L + $S, $L.$L(), requirements)",
                    AssertionPath.class,
                    PATH,
                    "." + property.name(),
                    ACTUAL_FIELD,
                    property.accessor());
        }
        return method.addStatement(RETURN_MYSELF).returns(SELF).build();
    }

    /** {@code isFoo()} or {@code isNotFoo()}: the property value is {@code expected}. */
    private MethodSpec booleanMethod(
            final String name, final PropertyModel property, final boolean expected) {
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$L()", IS_NOT_NULL)
                .addStatement(
                        "return $L($S, $L.$L(), $L)",
                        CHECK_PROPERTY,
                        property.name(),
                        ACTUAL_FIELD,
                        property.accessor(),
                        expected)
                .returns(SELF)
                .build();
    }

    /** Whether the property is a boolean, primitive or boxed, that has a single type. */
    private boolean isBoolean(final PropertyModel property) {
        final TypeMirror type = property.type();
        return !isConflicting(property)
                && (type.getKind() == TypeKind.BOOLEAN
                        || type.getKind() == TypeKind.DECLARED
                                && asTypeElement(type)
                                        .getQualifiedName()
                                        .contentEquals(Boolean.class.getCanonicalName()));
    }

    /** Whether the type is an array, or a collection or a map, whose elements are asserted. */
    private static boolean isIterableOrMap(final TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return true;
        }
        return type.getKind() == TypeKind.DECLARED && isOrExtendsIterableOrMap(asTypeElement(type));
    }

    private static boolean isOrExtendsIterableOrMap(final TypeElement type) {
        if (ITERABLE_OR_MAP.contains(type.getQualifiedName().toString())) {
            return true;
        }
        return Stream.concat(Stream.of(type.getSuperclass()), type.getInterfaces().stream())
                .filter(supertype -> supertype.getKind() == TypeKind.DECLARED)
                .anyMatch(supertype -> isOrExtendsIterableOrMap(asTypeElement(supertype)));
    }

    /** Overrides {@code isNotNull()} so that it fails with the path of the asserted value. */
    private MethodSpec isNotNullMethod() {
        return MethodSpec.methodBuilder(IS_NOT_NULL)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("if ($L == null)", ACTUAL_FIELD)
                .addStatement("failWithMessage($S, $L)", "%nExpecting %s not to be null", PATH)
                .endControlFlow()
                .addStatement(RETURN_MYSELF)
                .returns(SELF)
                .build();
    }

    private MethodSpec checkPropertyMethod() {
        return MethodSpec.methodBuilder(CHECK_PROPERTY)
                .addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "property", Modifier.FINAL)
                .addParameter(nullable(TypeName.OBJECT), "actualValue", Modifier.FINAL)
                .addParameter(nullable(TypeName.OBJECT), "expectedValue", Modifier.FINAL)
                .beginControlFlow("if (!$T.deepEquals(actualValue, expectedValue))", Objects.class)
                .addStatement(
                        "final $T representation = getWritableAssertionInfo().representation()",
                        REPRESENTATION)
                .addStatement(
                        "throw failureWithActualExpected(actualValue, expectedValue, $S, $L,"
                                + " property, representation.toStringOf(expectedValue),"
                                + " representation.toStringOf(actualValue))",
                        "%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s",
                        PATH)
                .endControlFlow()
                .addStatement(RETURN_MYSELF)
                .returns(SELF)
                .build();
    }

    private TypeName valueType(final PropertyModel property) {
        if (isConflicting(property) || property.type().getKind() == TypeKind.TYPEVAR) {
            return nullable(TypeName.OBJECT);
        }
        return nullable(typeVarsToWildcards(property.type(), false));
    }

    private TypeName requirementsType(final PropertyModel property) {
        final TypeName value =
                isConflicting(property)
                        ? TypeName.OBJECT
                        : nullableIf(
                                property.nullability() == Nullability.NULLABLE,
                                typeVarsToWildcards(property.type(), true));
        return ParameterizedTypeName.get(
                ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(value));
    }

    private boolean isConflicting(final PropertyModel property) {
        return model.properties().stream()
                        .filter(other -> other.name().equals(property.name()))
                        .count()
                > 1;
    }

    private TypeName nullableIf(final boolean nullable, final TypeName type) {
        return nullable ? nullable(type) : type;
    }

    /** Marks the type as nullable when JSpecify is available. Primitives cannot be annotated. */
    private TypeName nullable(final TypeName type) {
        return context.jspecifyAvailable() && !type.isPrimitive() ? type.annotated(NULLABLE) : type;
    }

    /** The asserted type, with wildcards for the type parameters of a generic type. */
    private TypeName assertedType() {
        if (model.typeParameters().isEmpty()) {
            return TypeName.get(model.type());
        }
        return ParameterizedTypeName.get(
                ClassName.get(asTypeElement(model.type())),
                model.typeParameters().stream()
                        .map(parameter -> WildcardTypeName.subtypeOf(Object.class))
                        .toArray(TypeName[]::new));
    }

    private AnnotationSpec generatedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", context.generatorName())
                .addMember("date", "$S", context.generationTime())
                .build();
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

    private static TypeElement asTypeElement(final TypeMirror type) {
        return (TypeElement) ((DeclaredType) type).asElement();
    }
}
