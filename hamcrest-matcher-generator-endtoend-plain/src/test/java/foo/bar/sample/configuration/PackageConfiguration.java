package foo.bar.sample.configuration;


import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.MatcherConfigurations;

@MatcherConfigurations(@MatcherConfiguration("foo.bar.sample.model.SomePojo"))
public class PackageConfiguration {
}
