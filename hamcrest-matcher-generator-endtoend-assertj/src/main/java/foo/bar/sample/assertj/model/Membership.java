/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.model;

/**
 * A membership with a name like its members have, two properties that can hold the same customer
 * instance and one value that is equal but not identical to another.
 */
public class Membership {
    private final String name;
    private final Customer member;
    private final Customer billingMember;
    private final Badge badge;

    public Membership(
            final String name,
            final Customer member,
            final Customer billingMember,
            final Badge badge) {
        this.name = name;
        this.member = member;
        this.billingMember = billingMember;
        this.badge = badge;
    }

    public String getName() {
        return name;
    }

    public Customer getMember() {
        return member;
    }

    public Customer getBillingMember() {
        return billingMember;
    }

    public Badge getBadge() {
        return badge;
    }
}
