package foo.bar.sample.configuration;


import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

@GenerateMatchers({
    "foo.bar.sample.model.SomePojoInterface",
    "foo.bar.sample.model.SomeLombokPojo",
    "foo.bar.sample.model.GeneratedByJavaPoetMatcherGeneratorType",
    "foo.bar.sample.model.inheritance",
    "foo.bar.sample.model"
})
public class PackageConfiguration {

}
