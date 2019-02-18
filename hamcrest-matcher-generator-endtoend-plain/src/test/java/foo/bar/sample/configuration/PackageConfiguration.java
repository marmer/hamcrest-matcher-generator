package foo.bar.sample.configuration;


import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.MatcherConfigurations;

@MatcherConfigurations(@MatcherConfiguration({"foo.bar.sample.model.SomePojo", "foo.bar.sample.model.ParentPojo"}))
public class PackageConfiguration {
}
