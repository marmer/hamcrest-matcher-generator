package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
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
                if (ElementSupport.isPublic(nestedType)) {
                    publicNestedTypes.add(create(nestedType, targetPackage));
                } else {
                    logTypeSkipped(nestedType);
                }
            });
        return publicNestedTypes;
    }

    private void logTypeSkipped(final TypeElement element) {
        ElementSupport.logNote(
            processingEnv,
            "Matcher generation skipped for non public type: " + element.getQualifiedName(),
            element,
            configurationMirror,
            ElementSupport.valueAttribute(processingEnv, configurationMirror)
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
        return (String) ElementSupport.valueAttribute(processingEnv, packageConfig).getValue();
    }

    private AnnotationMirror getAnnotationMirrorValue(final AnnotationMirror mirror, final String name) {
        return (AnnotationMirror) ElementSupport.annotationValue(processingEnv, mirror, name).getValue();
    }

    private PackageElement packageOf(final TypeElement type) {
        return processingEnv.getElementUtils().getPackageOf(type);
    }

    private List<PropertyModel> recordComponentProperties(final TypeElement type) {
        return type.getEnclosedElements().stream()
            .filter(ExecutableElement.class::isInstance)
            .map(ExecutableElement.class::cast)
            .filter(element -> ElementSupport.isPublic(element)
                && !ElementSupport.isStatic(element)
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
        final Map<String, ExecutableElement> skipped = new LinkedHashMap<>();
        final Map<String, String> reasons = new LinkedHashMap<>();
        transitiveInheritedElements(type).stream()
            .filter(this::isPropertyCandidate)
            .map(ExecutableElement.class::cast)
            .forEach(element -> {
                final String name = element.getSimpleName().toString();
                final Optional<String> reason = skipReason(element);
                if (reason.isPresent()) {
                    skipped.putIfAbsent(name, element);
                    reasons.putIfAbsent(name, reason.get());
                } else if (seenSimpleNames.putIfAbsent(name, true) == null) {
                    getters.add(element);
                }
            });
        logSkippedProperties(type, skipped, reasons, seenSimpleNames);
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
            return PropertyModel.distinctByName(all);
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

    /**
     * A method is a property candidate when its name carries a property prefix. Candidates that do not qualify are
     * reported as skipped properties in verbose mode.
     */
    private boolean isPropertyCandidate(final Element element) {
        if (!(element instanceof ExecutableElement)) {
            return false;
        }
        final String name = element.getSimpleName().toString();
        return name.startsWith("get") || name.startsWith("is");
    }

    /**
     * Returns the reason why a property candidate is no property, or nothing if it is one.
     */
    private Optional<String> skipReason(final ExecutableElement element) {
        final String name = element.getSimpleName().toString();
        final TypeKind returnKind = element.getReturnType().getKind();
        if (!ElementSupport.isPublic(element)) {
            return Optional.of("method is not public");
        }
        if (ElementSupport.isStatic(element)) {
            return Optional.of("method is static");
        }
        if (!hasNoParameters(element)) {
            return Optional.of("method takes parameters");
        }
        if (!hasReturnType(element)) {
            return Optional.of("method returns void");
        }
        if (withoutPropertyPrefix(name).isEmpty()) {
            return Optional.of("no property name follows the '" + propertyPrefix(name) + "' prefix");
        }
        if (name.startsWith("get") && returnKind == TypeKind.BOOLEAN) {
            return Optional.of("a boolean property must use the 'is' prefix");
        }
        if (name.startsWith("is") && returnKind != TypeKind.BOOLEAN) {
            return Optional.of("the 'is' prefix requires a boolean return type");
        }
        return Optional.empty();
    }

    private void logSkippedProperties(
        final TypeElement type,
        final Map<String, ExecutableElement> skipped,
        final Map<String, String> reasons,
        final Map<String, Boolean> acceptedNames
    ) {
        skipped.forEach((name, element) -> {
            // an accessor overridden by an accepted one is not skipped, the property exists
            if (!acceptedNames.containsKey(name)) {
                ElementSupport.logNote(
                    processingEnv,
                    "Skipped property '" + name + "' of " + type.getQualifiedName() + ": " + reasons.get(name),
                    element,
                    null,
                    null
                );
            }
        });
    }

    private static String propertyPrefix(final String name) {
        return name.startsWith("get") ? "get" : "is";
    }

    private String withoutPropertyPrefix(final String name) {
        return decapitalized(name.replaceFirst("^((get)|(is))", ""));
    }
}
