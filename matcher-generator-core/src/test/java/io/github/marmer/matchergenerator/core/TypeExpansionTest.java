/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The expansion of configured names without a javac round and without the worker: {@link Elements}
 * is a fake that knows the types and packages put into it, and the elements are stubs.
 */
class TypeExpansionTest {

    private final FakeElements elements = new FakeElements();

    @Test
    @DisplayName("An excluded type is not planned, the others of its package are")
    void anExcludedTypeIsNotPlanned() {
        final TypeElement kept = elements.addType("p", "Kept");
        elements.addType("p", "Dropped");

        final ConfiguredNames names = namesOf(List.of("p"), List.of("p.Dropped"));

        assertEquals(Map.of(kept, Set.of(kept)), TypeExpansion.typesToPlan(names, names.names()));
    }

    @Test
    @DisplayName("An exclusion that names a public member type is matched by the nesting type")
    void aPublicMemberTypeIsModelled() {
        final TypeElement outer = elements.addType("p", "Outer");
        elements.addNestedType(outer, "Inner", Set.of(Modifier.PUBLIC));

        final ConfiguredNames names = namesOf(List.of("p.Outer"), List.of("p.Outer.Inner"));

        assertEquals(Map.of(), TypeExpansion.unmatchedExclusions(names));
    }

    @Test
    @DisplayName("An exclusion that names a member type of a public member type is matched")
    void aPublicMemberTypeOfAPublicMemberTypeIsModelled() {
        final TypeElement outer = elements.addType("p", "Outer");
        final TypeElement inner = elements.addNestedType(outer, "Inner", Set.of(Modifier.PUBLIC));
        elements.addNestedType(inner, "Deep", Set.of(Modifier.PUBLIC));

        final ConfiguredNames names = namesOf(List.of("p.Outer"), List.of("p.Outer.Inner.Deep"));

        assertEquals(Map.of(), TypeExpansion.unmatchedExclusions(names));
    }

    @Test
    @DisplayName("An exclusion that names a non public member type is unmatched")
    void aNonPublicMemberTypeIsNotModelled() {
        final TypeElement outer = elements.addType("p", "Outer");
        elements.addNestedType(outer, "Hidden", Set.of());

        final ConfiguredNames names = namesOf(List.of("p.Outer"), List.of("p.Outer.Hidden"));

        assertEquals(Set.of("p.Outer.Hidden"), TypeExpansion.unmatchedExclusions(names).keySet());
    }

    @Test
    @DisplayName("A Kotlin file facade is no user type")
    void aKotlinFileFacadeIsNoUserType() {
        assertFalse(TypeExpansion.isUserType(elements.addKotlinType("p", "UtilKt", 2)));
    }

    @Test
    @DisplayName("A Kotlin class and a Java class are user types")
    void aKotlinClassAndAJavaClassAreUserTypes() {
        assertTrue(TypeExpansion.isUserType(elements.addKotlinType("p", "Data", 1)));
        assertTrue(TypeExpansion.isUserType(elements.addType("p", "Plain")));
    }

