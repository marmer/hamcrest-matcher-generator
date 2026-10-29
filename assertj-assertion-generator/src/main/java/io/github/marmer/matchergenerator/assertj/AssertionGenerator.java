/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    private static final String ADDRESSED = "addressed";
    private static final String PROPERTY_NAMES = "PROPERTY_NAMES";
    private static final String RETURN_MYSELF = "return myself";
    private static final String IS_NOT_NULL = "isNotNull";
    private static final String CHECK_PROPERTY = "checkProperty";
    private static final ClassName ABSTRACT_OBJECT_ASSERT =
            ClassName.get(ASSERTJ_API, "AbstractObjectAssert");
    private static final ClassName INSTANCE_OF_ASSERT_FACTORY =
            ClassName.get(ASSERTJ_API, "InstanceOfAssertFactory");
    private static final ClassName REFLECT_TYPE = ClassName.get("java.lang.reflect", "Type");
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

    /** The properties the assertions check, in declaration order, without {@code getClass()}. */
    List<PropertyModel> properties() {
        return properties;
    }

    String abstractName() {
        return abstractNameOf(model.simpleName());
    }

    String concreteName() {
        return concreteNameOf(model.simpleName());
    }

    static String abstractNameOf(final String simpleName) {
        return "Abstract" + simpleName + "Assert";
    }

    static String concreteNameOf(final String simpleName) {
        return simpleName + "Assert";
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

    /** The builder's type as a top level type, null marked when JSpecify is available. */
    TypeSpec topLevel(final TypeSpec.Builder builder) {
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
            builder.addField(propertyNamesField())
                    .addField(
                            FieldSpec.builder(String.class, PATH, Modifier.PRIVATE, Modifier.FINAL)
                                    .build())
                    .addField(addressedField());
        }
        final Set<String> inherited =
                context.inheritedMethodNames(ABSTRACT_OBJECT_ASSERT.toString());
        final List<List<MethodSpec>> methodsByProperty =
                properties.stream().map(this::generatedMethods).collect(Collectors.toList());
        final Set<String> sharedNames =
                reportSharedNamesAmongProperties(methodsByProperty, inherited);
        IntStream.range(0, properties.size())
                .forEach(
                        index -> {
                            final PropertyModel property = properties.get(index);
                            final List<MethodSpec> methods = methodsByProperty.get(index);
                            final List<String> clashing =
                                    methods.stream()
                                            .map(method -> method.name)
                                            .filter(inherited::contains)
                                            .collect(Collectors.toList());
                            if (!clashing.isEmpty()) {
                                context.reportError(clashMessage(property, clashing));
                            }
                            methods.stream()
                                    .filter(
                                            method ->
                                                    !inherited.contains(method.name)
                                                            && !sharedNames.contains(method.name))
                                    .forEach(builder::addMethod);
                        });
        if (!properties.isEmpty()) {
            builder.addMethod(isNotNullMethod())
                    .addMethod(checkPropertyMethod())
                    .addMethod(satisfiesCheckingAllPropertiesMethod())
                    .addMethod(hasAllPropertiesCheckedMethod());
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
                        .addField(instanceOfAssertFactoryField(concreteClass))
                        .addMethod(concreteConstructor(concreteClass))
                        .addMethod(assertThatMethod(concreteClass));
        reportFactoryConstantClashes();
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
                    "this.$L = $T.rootedAt($S, $L)",
                    PATH,
                    AssertionPath.class,
                    model.simpleName(),
                    ACTUAL_FIELD);
        }
        return constructor.build();
    }

    /**
     * The {@code InstanceOfAssertFactory} constant of the type, such as {@code ORDER_LINE}, which
     * navigates to the type with {@code as(..)}. A generic type is created with its raw class and
     * the erasures of its bounds as type arguments, since the type arguments are not known at run
     * time.
     */
    private FieldSpec instanceOfAssertFactoryField(final ClassName concreteClass) {
        final ClassName rawClass = rawClass();
        final FieldSpec.Builder field =
                FieldSpec.builder(
                        ParameterizedTypeName.get(
                                INSTANCE_OF_ASSERT_FACTORY, rawClass, concreteClass),
                        factoryName(model.simpleName()),
                        Modifier.PUBLIC,
                        Modifier.STATIC,
                        Modifier.FINAL);
        if (model.typeParameters().isEmpty()) {
            field.initializer(
                    "new $T<>($T.class, $T::new)",
                    INSTANCE_OF_ASSERT_FACTORY,
                    rawClass,
                    concreteClass);
        } else {
            field.addAnnotation(
                    AnnotationSpec.builder(SuppressWarnings.class)
                            .addMember("value", "$S", "rawtypes")
                            .build());
            field.initializer(
                    "new $T<>($T.class, new $T[] {$L}, $T::new)",
                    INSTANCE_OF_ASSERT_FACTORY,
                    rawClass,
                    REFLECT_TYPE,
                    typeArgumentClasses(),
                    concreteClass);
        }
        return field.build();
    }

    /**
     * A default method of the soft assertions mixin, such as {@code assertThat(Order)}: a soft
     * proxy of the concrete assertion class for the value.
     */
    MethodSpec softAssertionMethod(final String name, final ClassName concreteClass) {
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(nullable(assertedType()), ACTUAL_FIELD, Modifier.FINAL)
                .addStatement(
                        "return proxy($T.class, $T.class, $L)",
                        concreteClass,
                        rawClass(),
                        ACTUAL_FIELD)
                .returns(concreteClass)
                .build();
    }

    /** The erasure of the type, which is the class of the values the assertions accept. */
    private ClassName rawClass() {
        return ClassName.get(asTypeElement(model.type()));
    }

    /** The erasures of the bounds of the type parameters, as class literals, comma separated. */
    private CodeBlock typeArgumentClasses() {
        return asTypeElement(model.type()).getTypeParameters().stream()
                .map(parameter -> boundClass(parameter.getBounds()))
                .map(clazz -> CodeBlock.of("$T.class", clazz))
                .collect(CodeBlock.joining(", "));
    }

    /** The erasure of the first bound, which is {@code Object} for an unbounded parameter. */
    private static TypeName boundClass(final List<? extends TypeMirror> bounds) {
        final TypeMirror bound = bounds.get(0);
        if (bound.getKind() == TypeKind.DECLARED) {
            return ClassName.get(asTypeElement(bound));
        }
        return TypeName.OBJECT;
    }

    private static String factoryName(final String simpleName) {
        return simpleName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
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
        methods.add(ignoringMethod(property));
        return methods;
    }

    /**
     * Reports one error per generated method name that two or more properties generate, such as
     * {@code isNotFoo()} for a boolean {@code foo} and a boolean {@code notFoo}. The names that are
     * inherited are left to the per property check. Returns the reported names, whose methods are
     * left out.
     */
    private Set<String> reportSharedNamesAmongProperties(
            final List<List<MethodSpec>> methodsByProperty, final Set<String> inherited) {
        final Map<String, List<String>> propertiesByName = new LinkedHashMap<>();
        IntStream.range(0, properties.size())
                .forEach(
                        index ->
                                methodsByProperty.get(index).stream()
                                        .map(method -> method.name)
                                        .filter(name -> !inherited.contains(name))
                                        .forEach(
                                                name ->
                                                        propertiesByName
                                                                .computeIfAbsent(
                                                                        name,
                                                                        key -> new ArrayList<>())
                                                                .add(
                                                                        properties
                                                                                .get(index)
                                                                                .name())));
        final Set<String> shared = new HashSet<>();
        propertiesByName.forEach(
                (name, names) -> {
                    if (names.size() > 1) {
                        context.reportError(sharedNameMessage(name, names));
                        shared.add(name);
                    }
                });
        return shared;
    }

    private String sharedNameMessage(final String methodName, final List<String> propertyNames) {
        return "Properties "
                + quotedAndJoined(propertyNames)
                + " of '"
                + model.qualifiedName()
                + "' generate '"
                + methodName
                + "', which clashes between the generated methods; rename one of the properties";
    }

    /**
     * Reports one error per factory constant that two or more nested types of the type share, such
     * as {@code FOO_BAR} for {@code FooBar} and {@code Foo_Bar}. The types are one scope, so the
     * constants of their assertion classes clash for anyone who imports them by name.
     */
    private void reportFactoryConstantClashes() {
        model.nestedTypes().stream()
                .collect(
                        Collectors.groupingBy(
                                nested -> factoryName(nested.simpleName()),
                                TreeMap::new,
                                Collectors.toList()))
                .forEach(
                        (constant, nestedTypes) -> {
                            if (nestedTypes.size() > 1) {
                                context.reportError(
                                        factoryConstantClashMessage(
                                                constant,
                                                nestedTypes.stream()
                                                        .map(TypeModel::qualifiedName)
                                                        .collect(Collectors.toList())));
                            }
                        });
    }

    private String factoryConstantClashMessage(
            final String constant, final List<String> typeNames) {
        return "Nested types "
                + quotedAndJoined(typeNames)
                + " of '"
                + model.qualifiedName()
                + "' get the factory constant '"
                + constant
                + "', which clashes; rename one of the types";
    }

    static String quotedAndJoined(final List<String> names) {
        return names.stream().map(name -> "'" + name + "'").collect(Collectors.joining(" and "));
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
                        .addStatement("$L()", IS_NOT_NULL)
                        .addStatement("$L.add($S)", ADDRESSED, property.name());
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

    /**
     * {@code ignoringFoo()}: the property counts as addressed by the completeness check without any
     * value check.
     */
    private MethodSpec ignoringMethod(final PropertyModel property) {
        return MethodSpec.methodBuilder("ignoring" + capitalized(property.name()))
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$L.add($S)", ADDRESSED, property.name())
                .addStatement(RETURN_MYSELF)
                .returns(SELF)
                .build();
    }

    /** Whether the property is a boolean, primitive or boxed, that has a single type. */
    private boolean isBoolean(final PropertyModel property) {
        final TypeMirror type = property.type();
        return !isConflicting(property)
                && (type.getKind() == TypeKind.BOOLEAN
                        || (type.getKind() == TypeKind.DECLARED
                                && asTypeElement(type)
                                        .getQualifiedName()
                                        .contentEquals(Boolean.class.getCanonicalName())));
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

    /**
     * The names of the properties, in declaration order. The completeness check reports the ones
     * that are neither addressed nor ignored in this order.
     */
    private FieldSpec propertyNamesField() {
        return FieldSpec.builder(
                        ParameterizedTypeName.get(List.class, String.class),
                        PROPERTY_NAMES,
                        Modifier.PRIVATE,
                        Modifier.STATIC,
                        Modifier.FINAL)
                .initializer(
                        "$T.of($L)",
                        List.class,
                        properties.stream()
                                .map(property -> CodeBlock.of("$S", property.name()))
                                .collect(CodeBlock.joining(", ")))
                .build();
    }

    /** The names of the properties that are checked or ignored, one set per assertion object. */
    private FieldSpec addressedField() {
        return FieldSpec.builder(
                        ParameterizedTypeName.get(Set.class, String.class),
                        ADDRESSED,
                        Modifier.PRIVATE,
                        Modifier.FINAL)
                .initializer("new $T<>()", HashSet.class)
                .build();
    }

    /**
     * {@code satisfiesCheckingAllProperties(requirements)}: the scope form of the completeness
     * check. Only the calls made by the requirements count, so the set of the chain before is set
     * aside while they run and merged back afterwards, which also keeps the chain when they fail.
     */
    private MethodSpec satisfiesCheckingAllPropertiesMethod() {
        return MethodSpec.methodBuilder("satisfiesCheckingAllProperties")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterizedTypeName.get(
                                ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(SELF)),
                        "requirements",
                        Modifier.FINAL)
                .addStatement("$L()", IS_NOT_NULL)
                .addStatement(
                        "final $T<$T> chain = new $T<>($L)",
                        Set.class,
                        String.class,
                        HashSet.class,
                        ADDRESSED)
                .addStatement("$L.clear()", ADDRESSED)
                .beginControlFlow("try")
                .addStatement("requirements.accept(myself)")
                .addStatement("return hasAllPropertiesChecked()")
                .nextControlFlow("finally")
                .addStatement("$L.addAll(chain)", ADDRESSED)
                .endControlFlow()
                .returns(SELF)
                .build();
    }

    /**
     * {@code hasAllPropertiesChecked()}: the short form of the completeness check, at the end of a
     * chain. It fails with the names of the properties that are neither checked nor ignored.
     */
    private MethodSpec hasAllPropertiesCheckedMethod() {
        return MethodSpec.methodBuilder("hasAllPropertiesChecked")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$L()", IS_NOT_NULL)
                .addStatement(
                        "final $T<$T> neither = $L.stream().filter(property -> !$L.contains(property))"
                                + ".collect($T.toList())",
                        List.class,
                        String.class,
                        PROPERTY_NAMES,
                        ADDRESSED,
                        Collectors.class)
                .beginControlFlow("if (!neither.isEmpty())")
                .addStatement(
                        "failWithMessage($S, $L, $T.join($S, neither))",
                        "%nExpecting every property of %s to be checked or ignored, but these were"
                                + " neither: %s",
                        PATH,
                        String.class,
                        ", ")
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
                .addStatement("$L.add(property)", ADDRESSED)
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
    TypeName nullable(final TypeName type) {
        return context.jspecifyAvailable() && !type.isPrimitive() ? type.annotated(NULLABLE) : type;
    }

    /** The asserted type, with wildcards for the type parameters of a generic type. */
    TypeName assertedType() {
        if (model.typeParameters().isEmpty()) {
            return TypeName.get(model.type());
        }
        return ParameterizedTypeName.get(
                ClassName.get(asTypeElement(model.type())),
                model.typeParameters().stream()
                        .map(parameter -> WildcardTypeName.subtypeOf(Object.class))
                        .toArray(TypeName[]::new));
    }

    AnnotationSpec generatedAnnotation() {
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
