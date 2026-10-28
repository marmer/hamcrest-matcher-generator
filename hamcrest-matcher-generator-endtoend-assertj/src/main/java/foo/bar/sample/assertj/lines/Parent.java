/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.lines;

/** One half of a cyclic pair: the child refers back to its parent. */
public class Parent {
    private Child child;

    public Child getChild() {
        return child;
    }

    public void setChild(final Child child) {
        this.child = child;
    }
}
