package io.github.marmer.annotationprocessing;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Supplier;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.github.marmer.annotationprocessing.MatcherConfiguration")
@AutoService(Processor.class)
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
        worker = new MatcherGenerationProcessorWorker(timeProvider::get, processingEnv, getClass().getName());
    }

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment) {
        return worker.process(set, roundEnvironment);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return worker.getSupportedSourceVersion();
    }
}
