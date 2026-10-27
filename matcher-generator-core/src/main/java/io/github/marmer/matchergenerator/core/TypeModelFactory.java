package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
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

    /**
     * Kotlin getters that Java bean rules skip, such as {@code getEnabled(): boolean}, are only recognised in types
     * that Kotlin compiled. Java types keep the bean rules.
     */
    private static boolean isDeclaredInKotlin(final ExecutableElement element) {
        return ElementSupport.isDeclaredInKotlin(element.getEnclosingElement());
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
            packageNameOf(type),
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
        final String packageName = packageNameOf(type);
        if (configuredPackage.isBlank()) {
            return packageName;
        }
        final String prefix = configuredPackage.replaceAll("\\.+$", "");
        return packageName.isEmpty() ? prefix : prefix + "." + packageName;
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

    /**
     * The qualified name, which is empty for types in the default package (its {@code toString()} is not a name).
     */
    private String packageNameOf(final TypeElement type) {
        return packageOf(type).getQualifiedName().toString();
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
        final Set<String> seenSimpleNames = new HashSet<>();
        final Map<String, SkippedCandidate> skipped = new LinkedHashMap<>();
        transitiveInheritedElements(type).stream()
            .filter(this::isPropertyCandidate)
            .map(ExecutableElement.class::cast)
            .forEach(element -> {
                final String name = element.getSimpleName().toString();
                final Optional<String> reason = skipReason(element);
                if (reason.isPresent()) {
                    skipped.putIfAbsent(name, new SkippedCandidate(element, reason.get()));
                } else if (seenSimpleNames.add(name)) {
                    getters.add(element);
                }
            });
        logSkippedProperties(type, skipped, seenSimpleNames);
        final List<PropertyModel> getterProperties = getters.stream()
            .map(element -> new PropertyModel(
                propertyName(element),
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
     * Returns the reason why a property candidate is no property, or nothing if it is one. A boolean {@code get}
     * getter and a non boolean {@code is} getter only qualify in types Kotlin compiled.
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
        if (!SourceVersion.isIdentifier(element.getSimpleName())) {
            return Optional.of("method name is not a Java identifier");
        }
        if (withoutPropertyPrefix(name).isEmpty()) {
            return Optional.of("no property name follows the '" + propertyPrefix(name) + "' prefix");
        }
        final boolean kotlin = isDeclaredInKotlin(element);
        if (kotlin && !Character.isUpperCase(withoutPrefix(name).charAt(0))) {
            return Optional.of("in Kotlin, an upper case letter must follow the '" + propertyPrefix(name)
                + "' prefix");
        }
        if (name.startsWith("get") && returnKind == TypeKind.BOOLEAN && !kotlin) {
            return Optional.of("a boolean property must use the 'is' prefix");
        }
        if (name.startsWith("is") && returnKind != TypeKind.BOOLEAN && !kotlin) {
            return Optional.of("the 'is' prefix requires a boolean return type");
        }
        return Optional.empty();
    }

    private void logSkippedProperties(
        final TypeElement type,
        final Map<String, SkippedCandidate> skipped,
        final Set<String> acceptedNames
    ) {
        skipped.forEach((name, candidate) -> {
            // an accessor overridden by an accepted one is not skipped, the property exists
            if (!acceptedNames.contains(name)) {
                ElementSupport.logNote(
                    processingEnv,
                    "Skipped property '" + name + "' of " + type.getQualifiedName() + ": " + candidate.reason(),
                    candidate.element()
                );
            }
        });
    }

    /**
     * A method that looks like a property accessor but is none, with the reason why.
     */
    private record SkippedCandidate(ExecutableElement element, String reason) {
    }

    private static String propertyPrefix(final String name) {
        return name.startsWith("get") ? "get" : "is";
    }

    /**
     * A Kotlin {@code is} getter that does not return a primitive boolean keeps its full name, so {@code val isX:
     * Boolean?} becomes {@code isX}, the name Kotlin gives the property.
     */
    private String propertyName(final ExecutableElement element) {
        final String name = element.getSimpleName().toString();
        if (name.startsWith("is") && element.getReturnType().getKind() != TypeKind.BOOLEAN) {
            return decapitalized(name);
        }
        return withoutPropertyPrefix(name);
    }

    private String withoutPropertyPrefix(final String name) {
        return decapitalized(withoutPrefix(name));
    }

    private static String withoutPrefix(final String name) {
        return name.replaceFirst("^((get)|(is))", "");
    }
}
