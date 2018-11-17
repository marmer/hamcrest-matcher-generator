package io.github.marmer.testutils.generators.beanmatcher.generation;

import java.util.Optional;

public class ParentMatcherNamingStrategy implements MatcherNamingStrategy {
    @Override
    public Optional<String> packageFor(final Class<?> type) {
        return Optional.ofNullable(type)
                .map(t -> type.getPackage().getName());
    }

    @Override
    public Optional<String> typeNameFor(final Class<?> type) {
        return type == null
                ? Optional.empty()
                : Optional.of(enclosingPartsOf(type) + type.getSimpleName() + "Matcher");
    }

    private String enclosingPartsOf(final Class<?> type) {
        return type.isMemberClass()
                ? enclosingPartsOf(type.getEnclosingClass()) + type.getEnclosingClass().getSimpleName() + "$"
                : "";
    }
}
