/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({
    MatcherGenerationProcessorWorker.GENERATE_MATCHERS,
    MatcherGenerationProcessorWorker.MATCHER_CONFIGURATION,
    MatcherGenerationProcessorWorker.GENERATED,
    MatcherGenerationProcessorWorker.NULL_MARKED
})
public class MatcherGenerationProcessor extends AbstractProcessor {

    private MatcherGenerationProcessorWorker worker;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        worker =
                new MatcherGenerationProcessorWorker(
                        processingEnv, getClass().getName(), loadBackends());
    }

    private List<GeneratorBackend> loadBackends() {
        final List<GeneratorBackend> backends = new ArrayList<>();
        ServiceLoader.load(GeneratorBackend.class, getClass().getClassLoader())
                .forEach(backends::add);
        return backends;
    }

    @Override
    public boolean process(
            final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment) {
        return worker.process(set, roundEnvironment);
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(GeneratedSourceWriter.KAPT_KOTLIN_GENERATED, ElementSupport.VERBOSE_OPTION);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return worker.getSupportedSourceVersion();
    }
}
