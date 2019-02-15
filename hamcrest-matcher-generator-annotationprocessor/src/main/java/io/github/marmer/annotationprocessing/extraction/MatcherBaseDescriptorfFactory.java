package io.github.marmer.annotationprocessing.extraction;

import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.MatcherConfigurations;
import io.github.marmer.annotationprocessing.core.impl.StringUtils;
import io.github.marmer.annotationprocessing.core.model.MatcherBaseDescriptor;
import io.github.marmer.annotationprocessing.core.model.PropertyDescriptor;
import io.github.marmer.annotationprocessing.core.model.TypeDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Factory to create some matcher descriptions.
 */
public class MatcherBaseDescriptorfFactory {
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
                .map(t -> processingEnv.getElementUtils().getTypeElement(t))
                .map(typeElement -> typeDescriptorFor(processingEnv, typeElement))
                .collect(Collectors.toSet());
    }

    private MatcherBaseDescriptor typeDescriptorFor(final ProcessingEnvironment processingEnv, final TypeElement type) {
        return MatcherBaseDescriptor.builder()
                .base(TypeDescriptor.builder()
                        .packageName(processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString())
                        .typeName(type.getSimpleName().toString())
                        .fullQualifiedName(type.getQualifiedName().toString())
                        .primitive(false).build())
                .properties(propertiesFor(processingEnv, type)).build();
    }

    private List<PropertyDescriptor> propertiesFor(final ProcessingEnvironment processingEnv, final TypeElement type) {
        return type.getEnclosedElements().stream()
                .filter(enclosedElement -> enclosedElement.getKind().equals(ElementKind.METHOD))
                // TODO: marmer 15.02.2019 no non property methods
                // TODO: marmer 15.02.2019 no non property methods with parameters (so no unreal "property methods")
                // TODO: marmer 15.02.2019 no void property methods (so no unreal "property methods")
                .map(e -> (ExecutableElement) e)
                .map(element -> toPropertyDescriptor(processingEnv, element))
                .collect(Collectors.toList());
    }

    private PropertyDescriptor toPropertyDescriptor(final ProcessingEnvironment processingEnv, final ExecutableElement element) {
        final TypeMirror returnType = element.getReturnType();

        final String packageName = isPrimitive(returnType) ? null : processingEnv.getElementUtils().getPackageOf(processingEnv.getTypeUtils().asElement(returnType)).toString();
        final String typeName = isPrimitive(returnType) ? returnType.toString() : processingEnv.getTypeUtils().asElement(returnType).getSimpleName().toString();
        final String fullQualifiedName = returnType.toString();


        return PropertyDescriptor.builder()
                .property(toPropertyName(element))
                .returnValue(TypeDescriptor.builder()
                        .packageName(packageName)
                        .typeName(typeName)
                        .fullQualifiedName(fullQualifiedName)
                        .primitive(isPrimitive(returnType))
                        .build())
                .build();
    }

    private boolean isPrimitive(final TypeMirror returnType) {
        return returnType.getKind().isPrimitive();
    }

    private String toPropertyName(final ExecutableElement element) {
        final Name simpleName = element.getSimpleName();

        final String capitalizedPropertyName = simpleName.toString().replaceFirst(
                isPrimitiveBoolean(element.getReturnType()) ?
                        "is" :
                        "get", "");
        return StringUtils.uncapitalize(capitalizedPropertyName);
    }

    private boolean isPrimitiveBoolean(final TypeMirror returnType) {
        return returnType.getKind().isPrimitive() && "boolean".equals(returnType.toString());
    }

    // TODO: marmer 15.02.2019 create another module which uses some of those generated matchers to know whether they really work (or even exist^^)

}
