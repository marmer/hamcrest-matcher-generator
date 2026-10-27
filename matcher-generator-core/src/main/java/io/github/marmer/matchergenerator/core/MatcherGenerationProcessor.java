package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.spi.GeneratorBackend;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({
    MatcherGenerationProcessorWorker.GENERATE_MATCHERS,
    MatcherGenerationProcessorWorker.MATCHER_CONFIGURATION
})
public class MatcherGenerationProcessor extends AbstractProcessor {

    private final Supplier<LocalDateTime> timeProvider;
    private MatcherGenerationProcessorWorker worker;

    public MatcherGenerationProcessor() {
        this(LocalDateTime::now);
    }

    public MatcherGenerationProcessor(final Supplier<LocalDateTime> timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        worker = new MatcherGenerationProcessorWorker(
            timeProvider::get,
            processingEnv,
            getClass().getName(),
            loadBackends()
        );
    }

    private List<GeneratorBackend> loadBackends() {
        final List<GeneratorBackend> backends = new ArrayList<>();
        ServiceLoader.load(GeneratorBackend.class, getClass().getClassLoader()).forEach(backends::add);
        return backends;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment) {
        return worker.process(set, roundEnvironment);
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(
            MatcherGenerationProcessorWorker.KAPT_KOTLIN_GENERATED,
            MatcherGenerationProcessorWorker.VERBOSE_OPTION
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return worker.getSupportedSourceVersion();
    }
}
