package io.github.marmer.annotationprocessing.extraction;

import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.core.Logger;
import io.github.marmer.annotationprocessing.core.impl.StringUtils;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.PropertyDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Factory to create some matcher descriptions.
 */
public class MatcherBaseDescriptorfFactory {
    private static final String PRIMITIVE_BOOLEAN_PROPERTY_METHOD_PREFIX = "is";
    private static final String ANY_PROPERTY_METHOD_PREFIX = "get";
    private final ProcessingEnvironment processingEnv;
    private final Logger logger;

    public MatcherBaseDescriptorfFactory(final ProcessingEnvironment processingEnv, final Logger logger) {
        this.processingEnv = processingEnv;
        this.logger = logger;
    }

    /**
     * Creates Matcher descriptions.
     *
     * @param configuration Configuration for what to create {@link MatcherBaseDescriptor}s for.
     * @return Resulting {@link MatcherBaseDescriptor}s based on the configurations.
     */
    public Stream<MatcherBaseDescriptor> create(final MatcherConfiguration configuration) {
        return Stream.of(configuration.value())
                .flatMap(this::toTypeElements)
                .filter(this::isPublic)
                .map(this::toTypeDescriptor);
    }

    private Stream<TypeElement> toTypeElements(final String name) {
        final TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(name);
        if (typeElement != null) {
            return Stream.of(typeElement);
        }
        final PackageElement packageElement = processingEnv.getElementUtils().getPackageElement(name);
        if (packageElement != null) {
            final List<? extends Element> enclosedElements = packageElement.getEnclosedElements();
            return enclosedElements.stream().map(element -> processingEnv.getElementUtils().getTypeElement(element.toString()));
        }
        logger.error("Package or type does not exist: " + name);
        return Stream.empty();
    }

    private boolean isPublic(final Element typeElement) {
        return typeElement.getModifiers().contains(Modifier.PUBLIC);
    }

    private MatcherBaseDescriptor toTypeDescriptor(final TypeElement type, final TypeElement... outerTypes) {
        return MatcherBaseDescriptor.builder()
                .base(TypeDescriptor.builder()
                        .packageName(extractPackageName(type.asType()))
                        .typeName(extractTypename(type.asType()))
                        .parentNames(Stream.of(outerTypes)
                                .map(typeElement -> extractTypename(typeElement.asType()))
                                .collect(Collectors.toList()))
                        .fullQualifiedName(type.getQualifiedName().toString())
                        .primitive(isPrimitive(type.asType()))
                        .build())
                .properties(propertiesFor(type))
                .innerMatchers(innerMatchersFor(type, outerTypes))
                .build();
    }

    private List<MatcherBaseDescriptor> innerMatchersFor(final TypeElement type, final TypeElement... outerTypes) {
        return type.getEnclosedElements()
                .stream()
                .filter(this::isType)
                .filter(this::isPublic)
                .map(innerType -> toTypeDescriptor((TypeElement) innerType, asArray(outerTypes, type)))
                .collect(Collectors.toList());
    }

    private boolean isType(final Element element) {
        final ElementKind kind = element.getKind();
        return kind.isClass() || kind.isInterface();
    }

    private TypeElement[] asArray(final TypeElement[] outerTypes, final TypeElement type) {
        return Stream.concat(Stream.of(outerTypes), Stream.of(type)).toArray(TypeElement[]::new);
    }

