/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

/** A package-private type with a public getter: its matcher must read it from its own package. */
final class PackagePrivatePojo {
    private final String name;

    PackagePrivatePojo(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
