/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.model

data class Order(val id: Long, val customer: Customer, val note: String?)

data class Customer(val name: String, val address: Address)

data class Address(val city: String)
