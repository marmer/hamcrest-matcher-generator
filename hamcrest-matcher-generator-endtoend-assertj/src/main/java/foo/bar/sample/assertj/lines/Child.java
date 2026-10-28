/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.lines;

/** The other half of a cyclic pair: it refers back to its parent. */
public class Child {
    private final String name;
    private final Parent parent;

    public Child(final String name, final Parent parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public Parent getParent() {
        return parent;
    }
}
