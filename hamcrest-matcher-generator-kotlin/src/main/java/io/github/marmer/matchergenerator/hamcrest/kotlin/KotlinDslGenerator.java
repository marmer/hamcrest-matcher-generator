/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest.kotlin;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.FileSpec;
import com.squareup.kotlinpoet.FunSpec;
import com.squareup.kotlinpoet.KModifier;
import com.squareup.kotlinpoet.LambdaTypeName;
import com.squareup.kotlinpoet.MemberName;
import com.squareup.kotlinpoet.ParameterSpec;
import com.squareup.kotlinpoet.ParameterizedTypeName;
import com.squareup.kotlinpoet.PropertySpec;
import com.squareup.kotlinpoet.TypeName;
import com.squareup.kotlinpoet.TypeSpec;
import com.squareup.kotlinpoet.WildcardTypeName;
import io.github.marmer.matchergenerator.core.model.Nullability;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.hamcrest.MatcherNaming;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.jspecify.annotations.Nullable;

/**
 * Writes the Kotlin DSL of one configured type: an entry function and a scope class per type, the
 * type itself and its nested types, each delegating to the generated Hamcrest matcher.
 */
final class KotlinDslGenerator {

    /** The private property of a scope that holds the Java matcher it delegates to. */
    private static final String DELEGATE = "delegate";

    /** The parameter of the nested scope lambda, of the entry function and of a nested property. */
    private static final String BLOCK = "block";

    /** The parameter of the overload that takes a Hamcrest matcher. */
    private static final String MATCHER_PARAMETER = "matcher";

    /** The members of {@code Any} that a scope's functions would hide or be hidden by. */
    private static final String EQUALS = "equals";

    private static final String HASH_CODE = "hashCode";
    private static final String TO_STRING = "toString";

    /**
     * The names a DSL scope uses for its own members and parameters, and the members of {@code
     * Any}. A property of one of these names gets no DSL functions, which would hide or be hidden
     * by those members. The Java matcher keeps the property.
     */
    private static final Set<String> RESERVED_NAMES =
            Set.of(
                    MatcherNaming.CHECKING_ALL_PROPERTIES,
                    DELEGATE,
                    BLOCK,
                    MATCHER_PARAMETER,
                    EQUALS,
                    HASH_CODE,
                    TO_STRING);

    private static final ClassName MATCHER = new ClassName("org.hamcrest", "Matcher");
    private static final ClassName MATCHER_DSL =
            new ClassName(MatcherDsl.class.getPackageName(), MatcherDsl.class.getSimpleName());
    private static final ClassName UNIT = new ClassName("kotlin", "Unit");

    private final TypeModel model;
    private final GenerationContext context;
    private final Map<String, String> dslNames;
    private final Map<String, Scope> scopes;

    KotlinDslGenerator(final TypeModel model, final GenerationContext context) {
        this.model = model;
        this.context = context;
        final List<TypeModel> all =
                Stream.concat(context.generatedTypes().stream(), Stream.of(model)).toList();
        this.dslNames = dslNamesOf(all);
        this.scopes = scopesOf(all, dslNames);
    }

    /** The name of the Kotlin file of a type, which is also the name of its Java matcher's DSL. */
    static String fileName(final TypeModel type) {
        return MatcherNaming.matcherName(type) + "Dsl";
    }

    /** The matcher, the entry function and the scope class generated for one type. */
    private record Scope(ClassName matcher, MemberName entry, ClassName spec) {}

    /** A type with the simple names of the types enclosing it, outermost first. */
    private record Placed(TypeModel type, List<String> enclosing) {}

    private static List<Placed> placedTypesOf(final List<TypeModel> types) {
        final Map<String, Placed> placed = new LinkedHashMap<>();
        types.forEach(type -> place(type, List.of(), placed));
        return List.copyOf(placed.values());
    }

    private static void place(
            final TypeModel type, final List<String> enclosing, final Map<String, Placed> placed) {
        placed.putIfAbsent(type.qualifiedName(), new Placed(type, enclosing));
        final List<String> inner = new ArrayList<>(enclosing);
        inner.add(type.simpleName());
        type.nestedTypes().forEach(nested -> place(nested, inner, placed));
    }

