/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import model.Order
import model.isOrder

/** The control of the compile-failure checks: the same check in the right scope and of the right type. */
fun check(order: Order) = isOrder {
    customer { name("Ada") }
    id(1L)
}
