package io.github.marmer.annotationprocessing;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
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

public class MatcherGenerator {
    private static final String RETURN_THIS = "return this";
    private static final String BUILDER_FIELD_NAME = "beanPropertyMatcher";

    private final ProcessingEnvironment processingEnv;
    private final TypeElement baseType;
    private final Supplier<LocalDateTime> generationTimeStamp;
    private final String generationMarker;
    private final Collection<Element> additionalOriginationElements;
    private final AnnotationMirror matcherConfigurationMirror;
    private final String simpleMatcherName;

    public MatcherGenerator(
        final ProcessingEnvironment processingEnv,
        final TypeElement baseType,
        final Supplier<LocalDateTime> generationTimeStamp,
        final String generationMarker,
        final Collection<Element> additionalOriginationElements,
        final AnnotationMirror matcherConfigurationMirror
    ) {
        this.processingEnv = processingEnv;
        this.baseType = baseType;
        this.generationTimeStamp = generationTimeStamp;
        this.generationMarker = generationMarker;
        this.additionalOriginationElements = additionalOriginationElements;
        this.matcherConfigurationMirror = matcherConfigurationMirror;
        this.simpleMatcherName =
            processingEnv.getTypeUtils().getDeclaredType(baseType).asElement().getSimpleName() + "Matcher";
    }

