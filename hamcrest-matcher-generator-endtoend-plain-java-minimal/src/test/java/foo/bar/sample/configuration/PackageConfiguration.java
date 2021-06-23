package foo.bar.sample.configuration;


import io.github.marmer.testutils.generators.beanmatcher.dependencies.MatcherConfiguration;

@MatcherConfiguration({
    "foo.bar.sample.model.SomePojoInterface",
    "foo.bar.sample.model.SomeLombokPojo",
    "foo.bar.sample.resourcedir",
    "foo.bar.sample.model.GeneratedByJavaPoetMatcherGeneratorType",
    "foo.bar.sample.model.GeneratedBySomethingElse",
    "foo.bar.sample.model.inheritance",
    "foo.bar.sample.model"
})
public class PackageConfiguration {

}
