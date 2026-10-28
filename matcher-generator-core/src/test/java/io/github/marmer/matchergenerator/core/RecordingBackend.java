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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test backend registered via {@link java.util.ServiceLoader} that records every model it receives.
 * It is the seam for everything the core decides.
 */
public class RecordingBackend implements GeneratorBackend {
    private static final List<TypeModel> MODELS = new CopyOnWriteArrayList<>();
    private static final List<GenerationContext> CONTEXTS = new CopyOnWriteArrayList<>();

    static List<TypeModel> models() {
        return List.copyOf(MODELS);
    }

    static List<GenerationContext> contexts() {
        return List.copyOf(CONTEXTS);
    }

    static void reset() {
        MODELS.clear();
        CONTEXTS.clear();
    }

    @Override
    public List<GeneratedSource> generate(final TypeModel model, final GenerationContext context) {
        MODELS.add(model);
        CONTEXTS.add(context);
        return List.of(StubSources.emptyClass(model, "Recorded"));
    }
}