    private static String capitalized(final String value) {
        return value.isEmpty() ? value : value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private static String decapitalized(final String value) {
        return value.isEmpty() ? value : value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
    }

    private static boolean hasReturnType(final ExecutableElement element) {
        return element.getReturnType().getKind() != TypeKind.VOID;
    }

    private static boolean hasNoParameters(final ExecutableElement element) {
        return element.getParameters().isEmpty();
    }

    private static <T> List<T> distinctByName(final List<T> values, final java.util.function.Function<T, String> name) {
        final Map<String, T> distinct = new LinkedHashMap<>();
        values.forEach(value -> distinct.putIfAbsent(name.apply(value), value));
        return new ArrayList<>(distinct.values());
    }

    public void generate() {
        try {
            JavaFile.builder(getBasePackage(), getPreparedTypeSpecBuilder().build())
                .build()
                .writeTo(processingEnv.getFiler());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getBasePackage() {
        final String configuredPackage = getConfiguredPackage();
        if (configuredPackage.isBlank()) {
            return packageElementOf(baseType).toString();
        }
        return configuredPackage.replaceAll("\\.+$", "") + "." + packageElementOf(baseType);
    }

    private String getConfiguredPackage() {
        final AnnotationMirror generation = getAnnotationMirrorValue(matcherConfigurationMirror, "generation");
        final AnnotationMirror packageConfig = getAnnotationMirrorValue(generation, "packageConfig");
        return (String) getAnnotationValue(packageConfig, "value").getValue();
    }

    private AnnotationMirror getAnnotationMirrorValue(final AnnotationMirror mirror, final String name) {
        return (AnnotationMirror) getAnnotationValue(mirror, name).getValue();
    }

    private AnnotationValue getAnnotationValue(final AnnotationMirror mirror, final String name) {
        return processingEnv.getElementUtils().getElementValuesWithDefaults(mirror).entrySet().stream()
            .filter(entry -> entry.getKey().getSimpleName().contentEquals(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow();
    }

    private TypeSpec.Builder getPreparedTypeSpecBuilder() {
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
            .addTypes(getInnerMatchers())
            .addOriginatingElement(baseType);

        additionalOriginationElements.forEach(classBuilder::addOriginatingElement);

        return classBuilder;
    }

    private List<MethodSpec> getPropertyHamcrestMatcherMethods() {
        return distinctByName(properties(baseType), Property::name).stream()
            .map(this::toHamcrestMatcher)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private boolean isConflictingProperty(final String propertyName) {
        return properties(baseType).stream()
            .filter(property -> property.name().equals(propertyName))
            .count() > 1;
    }

    private List<MethodSpec> getPropertyHamcrestMatcherResetMethods() {
        return distinctByName(properties(baseType), Property::name).stream()
            .map(this::toHamcrestMatcherReset)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<MethodSpec> getPropertyEqualsMatcherMethods() {
        return properties(baseType).stream()
            .map(this::toEqualsMatcher)
            .collect(Collectors.toList());
    }

    private MethodSpec toHamcrestMatcher(final Property property) {
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

    private MethodSpec toHamcrestMatcherReset(final Property property) {
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

    private TypeName toParameterizedMatcherType(final Property property) {
        return ParameterizedTypeName.get(
            ClassName.get(Matcher.class),
            isConflictingProperty(property.name())
                ? WildcardTypeName.subtypeOf(TypeName.OBJECT)
                : WildcardTypeName.supertypeOf(typeVarsToWildcards(property.type(), true))
        );
    }

    private MethodSpec toEqualsMatcher(final Property property) {
        return MethodSpec.methodBuilder("with" + capitalized(property.name()))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(toEqualsMatcherParameterType(property), "value", Modifier.FINAL)
            .addStatement("$L.with($S, $T.equalTo(value))", BUILDER_FIELD_NAME, property.name(), Matchers.class)
            .addStatement(RETURN_THIS)
            .returns(getGeneratedTypeName())
            .build();
    }

    private TypeName toEqualsMatcherParameterType(final Property property) {
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
            return boxPrimitives
                ? TypeName.get(processingEnv.getTypeUtils().boxedClass((PrimitiveType) type).asType())
                : TypeName.get(type);
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
        return MethodSpec.methodBuilder("is" + baseType.getSimpleName())
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
                processingEnv.getTypeUtils().getDeclaredType(baseType)
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
            .addParameter(typeNameWithWildCards(baseType), "item", Modifier.FINAL)
            .addStatement("return $L.matches($L)", BUILDER_FIELD_NAME, "item")
            .returns(TypeName.BOOLEAN)
            .build();
    }

    private MethodSpec getDescribeMissmatchSafelyMethod() {
        return MethodSpec.methodBuilder("describeMismatchSafely")
            .addAnnotation(Override.class)
            .addParameter(typeNameWithWildCards(baseType), "item", Modifier.FINAL)
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
        return ParameterizedTypeName.get(ClassName.get(BeanPropertyMatcher.class), typeNameWithWildCards(baseType));
    }

    private TypeName getSuperClass() {
        return ParameterizedTypeName.get(ClassName.get(TypeSafeMatcher.class), typeNameWithWildCards(baseType));
    }

    private List<TypeSpec> getInnerMatchers() {
        final List<TypeElement> publicInnerTypes = new ArrayList<>();
        final List<TypeElement> nonPublicInnerTypes = new ArrayList<>();
        baseType.getEnclosedElements().stream()
            .filter(TypeElement.class::isInstance)
            .map(TypeElement.class::cast)
            .forEach(innerType -> {
                if (innerType.getModifiers().contains(Modifier.PUBLIC)) {
                    publicInnerTypes.add(innerType);
                } else {
                    nonPublicInnerTypes.add(innerType);
                }
            });

        nonPublicInnerTypes.forEach(this::logTypeSkipped);

        final List<Element> originatingElements = new ArrayList<>();
        originatingElements.add(baseType);
        originatingElements.addAll(additionalOriginationElements);

        return publicInnerTypes.stream()
            .map(innerType -> new MatcherGenerator(
                processingEnv,
                innerType,
                generationTimeStamp,
                generationMarker,
                originatingElements,
                matcherConfigurationMirror
            ).getPreparedTypeSpecBuilder()
                .addModifiers(Modifier.STATIC)
                .build())
            .collect(Collectors.toList());
    }

    private void logTypeSkipped(final TypeElement element) {
        MatcherGenerationProcessorWorker.logNote(
            processingEnv,
            "Matcher generation skipped for non public type: " + element.getQualifiedName(),
            element,
            matcherConfigurationMirror,
            MatcherGenerationProcessorWorker.getAnnotationValueForValue(matcherConfigurationMirror)
        );
    }

    private boolean isRecord(final TypeElement type) {
        return "RECORD".equals(type.getKind().name());
    }

    private List<Property> recordComponentProperties(final TypeElement type) {
        return type.getEnclosedElements().stream()
            .filter(ExecutableElement.class::isInstance)
            .map(ExecutableElement.class::cast)
            .filter(element -> MatcherGenerationProcessorWorker.isPublic(element)
                && !MatcherGenerationProcessorWorker.isStatic(element)
                && hasNoParameters(element)
                && hasReturnType(element)
                && isRecordComponentAccessor(element))
            .map(element -> new Property(element.getSimpleName().toString(), element.getReturnType()))
            .collect(Collectors.toList());
    }

    private boolean isRecordComponentAccessor(final ExecutableElement element) {
        final String name = element.getSimpleName().toString();
        return !name.equals("hashCode") && !name.equals("toString") && !name.startsWith("get")
            && !name.startsWith("is");
    }

    private List<Property> properties(final TypeElement type) {
        final List<ExecutableElement> getters = new ArrayList<>();
        final Map<String, Boolean> seenSimpleNames = new LinkedHashMap<>();
        transitiveInheritedElements(type).stream()
            .filter(this::isProperty)
            .map(ExecutableElement.class::cast)
            .forEach(element -> {
                if (seenSimpleNames.putIfAbsent(element.getSimpleName().toString(), true) == null) {
                    getters.add(element);
                }
            });
        final List<Property> getterProperties = getters.stream()
            .map(element -> new Property(
                withoutPropertyPrefix(element.getSimpleName().toString()),
                element.getReturnType()))
            .collect(Collectors.toList());

        if (isRecord(type)) {
            final List<Property> all = new ArrayList<>(recordComponentProperties(type));
            all.addAll(getterProperties);
            return distinctByName(all, Property::name);
        }
        return getterProperties;
    }

    private List<Element> transitiveInheritedElements(final TypeElement type) {
        final List<Element> elements = new ArrayList<>(type.getEnclosedElements());
        if (type.getSuperclass().getKind() != TypeKind.NONE) {
            elements.addAll(transitiveInheritedElements(asTypeElement(type.getSuperclass())));
        }
        type.getInterfaces()
            .forEach(anInterface -> elements.addAll(transitiveInheritedElements(asTypeElement(anInterface))));
        return elements;
    }

    private AnnotationSpec getGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", generationMarker)
            .addMember("date", "$S", generationTimeStamp.get())
            .build();
    }

    private TypeElement asTypeElement(final TypeMirror type) {
        return (TypeElement) processingEnv.getTypeUtils().asElement(type);
    }

    private boolean isMatcher(final TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED
            && Matcher.class.getCanonicalName().equals(asTypeElement(type).getQualifiedName().toString());
    }

    private PackageElement packageElementOf(final TypeElement type) {
        return processingEnv.getElementUtils().getPackageOf(type);
    }

    private TypeName typeNameWithWildCards(final TypeElement type) {
        if (type.getTypeParameters().isEmpty()) {
            return TypeName.get(type.asType());
        }
        return ParameterizedTypeName.get(
            ClassName.get(type),
            type.getTypeParameters().stream()
                .map(parameter -> WildcardTypeName.subtypeOf(Object.class))
                .toArray(TypeName[]::new)
        );
    }

    private boolean isProperty(final Element element) {
        if (!(element instanceof ExecutableElement)) {
            return false;
        }
        final ExecutableElement executable = (ExecutableElement) element;
        return MatcherGenerationProcessorWorker.isPublic(executable)
            && !MatcherGenerationProcessorWorker.isStatic(executable)
            && hasReturnTypeWithMatchingPropertyPrefix(executable)
            && hasNoParameters(executable);
    }

    private boolean hasReturnTypeWithMatchingPropertyPrefix(final ExecutableElement element) {
        final String name = element.getSimpleName().toString();
        final TypeKind returnKind = element.getReturnType().getKind();
        return (hasReturnType(element) && name.startsWith("get") && returnKind != TypeKind.BOOLEAN)
            || (name.startsWith("is") && returnKind == TypeKind.BOOLEAN);
    }

    private String withoutPropertyPrefix(final String name) {
        return decapitalized(name.replaceFirst("^((get)|(is))", ""));
    }

    private record Property(String name, TypeMirror type) {
    }
}
