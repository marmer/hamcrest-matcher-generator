package foo.bar.sample.records.configuration;

import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

@GenerateMatchers(
    value = "foo.bar.sample.records.model",
    exclude = "foo.bar.sample.records.model.HiddenRecord"
)
public final class RecordsConfiguration {}
