package io.github.marmer.testutils.generators.beanmatcher.processing;

import java.util.List;


/**
 * Filter for classes which are "illegal" in some way.
 *
 * @author marmer
 * @since  20.07.2017
 */
public interface IllegalClassFilter {
    /**
     * Filter.
     *
     * @param baseClassList the base class list
     */
    List<Class<?>> filter(List<Class<?>> baseClassList);
}
