/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.model

/** A nested type with the simple name of the top level [Customer] of the same package. */
data class NestedClash(
    val customer: Customer,
    val inner: Customer,
) {
    data class Customer(val label: String)
}
