package io.github.marmer.testutils.generators.beanmatcher.generation;

import java.util.Optional;

public class PackageMatcherNamingStrategy implements MatcherNamingStrategy {
    @Override
    public Optional<String> packageFor(final Class<?> type) {
        return type == null
                ? Optional.empty()
                : Optional.of(type.getPackage().getName() + enclosingPartsOf(type));

    }

    private String enclosingPartsOf(final Class<?> type) {
        return !type.isMemberClass()
                ? ""
                : enclosingPartsOf(type.getEnclosingClass()) + "." + type.getEnclosingClass().getSimpleName().toLowerCase();
    }

    @Override
    public Optional<String> typeNameFor(final Class<?> type) {
        return type == null
                ? Optional.empty()
                : Optional.of(type.getSimpleName());

    }
}
