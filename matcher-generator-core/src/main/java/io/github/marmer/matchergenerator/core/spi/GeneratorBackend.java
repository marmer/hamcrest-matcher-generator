/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.spi;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import java.util.List;

/**
 * A backend turns the library-free {@link TypeModel} of a configured type into the sources of its
 * generated files, usually one.
 *
 * <p>Backends are discovered via {@link java.util.ServiceLoader}. Every backend found on the
 * processor path runs for every configured type.
 */
public interface GeneratorBackend {

    /**
     * Generates the source files for the given type.
     *
     * @param model the configured type
     * @param context information about the current generation run
     * @return the generated source files, Java or Kotlin, at least one
     */
    List<GeneratedSource> generate(TypeModel model, GenerationContext context);
}
