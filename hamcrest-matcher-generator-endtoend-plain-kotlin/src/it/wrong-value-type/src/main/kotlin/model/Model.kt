/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package model

data class Customer(val name: String)

data class Order(val id: Long, val customer: Customer)
