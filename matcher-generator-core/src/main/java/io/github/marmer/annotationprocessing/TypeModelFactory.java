package io.github.marmer.annotationprocessing;

import io.github.marmer.annotationprocessing.model.Nullability;
import io.github.marmer.annotationprocessing.model.PropertyModel;
import io.github.marmer.annotationprocessing.model.TypeModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Creates the {@link TypeModel} of a configured type.
 */
class TypeModelFactory {
    private final ProcessingEnvironment processingEnv;
    private final AnnotationMirror configurationMirror;

    TypeModelFactory(final ProcessingEnvironment processingEnv, final AnnotationMirror configurationMirror) {
        this.processingEnv = processingEnv;
        this.configurationMirror = configurationMirror;
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

    private static <T> List<T> distinctByName(final List<T> values, final Function<T, String> name) {
        final Map<String, T> distinct = new LinkedHashMap<>();
        values.forEach(value -> distinct.putIfAbsent(name.apply(value), value));
        return new ArrayList<>(distinct.values());
    }

    TypeModel create(final TypeElement type) {
        return create(type, getTargetPackage(type));
    }

    private TypeModel create(final TypeElement type, final String targetPackage) {
        return new TypeModel(
            type.getQualifiedName().toString(),
            type.getSimpleName().toString(),
            packageOf(type).toString(),
            targetPackage,
            type.getTypeParameters().stream()
                .map(TypeParameterElement::getSimpleName)
                .map(Object::toString)
                .collect(Collectors.toList()),
            type.getKind() == ElementKind.RECORD ? TypeModel.Kind.RECORD : TypeModel.Kind.POJO,
            type.asType(),
            properties(type),
            nestedTypes(type, targetPackage)
        );
    }

    private List<TypeModel> nestedTypes(final TypeElement type, final String targetPackage) {
        final List<TypeModel> publicNestedTypes = new ArrayList<>();
        type.getEnclosedElements().stream()
            .filter(TypeElement.class::isInstance)
            .map(TypeElement.class::cast)
            .forEach(nestedType -> {
                if (MatcherGenerationProcessorWorker.isPublic(nestedType)) {
                    publicNestedTypes.add(create(nestedType, targetPackage));
                } else {
                    logTypeSkipped(nestedType);
                }
            });
        return publicNestedTypes;
    }

    private void logTypeSkipped(final TypeElement element) {
        MatcherGenerationProcessorWorker.logNote(
            processingEnv,
            "Matcher generation skipped for non public type: " + element.getQualifiedName(),
            element,
            configurationMirror,
            MatcherGenerationProcessorWorker.getAnnotationValueForValue(configurationMirror)
        );
    }

    private String getTargetPackage(final TypeElement type) {
        final String configuredPackage = getConfiguredPackage();
        if (configuredPackage.isBlank()) {
            return packageOf(type).toString();
        }
        return configuredPackage.replaceAll("\\.+$", "") + "." + packageOf(type);
    }

    private String getConfiguredPackage() {
        final AnnotationMirror generation = getAnnotationMirrorValue(configurationMirror, "generation");
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

    private PackageElement packageOf(final TypeElement type) {
        return processingEnv.getElementUtils().getPackageOf(type);
    }

    private List<PropertyModel> recordComponentProperties(final TypeElement type) {
        return type.getEnclosedElements().stream()
            .filter(ExecutableElement.class::isInstance)
            .map(ExecutableElement.class::cast)
            .filter(element -> MatcherGenerationProcessorWorker.isPublic(element)
                && !MatcherGenerationProcessorWorker.isStatic(element)
                && hasNoParameters(element)
                && hasReturnType(element)
                && isRecordComponentAccessor(element))
            .map(element -> new PropertyModel(
                element.getSimpleName().toString(),
                element.getReturnType(),
                element.getSimpleName().toString(),
                Nullability.UNSPECIFIED))
            .collect(Collectors.toList());
    }

    private boolean isRecordComponentAccessor(final ExecutableElement element) {
        final String name = element.getSimpleName().toString();
        return !name.equals("hashCode") && !name.equals("toString") && !name.startsWith("get")
            && !name.startsWith("is");
    }

    private List<PropertyModel> properties(final TypeElement type) {
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
        final List<PropertyModel> getterProperties = getters.stream()
            .map(element -> new PropertyModel(
                withoutPropertyPrefix(element.getSimpleName().toString()),
                element.getReturnType(),
                element.getSimpleName().toString(),
                Nullability.UNSPECIFIED))
            .collect(Collectors.toList());

        if (type.getKind() == ElementKind.RECORD) {
            final List<PropertyModel> all = new ArrayList<>(recordComponentProperties(type));
            all.addAll(getterProperties);
            return distinctByName(all, PropertyModel::name);
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

    private TypeElement asTypeElement(final TypeMirror type) {
        return (TypeElement) processingEnv.getTypeUtils().asElement(type);
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
}
