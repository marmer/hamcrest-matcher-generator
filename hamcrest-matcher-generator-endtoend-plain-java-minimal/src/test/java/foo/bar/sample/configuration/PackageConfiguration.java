package foo.bar.sample.configuration;


import io.github.marmer.annotationprocessing.MatcherConfiguration;

@MatcherConfiguration(
    value = {
        "foo.bar.sample.model.SomePojoInterface",
        "foo.bar.sample.model.SomeLombokPojo",
        "foo.bar.sample.resourcedir",
        "foo.bar.sample.model.GeneratedByJavaPoetMatcherGeneratorType",
        "foo.bar.sample.model.GeneratedBySomethingElse",
        "foo.bar.sample.model.inheritance",
        "foo.bar.sample.model"
    },
    exclude = {
        "foo.bar.sample.model.ExcludedPojo"
    })
public class PackageConfiguration {

}
