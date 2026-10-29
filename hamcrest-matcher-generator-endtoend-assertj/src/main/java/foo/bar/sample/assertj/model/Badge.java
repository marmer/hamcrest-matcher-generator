/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.model;

import java.util.Objects;

/** A value that is equal to another badge with the same label, but not identical to it. */
public class Badge {
    private final String label;

    public Badge(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Badge badge && Objects.equals(label, badge.label);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(label);
    }
}
