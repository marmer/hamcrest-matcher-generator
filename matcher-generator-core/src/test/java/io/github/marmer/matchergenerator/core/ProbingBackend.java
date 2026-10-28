/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import io.github.marmer.matchergenerator.core.spi.GeneratedSource;
import io.github.marmer.matchergenerator.core.spi.GenerationContext;
import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A backend that acts only on a type named {@code Probed}: it reports one compiler error for it and
 * records what the context tells about inherited methods. Other types get nothing.
 */
public class ProbingBackend implements GeneratorBackend {
    static final String PROBED = "Probed";
    static final String ERROR = "Probe reports an error";
    private static final List<Set<String>> INHERITED = new CopyOnWriteArrayList<>();
    private static final List<Set<String>> MISSING = new CopyOnWriteArrayList<>();

    static List<Set<String>> inheritedOfObject() {
        return List.copyOf(INHERITED);
    }

    static List<Set<String>> inheritedOfMissingClass() {
        return List.copyOf(MISSING);
    }

    static void reset() {
        INHERITED.clear();
        MISSING.clear();
    }

    @Override
    public List<GeneratedSource> generate(final TypeModel model, final GenerationContext context) {
        if (!model.simpleName().equals(PROBED)) {
            return List.of();
        }
        INHERITED.add(context.inheritedMethodNames("java.lang.Object"));
        MISSING.add(context.inheritedMethodNames("no.such.Class"));
        context.reportError(ERROR + " for " + model.simpleName());
        return List.of(StubSources.emptyClass(model, "Probe"));
    }
}
