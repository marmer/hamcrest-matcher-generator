package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory;
import io.github.marmer.testutils.generators.beanmatcher.generation.MatcherNamingStrategy;
import io.github.marmer.testutils.generators.beanmatcher.generation.PackageMatcherNamingStrategy;
import io.github.marmer.testutils.generators.beanmatcher.generation.ParentMatcherNamingStrategy;
import io.github.marmer.testutils.generators.beanmatcher.generation.PlainMatcherNamingStrategy;

import static io.github.marmer.testutils.generators.beanmatcher.generation.MatcherNamingStrategy.Name.PARENT;
import static io.github.marmer.testutils.generators.beanmatcher.generation.MatcherNamingStrategy.Name.PLAIN;

class MatcherNamingStrategyFactory {
    MatcherNamingStrategy strategyFor(final MatcherGeneratorFactory.MatcherGeneratorConfiguration matcherGeneratorConfiguration) {
        final MatcherNamingStrategy.Name namingStrategy = namingStrategyFrom(matcherGeneratorConfiguration);

        if (PARENT == namingStrategy) {
            return new ParentMatcherNamingStrategy();
        } else if (PLAIN == namingStrategy) {
            return new PlainMatcherNamingStrategy();
        } else {
            return new PackageMatcherNamingStrategy();
        }
    }

    private MatcherNamingStrategy.Name namingStrategyFrom(final MatcherGeneratorFactory.MatcherGeneratorConfiguration matcherGeneratorConfiguration) {
        return matcherGeneratorConfiguration == null
                ? null
                : matcherGeneratorConfiguration.getNamingStrategy();
    }
}
