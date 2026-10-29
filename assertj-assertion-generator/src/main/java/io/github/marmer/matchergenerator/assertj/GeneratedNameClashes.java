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
            "the generated mixin '" + SoftAssertionsProviderGenerator.NAME + "'";
    private static final String REPRESENTATION_CLAIMANT =
            "the generated representation '" + RepresentationGenerator.NAME + "'";

    private final Map<String, Claimants> claimantsByName = new TreeMap<>();

    /** The claimants of one class name: configured types by qualified name, generated classes. */
    private static final class Claimants {
        private final Set<String> types = new LinkedHashSet<>();
        private final Set<String> generated = new LinkedHashSet<>();
    }

    GeneratedNameClashes(final List<TypeModel> packageModels) {
        packageModels.forEach(
                model -> {
                    final String type = model.qualifiedName();
                    // A type in another package than the one generated into occupies no name here.
                    if (model.packageName().equals(model.targetPackage())) {
                        claimType(model.simpleName(), type);
                    }
                    claimType(AssertionGenerator.abstractNameOf(model.simpleName()), type);
                    claimType(AssertionGenerator.concreteNameOf(model.simpleName()), type);
                });
        claimGenerated(SoftAssertionsProviderGenerator.NAME, MIXIN_CLAIMANT);
        claimGenerated(RepresentationGenerator.NAME, REPRESENTATION_CLAIMANT);
    }

    private void claimType(final String name, final String type) {
        claimants(name).types.add(type);
    }

    private void claimGenerated(final String name, final String description) {
        claimants(name).generated.add(description);
    }

    private Claimants claimants(final String name) {
        return claimantsByName.computeIfAbsent(name, key -> new Claimants());
    }

    /** One message per class name that two or more claimants want. */
    List<String> messages(final String targetPackage) {
        final List<String> messages = new ArrayList<>();
        claimantsByName.forEach(
                (name, claimants) -> {
                    if (claimants.types.size() + claimants.generated.size() > 1) {
                        messages.add(message(targetPackage, name, claimants));
                    }
                });
        return messages;
    }

    private static String message(
            final String targetPackage, final String name, final Claimants claimants) {
        final List<String> types = new ArrayList<>(claimants.types);
        final List<String> all = new ArrayList<>();
        if (!types.isEmpty()) {
            all.add(AssertionGenerator.quotedAndJoined(types));
        }
        all.addAll(claimants.generated);
        return "The class name '"
                + name
                + "' of package '"
                + targetPackage
                + "' is needed by "
                + String.join(" and ", all)
                + ", which clashes; "
                + renameAdvice(types.size());
    }

    private static String renameAdvice(final int configuredTypes) {
        if (configuredTypes == 1) {
            return "rename the configured type";
        }
        return configuredTypes == 2 ? "rename one of the types" : "rename all but one of the types";
    }
}
