/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/**
 * Walks the supertypes of a type for the backends: the direct supertypes, with the type variables
 * of the type replaced by the type arguments it is given.
 */
public final class TypeHierarchy {

    private TypeHierarchy() {}

    /** The element of a declared type mirror. */
    public static TypeElement asTypeElement(final TypeMirror type) {
        return (TypeElement) ((DeclaredType) type).asElement();
    }

    /**
     * The direct supertypes of the type, the superclass first and then the interfaces, in the order
     * the type declares them. A type variable of the type among the arguments of a supertype is
     * replaced by the argument given at the same position in {@code arguments}, as in {@code
     * Holder<E> extends ArrayList<E>} given {@code String} giving {@code ArrayList<String>}. A type
     * variable without such an argument stays as it is, so pass no arguments when they do not
     * matter.
     */
    public static List<Supertype> directSupertypes(
            final TypeElement type, final List<? extends TypeMirror> arguments) {
        final Map<String, TypeMirror> bindings = new HashMap<>();
        for (var i = 0; i < Math.min(arguments.size(), type.getTypeParameters().size()); i++) {
            bindings.put(
                    type.getTypeParameters().get(i).getSimpleName().toString(), arguments.get(i));
        }
        return Stream.concat(Stream.of(type.getSuperclass()), type.getInterfaces().stream())
                .filter(supertype -> supertype.getKind() == TypeKind.DECLARED)
                .map(
                        supertype ->
                                new Supertype(
                                        asTypeElement(supertype),
                                        boundArguments((DeclaredType) supertype, bindings)))
                .toList();
    }

    /** The arguments of a supertype with the type variables of the subtype replaced. */
    private static List<TypeMirror> boundArguments(
            final DeclaredType supertype, final Map<String, TypeMirror> bindings) {
        return supertype.getTypeArguments().stream()
                .map(
                        argument ->
                                argument.getKind() == TypeKind.TYPEVAR
                                        ? bindings.getOrDefault(
                                                ((TypeVariable) argument)
                                                        .asElement()
                                                        .getSimpleName()
                                                        .toString(),
                                                argument)
                                        : argument)
                .toList();
    }

    /** A direct supertype with the type arguments the subtype gives it. */
    public record Supertype(TypeElement element, List<TypeMirror> arguments) {}
}
