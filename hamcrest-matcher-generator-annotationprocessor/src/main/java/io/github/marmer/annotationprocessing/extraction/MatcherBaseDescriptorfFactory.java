package io.github.marmer.annotationprocessing.extraction;

import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.MatcherConfigurations;
import io.github.marmer.annotationprocessing.core.impl.StringUtils;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.PropertyDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Factory to create some matcher descriptions.
 */
public class MatcherBaseDescriptorfFactory {
    private static final String PRIMITIVE_BOOLEAN_PROPERTY_METHOD_PREFIX = "is";
    private static final String ANY_PROPERTY_METHOD_PREFIX = "get";

    /**
     * Creates Matcher descriptions.
     *
     * @param configurationsWrapper Configuration for what to create {@link MatcherBaseDescriptor}s for.
     * @param processingEnv         environment used to find details based on the configurations.
     * @return Resulting {@link MatcherBaseDescriptor}s based on the configurations.
     */
    public Set<MatcherBaseDescriptor> create(final MatcherConfigurations configurationsWrapper, final ProcessingEnvironment processingEnv) {
        return Stream.of(configurationsWrapper.value())
                .map(matcherConfiguration -> create(matcherConfiguration, processingEnv))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }


    /**
     * Creates Matcher descriptions.
     *
     * @param configuration Configuration for what to create {@link MatcherBaseDescriptor}s for.
     * @param processingEnv environment used to find details based on the configurations.
     * @return Resulting {@link MatcherBaseDescriptor}s based on the configurations.
     */
    public Set<MatcherBaseDescriptor> create(final MatcherConfiguration configuration, final ProcessingEnvironment processingEnv) {
        // TODO: marmer 01.02.2019 Type does not exist -> warn
        return Stream.of(configuration.value())
                .map(processingEnv.getElementUtils()::getTypeElement)
                .map(typeElement -> typeDescriptorFor(processingEnv, typeElement))
                .collect(Collectors.toSet());
    }

    private MatcherBaseDescriptor typeDescriptorFor(final ProcessingEnvironment processingEnv, final TypeElement type) {
        return MatcherBaseDescriptor.builder()
                .base(TypeDescriptor.builder()
                        .packageName(processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString())
                        .typeName(simpleNameOf(type))
                        .fullQualifiedName(type.getQualifiedName().toString())
                        .primitive(false).build())
                .properties(propertiesFor(processingEnv, type)).build();
    }

    private List<PropertyDescriptor> propertiesFor(final ProcessingEnvironment processingEnv, final TypeElement type) {
        return type.getEnclosedElements().stream()
                .filter(this::isPropertyMethod)
                .map(e -> (ExecutableElement) e)
                .map(element -> toPropertyDescriptor(processingEnv, element))
                .collect(Collectors.toList());
    }

    private boolean isPropertyMethod(final Element element) {
        if (!isMethod(element)) {
            return false;
        }
        if (hasVoidReturnType((ExecutableElement) element) || hasParameters((ExecutableElement) element)) {
            return false;
        }

        return (hasAnyPropertyMethodName(element) &&
                !hasPrimitiveBooleanReturnType((ExecutableElement) element))
                ||
                (hasPrimitiveBooleanReturnType((ExecutableElement) element) &&
                        hasPrimitiveBooleanPropertyMethodName(element));
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
        return element.getKind().equals(ElementKind.METHOD);
    }

    private PropertyDescriptor toPropertyDescriptor(final ProcessingEnvironment processingEnv, final ExecutableElement element) {
        final TypeMirror returnType = element.getReturnType();

        return PropertyDescriptor.builder()
                .property(toPropertyName(element))
                .returnValue(TypeDescriptor.builder()
                        .packageName(extractPackageName(processingEnv, returnType))
                        .typeName(extractTypename(processingEnv, returnType))
                        .fullQualifiedName(returnType.toString())
                        .primitive(isPrimitive(returnType))
                        .build())
                .build();
    }

    private String extractPackageName(final ProcessingEnvironment processingEnv, final TypeMirror returnType) {
        return isPrimitive(returnType) ? null : processingEnv.getElementUtils().getPackageOf(processingEnv.getTypeUtils().asElement(returnType)).toString();
    }

    private String extractTypename(final ProcessingEnvironment processingEnv, final TypeMirror returnType) {
        return isPrimitive(returnType) ? returnType.toString() : simpleNameOf(processingEnv.getTypeUtils().asElement(returnType));
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
        return returnType.getKind().isPrimitive() && "boolean".equals(returnType.toString());
    }

    // TODO: marmer 15.02.2019 create another module which uses some of those generated matchers to know whether they really work (or even exist^^)

}