    private ConfiguredNames namesOf(final List<String> values, final List<String> exclusions) {
        return ConfiguredNames.of(elements.elements(), elements.annotation(values, exclusions));
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(
            final Class<T> type,
            final String description,
            final Map<String, Function<Object[], Object>> answers) {
        final InvocationHandler handler =
                (proxy, method, args) ->
                        switch (method.getName()) {
                            case "toString" -> description;
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> {
                                final Function<Object[], Object> answer =
                                        answers.get(method.getName());
                                if (answer == null) {
                                    throw new UnsupportedOperationException(
                                            description + "." + method.getName());
                                }
                                yield answer.apply(args);
                            }
                        };
        return (T)
                Proxy.newProxyInstance(
                        TypeExpansionTest.class.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static Name nameOf(final String text) {
        return stub(
                Name.class,
                text,
                Map.of(
                        "contentEquals",
                        args -> text.contentEquals((CharSequence) args[0]),
                        "toString",
                        args -> text));
    }

    private static AnnotationValue valueOf(final Object content) {
        return stub(AnnotationValue.class, "value " + content, Map.of("getValue", args -> content));
    }

    /** The Elements the rules use, over the types and packages put into it. */
    private static final class FakeElements {
        private final Map<String, TypeElement> typesByName = new LinkedHashMap<>();
        private final Map<String, List<TypeElement>> typesByPackage = new LinkedHashMap<>();
        private final Map<String, PackageElement> packages = new LinkedHashMap<>();
        private final Map<TypeElement, PackageElement> packageOfType = new LinkedHashMap<>();
        private final Map<TypeElement, List<Element>> members = new LinkedHashMap<>();
        private final ExecutableElement valueAttribute = attribute("value");
        private final ExecutableElement excludeAttribute = attribute("exclude");
        private final Map<AnnotationMirror, Map<ExecutableElement, AnnotationValue>> attributes =
                new LinkedHashMap<>();
        private final Elements elements =
                stub(
                        Elements.class,
                        "elements",
                        Map.of(
                                "getTypeElement",
                                args -> typesByName.get(args[0].toString()),
                                "getAllPackageElements",
                                args ->
                                        packages.containsKey(args[0].toString())
                                                ? Set.of(packages.get(args[0].toString()))
                                                : Set.of(),
                                "getPackageOf",
                                args -> packageOfType.get((TypeElement) args[0]),
                                "getElementValuesWithDefaults",
                                args -> attributes.get((AnnotationMirror) args[0])));

        private static ExecutableElement attribute(final String name) {
            return stub(
                    ExecutableElement.class,
                    "attribute " + name,
                    Map.of("getSimpleName", args -> nameOf(name)));
        }

        Elements elements() {
            return elements;
        }

        TypeElement addType(final String packageName, final String simpleName) {
            return addType(packageName, simpleName, List.of());
        }

        TypeElement addKotlinType(
                final String packageName, final String simpleName, final int kind) {
            final ExecutableElement kindAttribute = attribute("k");
            final AnnotationMirror metadata =
                    stub(
                            AnnotationMirror.class,
                            "kotlin.Metadata",
                            Map.of(
                                    "getAnnotationType",
                                    args ->
                                            stub(
                                                    DeclaredType.class,
                                                    "kotlin.Metadata type",
                                                    Map.of(
                                                            "asElement",
                                                            args2 ->
                                                                    stub(
                                                                            Element.class,
                                                                            "kotlin.Metadata",
                                                                            Map.of()))),
                                    "getElementValues",
                                    args -> Map.of(kindAttribute, valueOf(kind))));
            return addType(packageName, simpleName, List.of(metadata));
        }

        private TypeElement addType(
                final String packageName,
                final String simpleName,
                final List<AnnotationMirror> annotations) {
            final PackageElement packageElement =
                    packages.computeIfAbsent(packageName, this::packageElement);
            final String qualifiedName = packageName + "." + simpleName;
            final TypeElement type =
                    typeElement(
                            qualifiedName, packageElement, Set.of(Modifier.PUBLIC), annotations);
            typesByName.put(qualifiedName, type);
            typesByPackage.computeIfAbsent(packageName, key -> new ArrayList<>()).add(type);
            packageOfType.put(type, packageElement);
            return type;
        }

        TypeElement addNestedType(
                final TypeElement outer, final String simpleName, final Set<Modifier> modifiers) {
            final String qualifiedName = outer.getQualifiedName() + "." + simpleName;
            final TypeElement type = typeElement(qualifiedName, outer, modifiers, List.of());
            typesByName.put(qualifiedName, type);
            packageOfType.put(type, packageOfType.get(outer));
            members.computeIfAbsent(outer, key -> new ArrayList<>()).add(type);
            return type;
        }

        AnnotationMirror annotation(final List<String> values, final List<String> exclusions) {
            final AnnotationMirror mirror = stub(AnnotationMirror.class, "mirror", Map.of());
            attributes.put(
                    mirror,
                    Map.of(
                            valueAttribute, listValue(values),
                            excludeAttribute, listValue(exclusions)));
            return mirror;
        }

        private static AnnotationValue listValue(final List<String> names) {
            return valueOf(names.stream().map(TypeExpansionTest::valueOf).toList());
        }

        private PackageElement packageElement(final String name) {
            return stub(
                    PackageElement.class,
                    "package " + name,
                    Map.of(
                            "getQualifiedName",
                            args -> nameOf(name),
                            "getEnclosedElements",
                            args -> List.copyOf(typesByPackage.getOrDefault(name, List.of()))));
        }

        private TypeElement typeElement(
                final String qualifiedName,
                final Element parent,
                final Set<Modifier> modifiers,
                final List<AnnotationMirror> annotations) {
            return stub(
                    TypeElement.class,
                    "type " + qualifiedName,
                    Map.of(
                            "getQualifiedName",
                            args -> nameOf(qualifiedName),
                            "getEnclosingElement",
                            args -> parent,
                            "getModifiers",
                            args -> modifiers,
                            "getAnnotationMirrors",
                            args -> annotations,
                            "getEnclosedElements",
                            args ->
                                    List.copyOf(
                                            members.getOrDefault(
                                                    typesByName.get(qualifiedName), List.of()))));
        }
    }
}