    /**
     * The name each type with a DSL scope takes its scope class ({@code <name>Spec}) and entry
     * function ({@code is<name>}) from, by qualified type name. That is the simple name of the
     * type, unless another type with a scope in the same package has the same simple name: all
     * scopes of a package are top level declarations of it. Then a nested type puts the simple
     * names of its enclosing types before its own, outermost first; a top level type keeps its
     * name.
     */
    private static Map<String, String> dslNamesOf(final List<TypeModel> types) {
        final List<Placed> placed =
                placedTypesOf(types).stream()
                        .filter(entry -> MatcherNaming.hasProperties(entry.type()))
                        .toList();
        final Map<String, Long> countBySimpleName =
                placed.stream()
                        .collect(
                                Collectors.groupingBy(
                                        entry -> clashKey(entry.type()), Collectors.counting()));
        final Map<String, String> names = new HashMap<>();
        placed.forEach(
                entry -> {
                    final String simpleName = entry.type().simpleName();
                    final boolean clashing =
                            countBySimpleName.getOrDefault(clashKey(entry.type()), 0L) > 1;
                    names.put(
                            entry.type().qualifiedName(),
                            clashing
                                    ? String.join("", entry.enclosing()) + simpleName
                                    : simpleName);
                });
        return names;
    }

    private static String clashKey(final TypeModel type) {
        return type.targetPackage() + "/" + type.simpleName();
    }

    private static Map<String, Scope> scopesOf(
            final List<TypeModel> types, final Map<String, String> dslNames) {
        final Map<String, Scope> scopes = new HashMap<>();
        types.forEach(type -> collect(type, List.of(), dslNames, scopes));
        return scopes;
    }

    private static void collect(
            final TypeModel type,
            final List<String> enclosingMatchers,
            final Map<String, String> dslNames,
            final Map<String, Scope> scopes) {
        final List<String> matcherNames = new ArrayList<>(enclosingMatchers);
        matcherNames.add(MatcherNaming.matcherName(type));
        if (MatcherNaming.isComparedDeeply(type)) {
            final String dslName = dslNameOf(dslNames, type);
            scopes.putIfAbsent(
                    type.qualifiedName(),
                    new Scope(
                            new ClassName(type.targetPackage(), matcherNames),
                            new MemberName(type.targetPackage(), entryName(dslName)),
                            new ClassName(type.targetPackage(), specName(dslName))));
        }
        type.nestedTypes().forEach(nested -> collect(nested, matcherNames, dslNames, scopes));
    }

    /**
     * Whether the type or one of its nested types has a property to check. A type without
     * properties gets no DSL, which also spares the Kotlin compiler types it cannot see, such as
     * the class Kotlin generates for top level functions.
     */
    static boolean hasAnythingToGenerate(final TypeModel type) {
        return MatcherNaming.hasProperties(type)
                || type.nestedTypes().stream().anyMatch(KotlinDslGenerator::hasAnythingToGenerate);
    }

    private static String specName(final String dslName) {
        return dslName + "Spec";
    }

    private static String dslNameOf(final Map<String, String> dslNames, final TypeModel type) {
        return Objects.requireNonNull(dslNames.get(type.qualifiedName()), type.qualifiedName());
    }

    private static String entryName(final String dslName) {
        return "is" + dslName;
    }

    String generate() {
        final FileSpec.Builder file = FileSpec.builder(model.targetPackage(), fileName(model));
        file.addFileComment("Generated by %L. Do not edit.", context.generatorName());
        addTo(file, model, List.of());
        return file.build().toString();
    }

    private void addTo(
            final FileSpec.Builder file,
            final TypeModel type,
            final List<String> enclosingMatchers) {
        final List<String> matcherNames = new ArrayList<>(enclosingMatchers);
        matcherNames.add(MatcherNaming.matcherName(type));
        final ClassName matcher = new ClassName(type.targetPackage(), matcherNames);
        if (MatcherNaming.hasProperties(type)) {
            file.addFunction(entryFunction(type, matcher));
            file.addType(scopeClass(type, matcher));
        }
        type.nestedTypes().forEach(nested -> addTo(file, nested, matcherNames));
    }

    private FunSpec entryFunction(final TypeModel type, final ClassName matcher) {
        final String dslName = dslNameOf(dslNames, type);
        final ClassName spec = new ClassName(type.targetPackage(), specName(dslName));
        return FunSpec.builder(entryName(dslName))
                .addParameter(BLOCK, blockType(spec))
                .returns(matcher)
                .addStatement(
                        "return %T.%L().also { %T(it).block() }",
                        matcher, MatcherNaming.initializerName(type), spec)
                .build();
    }

