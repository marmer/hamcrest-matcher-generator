package foo.bar.sample.prodonly.model;

import static foo.bar.sample.prodonly.model.SomeProdOnlyModelMatcher.isSomeProdOnlyModel;
import static org.hamcrest.MatcherAssert.assertThat;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SomeProdOnlyModelMatcherTest {

    @Test
    @DisplayName("Prod Matcher can be generated if matcher configuration is located nearby production sources")
    @SneakyThrows
    void test_ProdMatcherCanBeGeneratedIfMatcherConfigurationIsLocatedNearbyProductionSources() {
        assertThat(new SomeProdOnlyModel() {
        }, isSomeProdOnlyModel());
    }

}
