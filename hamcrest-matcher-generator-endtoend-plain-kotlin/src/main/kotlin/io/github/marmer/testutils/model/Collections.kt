/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.model

data class OrderLine(val sku: String, val quantity: Int)

data class Cart(val lines: List<OrderLine>, val coupons: Set<String>)

data class Wishlist(val items: Set<OrderLine>, val notes: Map<String, String>)

data class Inventory(val bySku: Map<String, OrderLine>)

data class Shelf(val names: Array<String>, val counts: IntArray)

data class Keywords(val `in`: String, val `object`: String)