    private static TypeName blockType(final ClassName spec) {
        return LambdaTypeName.get(spec, List.<ParameterSpec>of(), UNIT);
    }

    private TypeSpec scopeClass(final TypeModel type, final ClassName matcher) {
        final TypeSpec.Builder scope =
                TypeSpec.classBuilder(specName(dslNameOf(dslNames, type)))
                        .addAnnotation(MATCHER_DSL)
                        .primaryConstructor(
                                FunSpec.constructorBuilder()
                                        .addParameter(DELEGATE, matcher)
                                        .build())
                        .addProperty(
                                PropertySpec.builder(DELEGATE, matcher, KModifier.PRIVATE)
                                        .initializer(DELEGATE)
                                        .build())
                        .addFunction(
                                FunSpec.builder(MatcherNaming.CHECKING_ALL_PROPERTIES)
                                        .addStatement(
                                                "%L.%L()",
                                                DELEGATE, MatcherNaming.CHECKING_ALL_PROPERTIES)
                                        .build());
        PropertyModel.distinctByName(type.properties())
                .forEach(property -> addProperty(scope, type, property));
        return scope.build();
    }

    private void addProperty(
            final TypeSpec.Builder scope, final TypeModel type, final PropertyModel property) {
        if (RESERVED_NAMES.contains(property.name())) {
            context.reportWarning(
                    "The Kotlin DSL of "
                            + type.qualifiedName()
                            + " leaves out the property "
                            + property.name()
                            + ", whose name clashes with a member of the DSL scope; the Java"
                            + " matcher keeps it");
            return;
        }
        final boolean conflicting =
                type.properties().stream()
                                .filter(other -> other.name().equals(property.name()))
                                .count()
                        > 1;
        type.properties().stream()
                .filter(other -> other.name().equals(property.name()))
                .forEach(other -> scope.addFunction(valueFunction(other)));
        if (!MatcherNaming.isMatcherType(property.type())) {
            scope.addFunction(matcherFunction(property, conflicting));
            final @Nullable Scope nested = nestedScopeOf(property.type());
            if (nested != null) {
                scope.addFunction(nestedFunction(property, nested));
            }
        }
        scope.addFunction(
                FunSpec.builder(MatcherNaming.ignoringMethodName(property.name()))
                        .addStatement(
                                "%L.%L()",
                                DELEGATE, MatcherNaming.ignoringMethodName(property.name()))
                        .build());
    }

    private static FunSpec valueFunction(final PropertyModel property) {
        final TypeName type = KotlinTypes.of(property.type());
        return FunSpec.builder(property.name())
                .addParameter(
                        "value",
                        property.type().getKind().isPrimitive() ? type : type.copy(true, List.of()))
                .addStatement(
                        "%L.%L(value)", DELEGATE, MatcherNaming.withMethodName(property.name()))
                .build();
    }

    private static FunSpec matcherFunction(
            final PropertyModel property, final boolean conflicting) {
        final TypeName matched;
        if (conflicting) {
            matched = WildcardTypeName.producerOf(KotlinTypes.any().copy(true, List.of()));
        } else {
            final TypeName type = KotlinTypes.of(property.type());
            matched =
                    WildcardTypeName.consumerOf(
                            property.nullability() == Nullability.NULLABLE
                                    ? type.copy(true, List.of())
                                    : type);
        }
        return FunSpec.builder(property.name())
                .addParameter(MATCHER_PARAMETER, ParameterizedTypeName.get(MATCHER, matched))
                .addStatement(
                        "%L.%L(matcher)", DELEGATE, MatcherNaming.withMethodName(property.name()))
                .build();
    }

    private static FunSpec nestedFunction(final PropertyModel property, final Scope nested) {
        return FunSpec.builder(property.name())
                .addParameter(BLOCK, blockType(nested.spec()))
                .addStatement(
                        "%L.%L(%M(block))",
                        DELEGATE, MatcherNaming.withMethodName(property.name()), nested.entry())
                .build();
    }

    private @Nullable Scope nestedScopeOf(final TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        return scopes.get(
                ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString());
    }
}
