/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Finds the top level class names of a package that two claimants want: {@code Foo} and {@code
 * AbstractFoo} both generate {@code AbstractFooAssert}, {@code Foo} generates {@code FooAssert}
 * which is also the name of a configured type, and a configured type may be called like the mixin
 * or the representation of the package. Each of them makes the compiler refuse the second class, so
 * the backend reports it and writes nothing for the package. Names that only resemble each other,
 * such as {@code FooBar} and {@code Foo_Bar}, are different classes and no clash: their factory
 * constants live in different classes.
 */
final class GeneratedNameClashes {
    private static final String MIXIN_CLAIMANT =
            "the generated mixin " + SoftAssertionsProviderGenerator.NAME;
    private static final String REPRESENTATION_CLAIMANT =
            "the generated representation " + RepresentationGenerator.NAME;

    private final Map<String, Set<String>> claimantsByName = new TreeMap<>();

    GeneratedNameClashes(final List<TypeModel> packageModels) {
        claim(SoftAssertionsProviderGenerator.NAME, MIXIN_CLAIMANT);
        claim(RepresentationGenerator.NAME, REPRESENTATION_CLAIMANT);
        packageModels.forEach(
                model -> {
                    final String claimant = "'" + model.qualifiedName() + "'";
                    claim(model.simpleName(), claimant);
                    claim(AssertionGenerator.abstractNameOf(model.simpleName()), claimant);
                    claim(AssertionGenerator.concreteNameOf(model.simpleName()), claimant);
                });
    }

    private void claim(final String name, final String claimant) {
        claimantsByName.computeIfAbsent(name, key -> new LinkedHashSet<>()).add(claimant);
    }

    /** One message per class name that two or more claimants want. */
    List<String> messages(final String targetPackage) {
        final List<String> messages = new ArrayList<>();
        claimantsByName.forEach(
                (name, claimants) -> {
                    if (claimants.size() > 1) {
                        messages.add(
                                claimants.stream().collect(Collectors.joining(" and "))
                                        + " of package '"
                                        + targetPackage
                                        + "' both need the class name '"
                                        + name
                                        + "', which clashes; rename one of the types");
                    }
                });
        return messages;
    }
}
