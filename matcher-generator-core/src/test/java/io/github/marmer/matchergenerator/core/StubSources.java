/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;

/** The trivial Java source the test backends generate. */
final class StubSources {

    private StubSources() {}

    /**
     * An empty public class named like the model's type plus the given suffix, in the model's
     * target package.
     */
    static GeneratedSource emptyClass(final TypeModel model, final String suffix) {
        final String simpleName = model.simpleName() + suffix;
        return new GeneratedSource(
                model.targetPackage(),
                simpleName,
                GeneratedSource.Language.JAVA,
                packageClause(model.targetPackage()) + "public class " + simpleName + " {\n}\n");
    }

    /** A package declaration, or nothing for the default package. */
    private static String packageClause(final String packageName) {
        return packageName.isEmpty() ? "" : "package " + packageName + ";\n";
    }
}
