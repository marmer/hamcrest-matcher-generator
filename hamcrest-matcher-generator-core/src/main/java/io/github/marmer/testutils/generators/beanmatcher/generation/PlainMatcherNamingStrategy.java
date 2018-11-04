package io.github.marmer.testutils.generators.beanmatcher.generation;

import java.util.Optional;

/**
 * Matchers are named pretty like the classes they have to match.
 */
public class PlainMatcherNamingStrategy implements MatcherNamingStrategy {
    @Override
    public Optional<String> packageFor(final Class<?> type) {
        return Optional.ofNullable(type)
                .map(t -> type.getPackage().getName());
    }

    @Override
    public Optional<String> typeNameFor(final Class<?> type) {

        return Optional.of(type.getSimpleName() + "Matcher");

    }
}
