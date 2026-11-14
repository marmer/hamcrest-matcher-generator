/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The configured names without a javac round: {@link Elements}, its only seam, is a fake that knows
 * the types and packages put into it, and the elements are stubs that answer what the rules ask.
 */
class ConfiguredNamesTest {

    private final FakeElements elements = new FakeElements();

    @Test
    @DisplayName("A name that is a type matches, keyed by the name as configured")
    void aNameThatIsATypeMatchesKeyedByTheNameAsConfigured() {
        final TypeElement foo = elements.addType("p", "Foo");
        final AnnotationValue value = value("p.Foo");

        final ConfiguredNames names = namesOf(List.of(value), List.of());

        assertEquals(Set.of(foo), names.typesNamed("p.Foo").keySet());
        assertEquals(List.of(), names.namesMatchingNothing());
        assertEquals(Map.of("p.Foo", value), names.entriesMatchingTypeOrPackage(names.names()));
    }

    @Test
    @DisplayName("A name that is a nested type stands for its highest nesting type")
    void aNameThatIsANestedTypeStandsForItsHighestNestingType() {
        final TypeElement outer = elements.addType("p", "Outer");
        final TypeElement inner = elements.addNestedType(outer, "Inner");

        final ConfiguredNames names = namesOf(List.of(value("p.Outer.Inner")), List.of());

        assertEquals(Map.of(outer, Set.of(inner)), names.typesNamed("p.Outer.Inner"));
    }

    @Test
    @DisplayName("A name that is a package matches, but names no type")
    void aNameThatIsAPackageMatchesButNamesNoType() {
        final TypeElement foo = elements.addType("p", "Foo");
        final TypeElement bar = elements.addType("p", "Bar");
        final AnnotationValue value = value("p");

        final ConfiguredNames names = namesOf(List.of(value), List.of());

        assertEquals(Set.of(foo, bar), names.typesNamed("p").keySet());
        assertEquals(List.of(), names.namesMatchingNothing());
        assertEquals(Map.of("p", value), names.entriesMatchingTypeOrPackage(names.names()));
        assertEquals(Map.of(), names.entriesNamingTypes(names.names()));
    }

    @Test
    @DisplayName("A name that matches neither a type nor a package matches nothing")
    void aNameThatMatchesNeitherATypeNorAPackageMatchesNothing() {
        elements.addType("p", "Foo");

        final ConfiguredNames names =
                namesOf(List.of(value("p.Foo"), value("q.Missing")), List.of());

        assertEquals(List.of("q.Missing"), names.namesMatchingNothing());
        assertEquals(List.of("q.Missing"), names.namesMatchingNothing(List.of("q.Missing")));
        assertEquals(Set.of("p.Foo"), names.entriesMatchingTypeOrPackage(names.names()).keySet());
        assertEquals(Map.of(), names.typesNamed("q.Missing"));
    }

    @Test
    @DisplayName("Names of one type are one entry by the canonical name, the first one")
    void namesOfOneTypeAreOneEntryByTheCanonicalNameTheFirstOne() {
        final TypeElement foo = elements.addType("p", "Foo");
        elements.addAlias("p.Alias", foo);
        final AnnotationValue first = value("p.Foo");
        final AnnotationValue second = value("p.Alias");

        final ConfiguredNames names = namesOf(List.of(first, second), List.of());

        final Map<String, AnnotationValue> entries = names.entriesNamingTypes(names.names());
        assertEquals(List.of("p.Foo"), List.copyOf(entries.keySet()));
        assertSame(first, entries.get("p.Foo"));
    }

    @Test
    @DisplayName("A name configured twice is one entry, with the value that named it first")
    void aNameConfiguredTwiceIsOneEntryWithTheValueThatNamedItFirst() {
        elements.addType("p", "Foo");
        final AnnotationValue first = value("p.Foo");
        final AnnotationValue second = value("p.Foo");

        final ConfiguredNames names = namesOf(List.of(first, second), List.of());

        assertEquals(List.of("p.Foo"), List.copyOf(names.names()));
        assertSame(first, names.entriesOf(List.of("p.Foo")).get("p.Foo"));
    }

    @Test
    @DisplayName("An exclusion entry excludes a type by its name or by its package")
    void anExclusionEntryExcludesATypeByItsNameOrByItsPackage() {
        final TypeElement foo = elements.addType("p", "Foo");
        final TypeElement bar = elements.addType("p", "Bar");
        final TypeElement other = elements.addType("q", "Other");
        final TypeElement kept = elements.addType("r", "Kept");

        final ConfiguredNames names = namesOf(List.of(), List.of(value("p.Foo"), value("q")));

        assertTrue(names.exclusions().test(foo));
        assertFalse(names.exclusions().test(bar));
        assertTrue(names.exclusions().test(other));
        assertFalse(names.exclusions().test(kept));
    }

    @Test
    @DisplayName("An exclusion entry that names none of the given types is reported as such")
    void anExclusionEntryThatNamesNoneOfTheGivenTypesIsReportedAsSuch() {
        final TypeElement foo = elements.addType("p", "Foo");
        final AnnotationValue matching = value("p.Foo");
        final AnnotationValue unmatched = value("p.Gone");
        final AnnotationValue unmatchedPackage = value("q");

        final ConfiguredNames names =
                namesOf(List.of(), List.of(matching, unmatched, unmatchedPackage));

        assertEquals(
                Map.of("p.Gone", unmatched, "q", unmatchedPackage),
                names.exclusionsNamingNone(List.of(foo)));
    }

