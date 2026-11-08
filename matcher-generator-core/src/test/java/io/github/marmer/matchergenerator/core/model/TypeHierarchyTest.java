/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

import static io.github.marmer.matchergenerator.core.model.TypeHierarchy.asTypeElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.marmer.matchergenerator.core.model.TypeHierarchy.Supertype;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The supertype walk over real type mirrors of a compilation. */
class TypeHierarchyTest {

    /** The JDK types the tests walk to; the fixture types come from the compiled source. */
    private static final List<String> JDK_TYPES =
            List.of(
                    "java.lang.Object",
                    "java.lang.String",
                    "java.util.ArrayList",
                    "java.util.List");

    private static final Map<String, TypeElement> TYPES = compileTypes();

    @Test
    @DisplayName("A superclass gets the arguments its subtype declares for it")
    void aSuperclassGetsTheArgumentsItsSubtypeDeclaresForIt() {
        final List<Supertype> supertypes =
                TypeHierarchy.directSupertypes(type("some.Lines"), List.of());

        assertEquals(List.of("some.Holder"), names(supertypes));
        assertEquals(List.of("some.Line"), argumentNames(supertypes.get(0)));
    }

    @Test
    @DisplayName("A type variable of the subtype is replaced by the argument it is given")
    void aTypeVariableOfTheSubtypeIsReplacedByTheArgumentItIsGiven() {
        final List<Supertype> supertypes =
                TypeHierarchy.directSupertypes(
                        type("some.Holder"), List.of(type("java.lang.String").asType()));

        assertEquals(List.of("java.util.ArrayList"), names(supertypes));
        assertEquals(List.of("java.lang.String"), argumentNames(supertypes.get(0)));
    }

    @Test
    @DisplayName(
            "An interface with bound arguments gives them, as class Bag implements List<String>")
    void anInterfaceWithBoundArgumentsGivesThem() {
        final List<Supertype> supertypes =
                TypeHierarchy.directSupertypes(type("some.Bag"), List.of());

        assertEquals(List.of("java.lang.Object", "java.util.List"), names(supertypes));
        assertEquals(List.of("java.lang.String"), argumentNames(supertypes.get(1)));
    }

    @Test
    @DisplayName("Object and an interface without supertypes have no direct supertypes")
    void objectAndAnInterfaceWithoutSupertypesHaveNoDirectSupertypes() {
        assertTrue(TypeHierarchy.directSupertypes(type("java.lang.Object"), List.of()).isEmpty());
        assertTrue(TypeHierarchy.directSupertypes(type("some.Plain"), List.of()).isEmpty());
    }

    @Test
    @DisplayName("A diamond reports the shared supertype once on each path to it")
    void aDiamondReportsTheSharedSupertypeOnEachPathToIt() {
        assertEquals(
                List.of("java.lang.Object", "some.B", "some.C"),
                names(TypeHierarchy.directSupertypes(type("some.D"), List.of())));
        assertEquals(
                List.of("some.A"),
                names(TypeHierarchy.directSupertypes(type("some.B"), List.of())));
        assertEquals(
                List.of("some.A"),
                names(TypeHierarchy.directSupertypes(type("some.C"), List.of())));
    }

    @Test
    @DisplayName("asTypeElement gives the element of a declared type mirror")
    void asTypeElementGivesTheElementOfADeclaredTypeMirror() {
        assertEquals(
                "java.lang.String",
                asTypeElement(type("java.lang.String").asType()).getQualifiedName().toString());
    }

    private static TypeElement type(final String qualifiedName) {
        return Objects.requireNonNull(TYPES.get(qualifiedName));
    }

    private static List<String> names(final List<Supertype> supertypes) {
        return supertypes.stream()
                .map(supertype -> supertype.element().getQualifiedName().toString())
                .toList();
    }

    private static List<String> argumentNames(final Supertype supertype) {
        return supertype.arguments().stream().map(Object::toString).toList();
    }

    /**
     * Compiles the fixture types and keeps their elements by qualified name, collected while
     * processing: the root elements of the compiled source, and the JDK types the tests walk to.
     */
    private static Map<String, TypeElement> compileTypes() {
        final Map<String, TypeElement> found = new HashMap<>();
        CompilationSubject.assertThat(
                        Compiler.javac()
                                .withProcessors(
                                        new AbstractProcessor() {
                                            @Override
                                            public Set<String> getSupportedAnnotationTypes() {
                                                return Set.of("*");
                                            }

                                            @Override
                                            public SourceVersion getSupportedSourceVersion() {
                                                return SourceVersion.latestSupported();
                                            }

                                            @Override
                                            public boolean process(
                                                    final Set<? extends TypeElement> annotations,
                                                    final RoundEnvironment round) {
                                                for (final TypeElement type :
                                                        ElementFilter.typesIn(
                                                                round.getRootElements())) {
                                                    found.put(
                                                            type.getQualifiedName().toString(),
                                                            type);
                                                }
                                                final Elements elements =
                                                        processingEnv.getElementUtils();
                                                JDK_TYPES.forEach(
                                                        name ->
                                                                found.put(
                                                                        name,
                                                                        elements.getTypeElement(
                                                                                name)));
                                                return false;
                                            }
                                        })
                                .compile(
                                        JavaFileObjects.forSourceLines(
                                                "some.Types",
                                                "package some;",
                                                "import java.util.ArrayList;",
                                                "import java.util.List;",
                                                "abstract class Holder<E> extends ArrayList<E> {}",
                                                "abstract class Bag implements List<String> {}",
                                                "abstract class Lines extends Holder<Line> {}",
                                                "class Line {}",
                                                "interface Plain {}",
                                                "interface A {}",
                                                "interface B extends A {}",
                                                "interface C extends A {}",
                                                "abstract class D implements B, C {}")))
                .succeeded();
        return found;
    }
}
