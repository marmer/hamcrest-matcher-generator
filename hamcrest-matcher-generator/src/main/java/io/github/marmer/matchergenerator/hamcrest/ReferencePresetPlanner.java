/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static io.github.marmer.matchergenerator.core.model.TypeHierarchy.asTypeElement;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import io.github.marmer.matchergenerator.core.model.PropertyModel;
import io.github.marmer.matchergenerator.core.model.TypeHierarchy;
import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import org.jspecify.annotations.Nullable;

/**
 * Decides which {@code ReferenceMatching} helper compares each property of a reference object:
 * deeply with the matcher of a type generated in the same round, or by {@code equals} for the rest.
 */
class ReferencePresetPlanner {
    /** The name of the reference object parameter of the generated {@code isXLike} methods. */
    static final String REFERENCE = "reference";

    private static final String COLLECTION = "java.util.Collection";
    private static final String LIST = "java.util.List";
    private static final String MAP = "java.util.Map";
    private static final List<String> LIBRARY_PREFIXES = List.of("java.", "javax.", "kotlin.");

    private final TypeModel owner;
    private final List<TypeModel> generatedTypes;
    private final Consumer<String> verboseNotes;
    private @Nullable Map<String, MatcherType> referenceTypes;

    /**
     * @param owner the configured type or nested type whose reference object is preset
     * @param generatedTypes the models of all types generated in the round
     * @param verboseNotes receives the verbose notes about properties compared by {@code equals}
     */
    ReferencePresetPlanner(
            final TypeModel owner,
            final List<TypeModel> generatedTypes,
            final Consumer<String> verboseNotes) {
        this.owner = owner;
        this.generatedTypes = generatedTypes;
        this.verboseNotes = verboseNotes;
    }

    /**
     * @param property a property of the owner
     * @return the code that presets the property of the matcher from the reference object
     */
    CodeBlock presetOf(final PropertyModel property) {
        final CodeBlock value = readOf(property);
        final TypeMirror type = property.type();
        final MatcherType direct = matcherTypeOf(type);
        if (direct != null) {
            return referencePreset("nested", value, direct);
        }
        TypeMirror compared = type;
        if (type.getKind() == TypeKind.ARRAY) {
            compared = ((ArrayType) type).getComponentType();
            final MatcherType item = matcherTypeOf(compared);
            if (item != null) {
                return referencePreset("array", value, item);
            }
            return equalsPreset(property, compared, value);
        }
        final @Nullable List<TypeMirror> collectionItem = argumentsAs(type, COLLECTION);
        if (collectionItem != null && collectionItem.size() == 1) {
            compared = collectionItem.get(0);
            final MatcherType item = matcherTypeOf(compared);
            if (item != null) {
                return referencePreset(
                        argumentsAs(type, LIST) != null ? "list" : "collection", value, item);
            }
        }
        final @Nullable List<TypeMirror> mapTypes = argumentsAs(type, MAP);
        if (mapTypes != null && mapTypes.size() == 2) {
            compared = mapTypes.get(1);
            final MatcherType item = matcherTypeOf(compared);
            if (item != null) {
                return referencePreset("map", value, item);
            }
        }
        return equalsPreset(property, compared, value);
    }

    /**
     * The code that reads the property of the reference object. A getter that declares a checked
     * exception is read through {@link ReferenceMatching#read}, since generated code has no handler
     * for the exception.
     */
    private static CodeBlock readOf(final PropertyModel property) {
        if (!property.declaresCheckedException()) {
            return CodeBlock.of("$L.$L()", REFERENCE, property.accessor());
        }
        return CodeBlock.of(
                "$T.read($S, $L::$L)",
                ReferenceMatching.class,
                property.name(),
                REFERENCE,
                property.accessor());
    }

    /**
     * The property is compared with {@code equals}, for want of a matcher for the type its value is
     * made of. The type might be configured in another processing round or compilation, which this
     * one cannot know, so the user is told in verbose mode.
     */
    private CodeBlock equalsPreset(
            final PropertyModel property, final TypeMirror compared, final CodeBlock value) {
        final @Nullable TypeElement unmatched = unmatchedUserType(compared);
        if (unmatched != null) {
            verboseNotes.accept(
                    MatcherNaming.referenceInitializerName(owner)
                            + " compares the property '"
                            + property.name()
                            + "' with equals: no matcher for "
                            + unmatched.getQualifiedName()
                            + " is generated in this processing round");
        }
        return valuePreset(value);
    }

