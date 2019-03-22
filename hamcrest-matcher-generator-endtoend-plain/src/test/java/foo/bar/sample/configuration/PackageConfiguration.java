package foo.bar.sample.configuration;


import io.github.marmer.annotationprocessing.MatcherConfiguration;
import io.github.marmer.annotationprocessing.MatcherConfigurations;

@MatcherConfigurations(
        @MatcherConfiguration({
                "foo.bar.sample.model.SomePojo.InnerClass",
                "foo.bar.sample.model.ParentPojo",
                "foo.bar.sample.model.SomePojoInterface",
                "foo.bar.sample.model.SomeLombokPojo",
                "foo.bar.sample.resourcedir",
                "org.springframework.http.ResponseEntity",
        }))
public class PackageConfiguration {
}
