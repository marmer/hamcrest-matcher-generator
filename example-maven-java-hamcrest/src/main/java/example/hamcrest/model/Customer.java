/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.hamcrest.model;

public class Customer {
    private final String name;
    private final Address address;
    private final boolean active;

    public Customer(final String name, final Address address, final boolean active) {
        this.name = name;
        this.address = address;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }
}
