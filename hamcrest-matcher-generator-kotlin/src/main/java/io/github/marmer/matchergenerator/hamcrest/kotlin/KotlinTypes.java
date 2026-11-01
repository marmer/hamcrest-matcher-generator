/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest.kotlin;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.ParameterizedTypeName;
import com.squareup.kotlinpoet.TypeName;
import com.squareup.kotlinpoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

/**
 * Writes Java types the way Kotlin spells them: Java's {@code String} as {@code kotlin.String},
 * primitives as Kotlin's, the collection interfaces as the Kotlin ones. A type variable is an
 * unknown type, which is {@code Any}, or a star projection as a type argument. This mirrors the
 * wildcards the generated Hamcrest matcher uses, so every argument the DSL accepts is one the
 * matcher accepts.
 */
final class KotlinTypes {

    private static final String KOTLIN = "kotlin";
    private static final String KOTLIN_COLLECTIONS = "kotlin.collections";

    private static final ClassName ANY = new ClassName(KOTLIN, "Any");

    /** Java classes that Kotlin knows under another name. */
    private static final Map<String, ClassName> MAPPED =
            Map.ofEntries(
                    Map.entry("java.lang.Object", ANY),
                    Map.entry("java.lang.String", new ClassName(KOTLIN, "String")),
                    Map.entry("java.lang.CharSequence", new ClassName(KOTLIN, "CharSequence")),
                    Map.entry("java.lang.Number", new ClassName(KOTLIN, "Number")),
                    Map.entry("java.lang.Boolean", new ClassName(KOTLIN, "Boolean")),
                    Map.entry("java.lang.Byte", new ClassName(KOTLIN, "Byte")),
                    Map.entry("java.lang.Short", new ClassName(KOTLIN, "Short")),
                    Map.entry("java.lang.Integer", new ClassName(KOTLIN, "Int")),
                    Map.entry("java.lang.Long", new ClassName(KOTLIN, "Long")),
                    Map.entry("java.lang.Character", new ClassName(KOTLIN, "Char")),
                    Map.entry("java.lang.Float", new ClassName(KOTLIN, "Float")),
                    Map.entry("java.lang.Double", new ClassName(KOTLIN, "Double")),
                    Map.entry("java.lang.Comparable", new ClassName(KOTLIN, "Comparable")),
                    Map.entry("java.lang.Enum", new ClassName(KOTLIN, "Enum")),
                    Map.entry("java.lang.Throwable", new ClassName(KOTLIN, "Throwable")),
                    Map.entry(
                            "java.lang.annotation.Annotation", new ClassName(KOTLIN, "Annotation")),
                    Map.entry("java.lang.Iterable", new ClassName(KOTLIN_COLLECTIONS, "Iterable")),
                    Map.entry("java.util.Iterator", new ClassName(KOTLIN_COLLECTIONS, "Iterator")),
                    Map.entry(
                            "java.util.ListIterator",
                            new ClassName(KOTLIN_COLLECTIONS, "ListIterator")),
                    Map.entry(
                            "java.util.Collection",
                            new ClassName(KOTLIN_COLLECTIONS, "Collection")),
                    Map.entry("java.util.List", new ClassName(KOTLIN_COLLECTIONS, "List")),
                    Map.entry("java.util.Set", new ClassName(KOTLIN_COLLECTIONS, "Set")),
                    Map.entry("java.util.Map", new ClassName(KOTLIN_COLLECTIONS, "Map")),
                    Map.entry(
                            "java.util.Map.Entry",
                            new ClassName(KOTLIN_COLLECTIONS, "Map", "Entry")));

    /**
     * Kotlin types that declare their type parameters {@code out}, by the indexes of those
     * parameters. Writing {@code out} again there is redundant, and a warning for the compiler.
     */
    private static final Map<String, Set<Integer>> DECLARED_OUT =
            Map.of(
                    "java.lang.Iterable", Set.of(0),
                    "java.util.Iterator", Set.of(0),
                    "java.util.ListIterator", Set.of(0),
                    "java.util.Collection", Set.of(0),
                    "java.util.List", Set.of(0),
                    "java.util.Set", Set.of(0),
                    "java.util.Map", Set.of(1),
                    "java.util.Map.Entry", Set.of(0, 1));

