/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import model.Order
import model.isOrder

/** A check of the order's id written into the customer's scope: the outer scope is not in reach. */
fun check(order: Order) = isOrder {
    customer { id(1L) }
}
