package io.github.marmer.annotationprocessing.extraction;

import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.core.Logger;
import io.github.marmer.annotationprocessing.core.impl.StringUtils;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.PropertyDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;
import io.github.marmer.testutils.generators.beanmatcher.dependencies.BasedOn;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.collectingAndThen;

/**
 * Factory to create some matcher descriptions.
 */
public class MatcherBaseDescriptorFactory {
    private static final String PRIMITIVE_BOOLEAN_PROPERTY_METHOD_PREFIX = "is";
    private static final String ANY_PROPERTY_METHOD_PREFIX = "get";
    private final ProcessingEnvironment processingEnv;
    private final Logger logger;

    public MatcherBaseDescriptorFactory(final ProcessingEnvironment processingEnv, final Logger logger) {
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
        final Map<Boolean, List<TypeElement>> matcherBaseDescriptorStream = Stream.of(configuration.value())
                .flatMap(this::toTypeElements)
                .filter(this::isNotSelfGenerated)
                .map(this::toTopLevelContainerType)
                .filter(this::isNotSelfGenerated)
                .collect(Collectors.partitioningBy(this::isPublic));

        matcherBaseDescriptorStream.get(false)
                .forEach(this::logInfoNotPublic);

        return matcherBaseDescriptorStream.get(true).stream()
                .map(type -> toTypeDescriptor(type, configuration));
    }

    private boolean isNotSelfGenerated(final Element element) {
        return element.getAnnotation(BasedOn.class) == null;
    }

    private void logInfoNotPublic(final Element element) {
        logger.info("Processing skipped for non public type: " + element, element);
    }

    private TypeElement toTopLevelContainerType(final TypeElement typeElement) {
        return parentsOf(typeElement).stream().findFirst().orElse(typeElement);
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

    private MatcherBaseDescriptor
    toTypeDescriptor(final TypeElement type, final MatcherConfiguration configuration, final TypeElement... outerTypes) {
        return MatcherBaseDescriptor.builder()
                .base(TypeDescriptor.builder()
                        .packageName(extractPackageName(type.asType()))
                        .typeName(extractTypename(type.asType()))
                        .parentNames(parentNamesOf(type))
                        .fullQualifiedName(type.getQualifiedName().toString())
                        .primitive(isPrimitive(type.asType()))
                        .build())
                .properties(propertiesFor(type).collect(Collectors.toList()))
                .innerMatchers(innerMatchersFor(type, configuration, outerTypes))
                .matcherConfiguration(configuration)
                .build();
    }

    private List<MatcherBaseDescriptor> innerMatchersFor(final TypeElement type, final MatcherConfiguration configuration, final TypeElement... outerTypes) {
        final Map<Boolean, ? extends List<? extends Element>> matcherBaseDescriptorStream = type.getEnclosedElements()
                .stream()
                .filter(this::isType)
                .collect(Collectors.partitioningBy(this::isPublic));

        matcherBaseDescriptorStream.get(false)
                .forEach(this::logInfoNotPublic);

        return matcherBaseDescriptorStream.get(true).stream()
                .map(innerType -> toTypeDescriptor((TypeElement) innerType, configuration, asArray(outerTypes, type)))
                .collect(Collectors.toList());
    }

    private boolean isType(final Element element) {
        final ElementKind kind = element.getKind();
        return kind.isClass() || kind.isInterface();
    }

    private TypeElement[] asArray(final TypeElement[] outerTypes, final TypeElement type) {
        return Stream.concat(Stream.of(outerTypes), Stream.of(type)).toArray(TypeElement[]::new);
    }

    private Stream<PropertyDescriptor> propertiesFor(final TypeElement type) {
        if (type == null) {
            return Stream.empty();
        }
        final Stream<PropertyDescriptor> propertyDescriptorStream = type.getEnclosedElements().stream()
                .filter(this::isPropertyMethod)
                .map(e -> (ExecutableElement) e)
                .map(this::toPropertyDescriptor);
        final Stream<PropertyDescriptor> stream = propertiesFor(getSupertype(type));
        final Stream<PropertyDescriptor> listStream = getSuperInterfacesFor(type).flatMap(this::propertiesFor);
        return Stream.of(
                propertyDescriptorStream,
                listStream,
                stream)
                .flatMap(identity())
                .collect(distinctByPropertyName());
    }

    private Collector<PropertyDescriptor, Collection<PropertyDescriptor>, Stream<PropertyDescriptor>> distinctByPropertyName() {
        return collectingAndThen(Collector.of(ArrayList::new,
                (propertyDescriptors, propDesc) -> {
                    //contains by property name
                    if (propertyDescriptors.stream()
                            .map(PropertyDescriptor::getProperty)
                            .noneMatch(property -> Objects.equals(property, propDesc.getProperty()))) {
                        propertyDescriptors.add(propDesc);
                    }
                },
                ListUtil::joinToList),
                Collection::stream);

    }

    private Stream<TypeElement> getSuperInterfacesFor(final TypeElement type) {
        return type.getInterfaces().stream().map((Function<TypeMirror, TypeElement>) this::toTypeElement);
    }

    private TypeElement toTypeElement(final TypeMirror i) {
        return processingEnv.getElementUtils().getTypeElement(i.toString().replaceAll("<.+>", ""));
    }

    private TypeElement getSupertype(final TypeElement type) {
        final TypeMirror superclass = type.getSuperclass();
        return TypeKind.NONE.equals(superclass.getKind()) ?
                null :
                toTypeElement(superclass);
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
                        .fullQualifiedName(fullQualifiedNameOf(returnType))
                        .parentNames(parentNamesOf(returnType))
                        .primitive(isPrimitive(returnType))
                        .build())
                .build();
    }

    private String fullQualifiedNameOf(final TypeMirror type) {
        return isTypeVar(type) ?
                "java.lang.Object" :
                type.toString();
    }

    private List<String> parentNamesOf(final TypeMirror type) {
        return isTypeVar(type) ?
                Collections.emptyList() :
                parentNamesOf(processingEnv.getTypeUtils().asElement(type));
    }

    private List<String> parentNamesOf(final Element element) {
        return parentsOf(element).stream()
                .map(Element::getSimpleName)
                .map(Objects::toString)
                .collect(Collectors.toList());
    }

    private List<TypeElement> parentsOf(final Element element) {
        final List<TypeElement> result = new ArrayList<>();

        if (element != null) {
            final Element enclosingElement = element.getEnclosingElement();
            if (isType(enclosingElement)) {
                result.addAll(parentsOf(enclosingElement));
                result.add((TypeElement) enclosingElement);
            }
        }
        return result;
    }

    private String extractPackageName(final TypeMirror type) {
        if (type instanceof ArrayType) {
            return extractPackageName(((ArrayType) type)
                    .getComponentType());
        } else if (isPrimitive(type)) {
            return null;
        } else if (isTypeVar(type)) {
            return "java.lang";
        } else {
            return processingEnv.getElementUtils().getPackageOf(processingEnv.getTypeUtils().asElement(type)).toString();
        }
    }

    private boolean isTypeVar(final TypeMirror type) {
        return TypeKind.TYPEVAR.equals(type.getKind());
    }

    private String extractTypename(final TypeMirror type) {
        if (type instanceof ArrayType) {
            return type.toString().replaceFirst(extractPackageName(type) + ".", "");
        } else if (isPrimitive(type)) {
            return type.toString();
        } else if (isTypeVar(type)) {
            return "Object";
        } else {
            return simpleNameOf(processingEnv.getTypeUtils().asElement(type));
        }
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