    /**
     * @return the type if it is a class of the user that no generated type of this round stands
     *     for. Enums, JDK and Kotlin library types are compared with {@code equals} on purpose.
     */
    private @Nullable TypeElement unmatchedUserType(final TypeMirror type) {
        final TypeMirror bound =
                type instanceof WildcardType wildcard ? wildcard.getExtendsBound() : type;
        if (bound == null || bound.getKind() != TypeKind.DECLARED) {
            return null;
        }
        final TypeElement element = asTypeElement(bound);
        final String name = element.getQualifiedName().toString();
        final boolean library = LIBRARY_PREFIXES.stream().anyMatch(name::startsWith);
        return library || element.getKind() == ElementKind.ENUM || isGenerated(name)
                ? null
                : element;
    }

    private boolean isGenerated(final String qualifiedName) {
        return generatedTypes.stream()
                .anyMatch(generated -> isOrContains(generated, qualifiedName));
    }

    private static boolean isOrContains(final TypeModel type, final String qualifiedName) {
        return type.qualifiedName().equals(qualifiedName)
                || type.nestedTypes().stream()
                        .anyMatch(nested -> isOrContains(nested, qualifiedName));
    }

    private CodeBlock valuePreset(final CodeBlock value) {
        return CodeBlock.of("$T.value($L)", ReferenceMatching.class, value);
    }

    private CodeBlock referencePreset(
            final String helper, final CodeBlock value, final MatcherType matcherType) {
        return CodeBlock.of(
                "$T.$L($L, $T::$L)",
                ReferenceMatching.class,
                helper,
                value,
                matcherType.className(),
                matcherType.likeMethod());
    }

    /**
     * @return the matcher a value of the type is compared with deeply, or {@code null} if it is
     *     compared by {@code equals}. That is the case for types without a matcher generated in
     *     this run, for enums and for types without properties, which tell nothing apart.
     */
    private @Nullable MatcherType matcherTypeOf(final TypeMirror type) {
        if (type instanceof WildcardType wildcard) {
            final TypeMirror bound = wildcard.getExtendsBound();
            return bound == null ? null : matcherTypeOf(bound);
        }
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        return referenceTypes().get(asTypeElement(type).getQualifiedName().toString());
    }

    private Map<String, MatcherType> referenceTypes() {
        if (referenceTypes == null) {
            referenceTypes = collectReferenceTypes();
        }
        return referenceTypes;
    }

    private Map<String, MatcherType> collectReferenceTypes() {
        final Map<String, MatcherType> types = new HashMap<>();
        generatedTypes.forEach(
                generated ->
                        collectInto(
                                generated,
                                ClassName.get(
                                        generated.targetPackage(),
                                        MatcherNaming.matcherName(generated)),
                                types));
        return types;
    }

    private void collectInto(
            final TypeModel generated,
            final ClassName className,
            final Map<String, MatcherType> types) {
        if (MatcherNaming.isComparedDeeply(generated)) {
            types.put(
                    generated.qualifiedName(),
                    new MatcherType(className, MatcherNaming.referenceInitializerName(generated)));
        }
        generated
                .nestedTypes()
                .forEach(
                        nested ->
                                collectInto(
                                        nested,
                                        className.nestedClass(MatcherNaming.matcherName(nested)),
                                        types));
    }

    /**
     * The type arguments the type gives to one of its supertypes, as in {@code Lines extends
     * ArrayList<Line>} giving {@code Line} to {@code Collection}.
     *
     * @return the arguments, or {@code null} if the type has no such supertype
     */
    private @Nullable List<TypeMirror> argumentsAs(
            final TypeMirror type, final String qualifiedName) {
        return type.getKind() == TypeKind.DECLARED
                ? argumentsAs(
                        asTypeElement(type),
                        ((DeclaredType) type).getTypeArguments(),
                        qualifiedName)
                : null;
    }

    private @Nullable List<TypeMirror> argumentsAs(
            final TypeElement type,
            final List<? extends TypeMirror> arguments,
            final String qualifiedName) {
        if (type.getQualifiedName().contentEquals(qualifiedName)) {
            return new ArrayList<>(arguments);
        }
        return TypeHierarchy.directSupertypes(type, arguments).stream()
                .map(
                        supertype ->
                                argumentsAs(
                                        supertype.element(), supertype.arguments(), qualifiedName))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** The matcher generated for a type and the name of its method for reference objects. */
    private record MatcherType(ClassName className, String likeMethod) {}
}
