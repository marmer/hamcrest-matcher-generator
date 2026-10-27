/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import org.jspecify.annotations.NullMarked;

/**
 * Implemented by every generated matcher, so that a generated matcher used as a property check of
 * another one can be flattened into property paths when a mismatch is described.
 */
@NullMarked
public interface GeneratedBeanMatcher {

    /**
     * @return the runtime matcher this generated matcher delegates to.
     */
    BeanPropertyMatcher<?> beanPropertyMatcher();
}
