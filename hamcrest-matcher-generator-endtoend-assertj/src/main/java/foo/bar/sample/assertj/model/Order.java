/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.model;

import java.math.BigDecimal;
import java.util.List;

public class Order {
    private final long id;
    private final Customer customer;
    private final BigDecimal total;
    private final List<String> tags;
    private final boolean paid;

    public Order(
            final long id,
            final Customer customer,
            final BigDecimal total,
            final List<String> tags,
            final boolean paid) {
        this.id = id;
        this.customer = customer;
        this.total = total;
        this.tags = tags;
        this.paid = paid;
    }

    public long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean isPaid() {
        return paid;
    }
}
