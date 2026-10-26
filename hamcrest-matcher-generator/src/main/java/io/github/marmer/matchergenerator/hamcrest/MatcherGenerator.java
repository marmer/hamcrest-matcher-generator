package io.github.marmer.matchergenerator.hamcrest;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
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

class MatcherGenerator {
    private static final String RETURN_THIS = "return this";
    private static final String BUILDER_FIELD_NAME = "beanPropertyMatcher";

    private final TypeModel model;
    private final GenerationContext context;
    private final String simpleMatcherName;

    MatcherGenerator(final TypeModel model, final GenerationContext context) {
        this.model = model;
        this.context = context;
        this.simpleMatcherName = model.simpleName() + "Matcher";
    }

    private static String capitalized(final String value) {
        return value.isEmpty() ? value : value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    String simpleMatcherName() {
        return simpleMatcherName;
    }

    TypeSpec.Builder getPreparedTypeSpecBuilder() {
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(simpleMatcherName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(getGeneratedAnnotation())
            .superclass(getSuperClass())
            .addFields(getFields())
            .addMethod(getConstructor())
            .addMethods(getPropertyHamcrestMatcherMethods())
            .addMethods(getPropertyHamcrestMatcherResetMethods())
            .addMethods(getPropertyEqualsMatcherMethods())
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
            .count() > 1;
    }

    private List<MethodSpec> getPropertyHamcrestMatcherResetMethods() {
        return PropertyModel.distinctByName(model.properties()).stream()
            .map(this::toHamcrestMatcherReset)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<MethodSpec> getPropertyEqualsMatcherMethods() {
        return model.properties().stream()
            .map(this::toEqualsMatcher)
            .collect(Collectors.toList());
    }

    private MethodSpec toHamcrestMatcher(final PropertyModel property) {
        if (isMatcher(property.type())) {
            return null;
        }
        return MethodSpec.methodBuilder("with" + capitalized(property.name()))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(toParameterizedMatcherType(property), "matcher", Modifier.FINAL)
            .addStatement("$L.with($S, matcher)", BUILDER_FIELD_NAME, property.name())
            .addStatement(RETURN_THIS)
            .returns(getGeneratedTypeName())
            .build();
    }

    private MethodSpec toHamcrestMatcherReset(final PropertyModel property) {
        if (isMatcher(property.type())) {
            return null;
        }
        return MethodSpec.methodBuilder("reset" + capitalized(property.name()))
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
                : WildcardTypeName.supertypeOf(typeVarsToWildcards(property.type(), true))
        );
    }

    private MethodSpec toEqualsMatcher(final PropertyModel property) {
        return MethodSpec.methodBuilder("with" + capitalized(property.name()))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(toEqualsMatcherParameterType(property), "value", Modifier.FINAL)
            .addStatement("$L.with($S, $T.equalTo(value))", BUILDER_FIELD_NAME, property.name(), Matchers.class)
            .addStatement(RETURN_THIS)
            .returns(getGeneratedTypeName())
            .build();
    }

    private TypeName toEqualsMatcherParameterType(final PropertyModel property) {
        if (property.type().getKind() == TypeKind.TYPEVAR) {
            return TypeName.get(Object.class);
        }
        return typeVarsToWildcards(property.type(), false);
    }

    private TypeName typeVarsToWildcards(final TypeMirror type, final boolean boxPrimitives) {
        if (type instanceof DeclaredType && !((DeclaredType) type).getTypeArguments().isEmpty()) {
            final TypeName[] typeArguments = ((DeclaredType) type).getTypeArguments().stream()
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

    private TypeName typeArgumentToWildcard(final TypeMirror argument, final boolean boxPrimitives) {
        if (argument instanceof TypeVariable) {
            return WildcardTypeName.subtypeOf(Object.class);
        }
        if (argument instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) argument;
            if (wildcard.getExtendsBound() != null) {
                return WildcardTypeName.subtypeOf(typeVarsToWildcards(wildcard.getExtendsBound(), boxPrimitives));
            }
            if (wildcard.getSuperBound() != null) {
                return WildcardTypeName.supertypeOf(typeVarsToWildcards(wildcard.getSuperBound(), boxPrimitives));
            }
            return WildcardTypeName.subtypeOf(Object.class);
        }
        return WildcardTypeName.subtypeOf(typeVarsToWildcards(argument, boxPrimitives));
    }

    private MethodSpec getApiInitializer() {
        return MethodSpec.methodBuilder("is" + model.simpleName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addStatement("return new $T()", getGeneratedTypeName())
            .returns(getGeneratedTypeName())
            .build();
    }

    private ClassName getGeneratedTypeName() {
        return ClassName.get("", simpleMatcherName);
    }

    private MethodSpec getConstructor() {
        return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addStatement(
                "$L = new $T($T.class)",
                BUILDER_FIELD_NAME,
                getBuilderFieldType(),
                rawTypeName()
            )
            .build();
    }

    private List<MethodSpec> getMatcherMethods() {
        return List.of(
            getDescribeToMethod(),
            getMatchesSafelyMethod(),
            getDescribeMissmatchSafelyMethod()
        );
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
            .addStatement("$L.describeMismatch($L, $L)", BUILDER_FIELD_NAME, "item", "description")
            .addModifiers(Modifier.PROTECTED)
            .build();
    }

    private List<FieldSpec> getFields() {
        return List.of(FieldSpec.builder(
            getBuilderFieldType(),
            BUILDER_FIELD_NAME,
            Modifier.PRIVATE,
            Modifier.FINAL
        ).build());
    }

    private TypeName getBuilderFieldType() {
        return ParameterizedTypeName.get(ClassName.get(BeanPropertyMatcher.class), typeNameWithWildCards());
    }

    private TypeName getSuperClass() {
        return ParameterizedTypeName.get(ClassName.get(TypeSafeMatcher.class), typeNameWithWildCards());
    }

    private List<TypeSpec> getInnerMatchers() {
        return model.nestedTypes().stream()
            .map(innerType -> new MatcherGenerator(innerType, context).getPreparedTypeSpecBuilder()
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
            && Matcher.class.getCanonicalName().equals(asTypeElement(type).getQualifiedName().toString());
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
                .toArray(TypeName[]::new)
        );
    }
}
