/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Builds the {@code PropertyRepresentation} of a target package: an AssertJ {@code
 * StandardRepresentation} that prints an instance of a configured type with properties by its
 * properties, such as {@code Line[sku="A-1", quantity=2]}, and formats everything else as AssertJ
 * does. It overrides the fallback, so the elements of a collection are printed by it too. The user
 * registers it with {@code Assertions.useRepresentation}; it is never registered automatically.
 *
 * <p>A subtype is tested before its supertypes, so its branch is reachable. An object that is
 * already being rendered on the current thread is printed by AssertJ's default rendering, which
 * stops a cycle of configured types.
 */
final class RepresentationGenerator {
    static final String NAME = "PropertyRepresentation";
    private static final ClassName STANDARD_REPRESENTATION =
            ClassName.get("org.assertj.core.presentation", "StandardRepresentation");
    private static final String FALLBACK_TO_STRING_OF = "fallbackToStringOf";
    private static final String OBJECT = "object";
    private static final String ACTUAL = "actual";
    private static final String RENDERING = "RENDERING";

    private final List<TypeModel> models;
    private final GenerationContext context;

    /**
     * @param models the top level types of the package, sorted by name, the first one writes the
     *     representation
     * @param context information about the current generation run
     */
    RepresentationGenerator(final List<TypeModel> models, final GenerationContext context) {
        this.models = models;
        this.context = context;
    }

    TypeSpec typeSpec() {
        final AssertionGenerator first = new AssertionGenerator(models.get(0), context);
        final MethodSpec.Builder fallback =
                MethodSpec.methodBuilder(FALLBACK_TO_STRING_OF)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PROTECTED)
                        .addParameter(first.nullable(TypeName.OBJECT), OBJECT, Modifier.FINAL)
                        .returns(String.class)
                        .beginControlFlow("if (!$L.get().add($L))", RENDERING, OBJECT)
                        .addStatement("return super.$L($L)", FALLBACK_TO_STRING_OF, OBJECT)
                        .endControlFlow()
                        .beginControlFlow("try");
        branchModels().forEach(model -> addBranch(fallback, model));
        fallback.addStatement("return super.$L($L)", FALLBACK_TO_STRING_OF, OBJECT)
                .nextControlFlow("finally")
                .addStatement("$L.get().remove($L)", RENDERING, OBJECT)
                .endControlFlow();
        return first.topLevel(
                TypeSpec.classBuilder(NAME)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(first.generatedAnnotation())
                        .superclass(STANDARD_REPRESENTATION)
                        .addField(renderingField(first))
                        .addMethod(fallback.build()));
    }

    /**
     * The objects being rendered on the current thread, by identity, so that a cycle is found. The
     * fallback removes an object again when its rendering ends.
     */
    private static FieldSpec renderingField(final AssertionGenerator first) {
        final TypeName objectSet =
                ParameterizedTypeName.get(ClassName.get(Set.class), TypeName.OBJECT);
        return FieldSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(ThreadLocal.class), objectSet),
                        RENDERING,
                        Modifier.PRIVATE,
                        Modifier.STATIC,
                        Modifier.FINAL)
                .initializer(
                        "$T.withInitial(() -> $T.newSetFromMap(new $T<>()))",
                        ThreadLocal.class,
                        Collections.class,
                        IdentityHashMap.class)
                .build();
    }

    /**
     * The types with properties, nested types included, subtypes before their supertypes. A type
     * with more ancestors is a subtype of every type it has among its ancestors, so sorting by the
     * number of ancestors, most first, puts a subtype before its supertypes. Ties are sorted by
     * qualified name, so the order is the same in every run.
     */
    private List<TypeModel> branchModels() {
        final List<TypeModel> all = new ArrayList<>();
        models.forEach(model -> flatten(model, all));
        all.removeIf(model -> new AssertionGenerator(model, context).properties().isEmpty());
        all.sort(
                Comparator.comparingInt((TypeModel model) -> ancestorCount(model.type()))
                        .reversed()
                        .thenComparing(TypeModel::qualifiedName));
        return all;
    }

    private static void flatten(final TypeModel model, final List<TypeModel> into) {
        into.add(model);
        model.nestedTypes().forEach(nested -> flatten(nested, into));
    }

    /** The number of distinct supertypes, direct and inherited, of the type. */
    private static int ancestorCount(final TypeMirror type) {
        final Set<String> ancestors = new HashSet<>();
        collectAncestors(asTypeElement(type), ancestors);
        return ancestors.size();
    }

    private static void collectAncestors(final TypeElement type, final Set<String> ancestors) {
        final List<TypeMirror> supertypes = new ArrayList<>(type.getInterfaces());
        supertypes.add(type.getSuperclass());
        for (final TypeMirror supertype : supertypes) {
            if (supertype.getKind() == TypeKind.DECLARED) {
                final TypeElement element = asTypeElement(supertype);
                if (ancestors.add(element.getQualifiedName().toString())) {
                    collectAncestors(element, ancestors);
                }
            }
        }
    }

    private static TypeElement asTypeElement(final TypeMirror type) {
        return (TypeElement) ((DeclaredType) type).asElement();
    }

    /** One branch: {@code if (object instanceof Line actual) return ...;}. */
    private void addBranch(final MethodSpec.Builder fallback, final TypeModel model) {
        final AssertionGenerator generator = new AssertionGenerator(model, context);
        fallback.beginControlFlow(
                        "if ($L instanceof $T $L)", OBJECT, generator.assertedType(), ACTUAL)
                .addStatement("return $L", expression(model, generator.properties()))
                .endControlFlow();
    }

    /**
     * {@code "Line[sku=" + toStringOf(actual.getSku()) + ", quantity=" + ... + "]"}, each value
     * formatted by the representation itself.
     */
    private static CodeBlock expression(
            final TypeModel model, final List<PropertyModel> properties) {
        final CodeBlock.Builder expression = CodeBlock.builder();
        String literal = model.simpleName() + "[";
        for (final PropertyModel property : properties) {
            expression.add(
                    "$S + toStringOf($L.$L()) + ",
                    literal + property.name() + "=",
                    ACTUAL,
                    property.accessor());
            literal = ", ";
        }
        return expression.add("$S", "]").build();
    }
}