    private List<PropertyDescriptor> propertiesFor(final TypeElement type) {
        if (type == null) {
            return Collections.emptyList();
        }
        return Stream.concat(
                type.getEnclosedElements().stream()
                        .filter(this::isPropertyMethod)
                        .map(e -> (ExecutableElement) e)
                        .map(this::toPropertyDescriptor),
                propertiesFor(getSupertype(type)).stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private TypeElement getSupertype(final TypeElement type) {
        final TypeMirror superclass = type.getSuperclass();
        return TypeKind.NONE.equals(superclass.getKind()) ?
                null :
                processingEnv.getElementUtils().getTypeElement(superclass.toString());
    }

    private boolean isPropertyMethod(final Element element) {
        if (!isMethod(element) || !isPublic(element)) {
            return false;
        }
        if (hasVoidReturnType((ExecutableElement) element) ||
                hasParameters((ExecutableElement) element) ||
                isStatic(element)) {
            return false;
        }

        return (hasAnyPropertyMethodName(element) &&
                !hasPrimitiveBooleanReturnType((ExecutableElement) element))
                ||
                (hasPrimitiveBooleanReturnType((ExecutableElement) element) &&
                        hasPrimitiveBooleanPropertyMethodName(element));
    }

    private boolean isStatic(final Element element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    private boolean hasParameters(final ExecutableElement element) {
        return !element.getParameters().isEmpty();
    }

    private boolean hasVoidReturnType(final ExecutableElement element) {
        return TypeKind.VOID.equals(element.getReturnType().getKind());
    }

    private boolean hasAnyPropertyMethodName(final Element element) {
        return simpleNameOf(element).startsWith(ANY_PROPERTY_METHOD_PREFIX);
    }

    private String simpleNameOf(final Element element) {
        return element.getSimpleName().toString();
    }

    private boolean hasPrimitiveBooleanPropertyMethodName(final Element element) {
        return simpleNameOf(element).startsWith(PRIMITIVE_BOOLEAN_PROPERTY_METHOD_PREFIX);
    }

    private boolean hasPrimitiveBooleanReturnType(final ExecutableElement element) {
        return element.getReturnType().getKind().isPrimitive() && "boolean".equals(element.getReturnType().toString());
    }

    private boolean isMethod(final Element element) {
        return ElementKind.METHOD.equals(element.getKind());
    }

    private PropertyDescriptor toPropertyDescriptor(final ExecutableElement element) {
        final TypeMirror returnType = element.getReturnType();

        return PropertyDescriptor.builder()
                .property(toPropertyName(element))
                .returnValue(TypeDescriptor.builder()
                        .packageName(extractPackageName(returnType))
                        .typeName(extractTypename(returnType))
                        .fullQualifiedName(returnType.toString())
                        .parentNames(parentNamesOf(returnType))
                        .primitive(isPrimitive(returnType))
                        .build())
                .build();
    }

    private List<String> parentNamesOf(final TypeMirror type) {
        return parentNamesOf(processingEnv.getTypeUtils().asElement(type));
    }

    private List<String> parentNamesOf(final Element element) {
        final List<String> result = new ArrayList<>();

        if (element != null) {
            final Element enclosingElement = element.getEnclosingElement();
            if (isType(enclosingElement)) {
                result.addAll(parentNamesOf(enclosingElement));
                result.add(enclosingElement.getSimpleName().toString());
            }
        }
        return result;
    }

    private String extractPackageName(final TypeMirror type) {
        if (type instanceof ArrayType) {
            return extractPackageName(((ArrayType) type)
                    .getComponentType());
        }
        return isPrimitive(type) ? null : processingEnv.getElementUtils().getPackageOf(processingEnv.getTypeUtils().asElement(type)).toString();
    }

    private String extractTypename(final TypeMirror type) {
        if (type instanceof ArrayType) {
            return type.toString().replaceFirst(extractPackageName(type) + ".", "");
        }
        return isPrimitive(type) ? type.toString() : simpleNameOf(processingEnv.getTypeUtils().asElement(type));
    }

    private boolean isPrimitive(final TypeMirror returnType) {
        return returnType.getKind().isPrimitive();
    }

    private String toPropertyName(final ExecutableElement element) {
        final String capitalizedPropertyName = simpleNameOf(element).replaceFirst(
                isPrimitiveBoolean(element.getReturnType()) ?
                        PRIMITIVE_BOOLEAN_PROPERTY_METHOD_PREFIX :
                        ANY_PROPERTY_METHOD_PREFIX, "");
        return StringUtils.uncapitalize(capitalizedPropertyName);
    }

    private boolean isPrimitiveBoolean(final TypeMirror returnType) {
        return "boolean".equals(returnType.toString());
    }
}
