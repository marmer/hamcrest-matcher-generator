package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory.MatcherGeneratorConfiguration;
import io.github.marmer.testutils.generators.beanmatcher.generation.MatcherNamingStrategy;
import io.github.marmer.testutils.generators.beanmatcher.generation.PackageMatcherNamingStrategy;
import io.github.marmer.testutils.generators.beanmatcher.generation.ParentMatcherNamingStrategy;
import io.github.marmer.testutils.generators.beanmatcher.generation.PlainMatcherNamingStrategy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.Arrays;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class MatcherNamingStrategyFactoryTest {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    @Parameterized.Parameter(0)
    public String info;
    @Parameterized.Parameter(1)
    public MatcherGeneratorConfiguration configuration;
    @Parameterized.Parameter(2)
    public Class<?> expectedInstance;
    @InjectMocks
    private MatcherNamingStrategyFactory underTest;

    @Parameterized.Parameters(name = "{index}: info={0}, configuration={1}, expectedInstance={2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"PACKAGE", MatcherGeneratorConfiguration.builder().namingStrategy(MatcherNamingStrategy.Name.PACKAGE).build(), PackageMatcherNamingStrategy.class},
                {"PARRENT", MatcherGeneratorConfiguration.builder().namingStrategy(MatcherNamingStrategy.Name.PARENT).build(), ParentMatcherNamingStrategy.class},
                {"PLAIN", MatcherGeneratorConfiguration.builder().namingStrategy(MatcherNamingStrategy.Name.PLAIN).build(), PlainMatcherNamingStrategy.class},
                {"Nothing", MatcherGeneratorConfiguration.builder().namingStrategy(null).build(), PackageMatcherNamingStrategy.class},
                {"Not even a configuration", null, PackageMatcherNamingStrategy.class}
        });
    }

    @Test
    public void testStrategyFor_NatcherNamingStrategyTypeGiven_ShouldReturnMatchingInstance()
            throws Exception {
        // Execution
        final MatcherNamingStrategy result = underTest.strategyFor(configuration);

        // Assertion
        assertThat(result, is(instanceOf(expectedInstance)));
    }
}