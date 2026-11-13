/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import model.isOrder

/** A value of the wrong type: the order's id is a number, not a text. */
fun check() = isOrder {
    id("forty-two")
}
