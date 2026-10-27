package foo.bar.sample.records.configuration;

import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

@GenerateMatchers({
    "foo.bar.sample.records.model.PersonRecord",
    "foo.bar.sample.records.model.AddressRecord",
    "foo.bar.sample.records.model.CustomerRecord",
    "foo.bar.sample.records.model.OrderRecord"
})
public final class RecordsConfiguration {}
