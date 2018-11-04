package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory;
import io.github.marmer.testutils.generators.beanmatcher.generation.MatcherNamingStrategy;
import io.github.marmer.testutils.generators.beanmatcher.generation.PlainMatcherNamingStrategy;

class MatcherNamingStrategyFactory {
    MatcherNamingStrategy strategyFor(final MatcherGeneratorFactory.MatcherGeneratorConfiguration matcherGeneratorConfiguration) {
        switch (matcherGeneratorConfiguration.getNamingStrategy()) {
            case PACKAGE:
                // TODO: marmer 04.11.2018 comming soon
            case PARENT:
                // TODO: marmer 04.11.2018 comming soon
            case PLAIN:
            default:
                return new PlainMatcherNamingStrategy();
        }
    }
}