    private KotlinTypes() {}

    static ClassName any() {
        return ANY;
    }

    /** The type of a value: a type variable is {@code Any}, a primitive stays a primitive. */
    static TypeName of(final TypeMirror type) {
        return switch (type.getKind()) {
            case BOOLEAN -> new ClassName(KOTLIN, "Boolean");
            case BYTE -> new ClassName(KOTLIN, "Byte");
            case SHORT -> new ClassName(KOTLIN, "Short");
            case INT -> new ClassName(KOTLIN, "Int");
            case LONG -> new ClassName(KOTLIN, "Long");
            case CHAR -> new ClassName(KOTLIN, "Char");
            case FLOAT -> new ClassName(KOTLIN, "Float");
            case DOUBLE -> new ClassName(KOTLIN, "Double");
            case ARRAY -> arrayOf((ArrayType) type);
            case DECLARED -> declared((DeclaredType) type);
            default -> ANY;
        };
    }

    private static TypeName arrayOf(final ArrayType type) {
        final TypeMirror component = type.getComponentType();
        if (component.getKind().isPrimitive()) {
            return new ClassName(KOTLIN, ((ClassName) of(component)).getSimpleName() + "Array");
        }
        return ParameterizedTypeName.get(
                new ClassName(KOTLIN, "Array"), argument(component, false));
    }

    private static TypeName declared(final DeclaredType type) {
        final TypeElement element = (TypeElement) type.asElement();
        final String qualifiedName = element.getQualifiedName().toString();
        final ClassName rawName = classNameOf(element);
        if (type.getTypeArguments().isEmpty()) {
            return rawName;
        }
        final Set<Integer> declaredOut = DECLARED_OUT.getOrDefault(qualifiedName, Set.of());
        final List<TypeName> arguments = new ArrayList<>();
        for (var i = 0; i < type.getTypeArguments().size(); i++) {
            arguments.add(argument(type.getTypeArguments().get(i), declaredOut.contains(i)));
        }
        return ParameterizedTypeName.get(rawName, arguments);
    }

    /**
     * A type argument. Java generics are invariant, so a Kotlin type spelled with an argument that
     * is not a star projection accepts only exactly that argument. The matchers the DSL calls take
     * {@code ? extends} arguments, which Kotlin spells {@code out}.
     */
    private static TypeName argument(final TypeMirror argument, final boolean declaredOut) {
        if (argument instanceof WildcardType wildcard) {
            if (wildcard.getExtendsBound() != null) {
                return WildcardTypeName.producerOf(of(wildcard.getExtendsBound()));
            }
            if (wildcard.getSuperBound() != null) {
                return WildcardTypeName.consumerOf(of(wildcard.getSuperBound()));
            }
            return star();
        }
        if (argument.getKind() == TypeKind.TYPEVAR) {
            return star();
        }
        final TypeName name = of(argument);
        return declaredOut ? name : WildcardTypeName.producerOf(name);
    }

    private static TypeName star() {
        return WildcardTypeName.producerOf(ANY.copy(true, List.of()));
    }

    private static ClassName classNameOf(final TypeElement element) {
        final ClassName mapped = MAPPED.get(element.getQualifiedName().toString());
        if (mapped != null) {
            return mapped;
        }
        final List<String> simpleNames = new ArrayList<>();
        Element current = element;
        while (current instanceof TypeElement type) {
            simpleNames.add(0, type.getSimpleName().toString());
            current = type.getEnclosingElement();
        }
        // The outermost enclosing element of a type is its package, the unnamed one included.
        final String packageName =
                ((PackageElement) Objects.requireNonNull(current)).getQualifiedName().toString();
        return new ClassName(packageName, simpleNames);
    }
}
