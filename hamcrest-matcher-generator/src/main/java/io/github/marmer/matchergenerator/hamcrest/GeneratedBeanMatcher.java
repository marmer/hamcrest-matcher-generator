package io.github.marmer.matchergenerator.hamcrest;

/**
 * Implemented by every generated matcher, so that a generated matcher used as a property check of another one can be
 * flattened into property paths when a mismatch is described.
 */
public interface GeneratedBeanMatcher {

    /**
     * @return the runtime matcher this generated matcher delegates to.
     */
    BeanPropertyMatcher<?> beanPropertyMatcher();
}
