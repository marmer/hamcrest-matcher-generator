/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.model

/** Two properties of types with the same simple name, from two packages. */
data class Shipment(
    val customer: Customer,
    val receiver: io.github.marmer.testutils.model.other.Customer,
)
