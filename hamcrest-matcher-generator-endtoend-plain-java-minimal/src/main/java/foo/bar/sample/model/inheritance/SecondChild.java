/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.model.inheritance;

import java.util.UUID;

public class SecondChild extends Parent {
    private final String name;

    public SecondChild(final UUID id, final String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
