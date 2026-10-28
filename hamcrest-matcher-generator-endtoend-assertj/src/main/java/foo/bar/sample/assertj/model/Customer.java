/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.model;

public class Customer {
    private final String name;
    private final String phone;
    private final Address address;

    public Customer(final String name, final String phone, final Address address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Address getAddress() {
        return address;
    }
}
