/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

/**
 * A matcher with an accessor and the bean it matches, as a generated matcher has them. {@link
 * PropertyAccessRetentionTest} loads this class in a throw-away class loader, so the accessor's
 * lambda belongs to that loader.
 */
public final class RetentionFixture {
    private RetentionFixture() {}

    public static Object newMatcher() {
        return new BeanPropertyMatcher<>(
                Bean.class, BeanPropertyMatcher.property("name", (Bean bean) -> bean.name()));
    }

    public static final class Bean {
        public String name() {
            return "bean";
        }
    }
}