    @Test
    @DisplayName("A name that matched nothing is remembered and resolves in a later round")
    void aNameThatMatchedNothingIsRememberedAndResolvesInALaterRound() {
        final AnnotationValue late = value("p.Late");
        final ConfiguredNames.Memory memory = new ConfiguredNames.Memory();
        final ConfiguredNames firstRound = namesOf(List.of(late), List.of());
        memory.remember("c.Config", firstRound.namesMatchingNothing());

        final TypeElement lateType = elements.addType("p", "Late");

        final ConfiguredNames laterRound = namesOf(List.of(late), List.of());
        final List<String> remembered = Objects.requireNonNull(memory.remembered().get("c.Config"));
        assertEquals(List.of("p.Late"), remembered);
        assertEquals(List.of(), laterRound.namesMatchingNothing(remembered));
        assertEquals(Set.of(lateType), laterRound.typesNamed("p.Late").keySet());
        assertEquals(Map.of("p.Late", late), laterRound.entriesMatchingTypeOrPackage(remembered));
    }

    @Test
    @DisplayName("The memory keeps names per configuration class until they are forgotten")
    void theMemoryKeepsNamesPerConfigurationClassUntilTheyAreForgotten() {
        final ConfiguredNames.Memory memory = new ConfiguredNames.Memory();

        memory.remember("c.One", List.of("a.A"));
        memory.remember("c.Two", List.of("b.B"));
        memory.remember("c.One", List.of("a.A", "a.C"));
        final Map<String, List<String>> snapshot = memory.remembered();
        memory.forget("c.Two");

        assertEquals(Map.of("c.One", List.of("a.A", "a.C"), "c.Two", List.of("b.B")), snapshot);
        assertEquals(Map.of("c.One", List.of("a.A", "a.C")), memory.remembered());
    }

    private ConfiguredNames namesOf(
            final List<AnnotationValue> values, final List<AnnotationValue> exclusions) {
        return ConfiguredNames.of(elements.elements(), elements.annotation(values, exclusions));
    }

    private static AnnotationValue value(final String name) {
        return stub(AnnotationValue.class, "value " + name, Map.of("getValue", args -> name));
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
                        ConfiguredNamesTest.class.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static Name nameOf(final String text) {
        return stub(
                Name.class,
                text,
                Map.of("contentEquals", args -> text.contentEquals((CharSequence) args[0])));
    }

    /** The Elements the rules use, over the types and packages put into it. */
    private static final class FakeElements {
        private final Map<String, TypeElement> typesByName = new LinkedHashMap<>();
        private final Map<String, List<TypeElement>> typesByPackage = new LinkedHashMap<>();
        private final Map<TypeElement, PackageElement> packageOfType = new LinkedHashMap<>();
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
                                args -> packagesNamed(args[0].toString()),
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
            final PackageElement packageElement =
                    packageOfType.values().stream()
                            .filter(p -> p.getQualifiedName().contentEquals(packageName))
                            .findFirst()
                            .orElseGet(() -> packageElement(packageName));
            final TypeElement type = typeElement(packageName + "." + simpleName, packageElement);
            typesByName.put(packageName + "." + simpleName, type);
            typesByPackage.computeIfAbsent(packageName, key -> new ArrayList<>()).add(type);
            packageOfType.put(type, packageElement);
            return type;
        }

        TypeElement addNestedType(final TypeElement outer, final String simpleName) {
            final String qualifiedName = outer.getQualifiedName() + "." + simpleName;
            final TypeElement type = typeElement(qualifiedName, outer);
            typesByName.put(qualifiedName, type);
            packageOfType.put(type, packageOfType.get(outer));
            return type;
        }

        void addAlias(final String alias, final TypeElement type) {
            typesByName.put(alias, type);
        }

        AnnotationMirror annotation(
                final List<AnnotationValue> values, final List<AnnotationValue> exclusions) {
            final AnnotationMirror mirror = stub(AnnotationMirror.class, "mirror", Map.of());
            attributes.put(
                    mirror,
                    Map.of(
                            valueAttribute, listValue(values),
                            excludeAttribute, listValue(exclusions)));
            return mirror;
        }

        private static AnnotationValue listValue(final List<AnnotationValue> values) {
            return stub(AnnotationValue.class, "list", Map.of("getValue", args -> values));
        }

        private Set<PackageElement> packagesNamed(final String name) {
            return typesByPackage.containsKey(name)
                    ? Set.of(packageOfType.get(typesByPackage.get(name).get(0)))
                    : Set.of();
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

        private static TypeElement typeElement(final String qualifiedName, final Element parent) {
            return stub(
                    TypeElement.class,
                    "type " + qualifiedName,
                    Map.of(
                            "getQualifiedName",
                            args -> nameOf(qualifiedName),
                            "getEnclosingElement",
                            args -> parent));
        }
    }
}
