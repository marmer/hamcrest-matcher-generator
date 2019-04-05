package io.github.marmer.annotationprocessing.extraction;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
final class ListUtil {
    /**
     * Merges two collections into one single list.
     * <p>
     * A fresh list is allways returned.
     *
     * @param collections the collections to merge
     * @param <T>         Content type of the collection
     * @return The merged collection
     */
    @SafeVarargs
    static <T> List<T> joinToList(final Collection<T>... collections) {
        return Stream.of(collections).flatMap(Collection::stream).collect(Collectors.toList());
    }
}