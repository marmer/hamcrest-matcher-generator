/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.model.inheritance;

import java.util.List;

public class Main {
    private final List<Parent> parents;

    public Main(final List<Parent> parents) {
        this.parents = parents;
    }

    public List<Parent> getParents() {
        return parents;
    }
}
