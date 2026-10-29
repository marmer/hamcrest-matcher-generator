/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.kotlin.model

data class Customer(val name: String, val address: Address, val active: Boolean)

data class Address(val city: String)
