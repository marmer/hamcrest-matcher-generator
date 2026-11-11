/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.model;

/** A public type with a property of the package-private type {@link PackagePrivatePojo}. */
public final class PackagePrivateHolder {
    private final PackagePrivatePojo pojo;

    public PackagePrivateHolder(final PackagePrivatePojo pojo) {
        this.pojo = pojo;
    }

    public PackagePrivatePojo getPojo() {
        return pojo;
    }
}
