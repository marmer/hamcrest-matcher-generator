/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.entries
import io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInAnyOrder
import io.github.marmer.matchergenerator.hamcrest.CollectionMatchers.itemsInOrder
import io.github.marmer.testutils.model.Cart
import io.github.marmer.testutils.model.Inventory
import io.github.marmer.testutils.model.OrderLine
import io.github.marmer.testutils.model.Shelf
import io.github.marmer.testutils.model.Wishlist
import io.github.marmer.testutils.model.isCart
import io.github.marmer.testutils.model.isInventory
import io.github.marmer.testutils.model.isOrderLine
import io.github.marmer.testutils.model.isShelf
import io.github.marmer.testutils.model.isWishlist
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class KotlinCollectionsTest {
    private val cart = Cart(
        listOf(OrderLine("A-1", 1), OrderLine("B-2", 3)),
        setOf("SUMMER", "WINTER"),
    )

    @Test
    @DisplayName("A list property takes itemsInOrder with an entry function per item")
    fun test_ListWithItemsInOrder() {
        assertThat(cart, isCart {
            lines(itemsInOrder(
                isOrderLine { sku("A-1"); quantity(1) },
                isOrderLine { sku("B-2"); quantity(3) },
            ))
        })
    }

    @Test
    @DisplayName("itemsInOrder fails for the same items in another order")
    fun test_ListWithItemsInOrderFailsForAnotherOrder() {
        assertThrows(AssertionError::class.java) {
            assertThat(cart, isCart {
                lines(itemsInOrder(
                    isOrderLine { sku("B-2"); quantity(3) },
                    isOrderLine { sku("A-1"); quantity(1) },
                ))
            })
        }
    }

    @Test
    @DisplayName("A set property takes itemsInAnyOrder, every matcher needs an item of its own")
    fun test_SetWithItemsInAnyOrder() {
        assertThat(cart, isCart {
            coupons(itemsInAnyOrder(equalTo("WINTER"), equalTo("SUMMER")))
        })
        assertThrows(AssertionError::class.java) {
            assertThat(cart, isCart {
                coupons(itemsInAnyOrder(equalTo("WINTER"), equalTo("WINTER")))
            })
        }
    }

    @Test
    @DisplayName("A collection of generated types takes itemsInAnyOrder of entry functions")
    fun test_CollectionOfGeneratedTypesWithItemsInAnyOrder() {
        val wishlist = Wishlist(setOf(OrderLine("A-1", 1), OrderLine("B-2", 3)), emptyMap())

        assertThat(wishlist, isWishlist {
            items(itemsInAnyOrder(
                isOrderLine { sku("B-2"); quantity(3) },
                isOrderLine { sku("A-1"); quantity(1) },
            ))
        })
    }

    @Test
    @DisplayName("A map property takes entries with its keys and an entry function per value")
    fun test_MapWithEntries() {
        val inventory = Inventory(mapOf("A-1" to OrderLine("A-1", 1), "B-2" to OrderLine("B-2", 3)))

        assertThat(inventory, isInventory {
            bySku(entries(mapOf(
                "A-1" to isOrderLine { quantity(1) },
                "B-2" to isOrderLine { quantity(3) },
            )))
        })
        assertThat(inventory, isInventory {
            bySku(entries("A-1", isOrderLine { sku("A-1") }, "B-2", isOrderLine { quantity(3) }))
        })
    }

    @Test
    @DisplayName("entries fails for a missing key and for an unexpected key")
    fun test_MapWithEntriesFailsForOtherKeys() {
        val inventory = Inventory(mapOf("A-1" to OrderLine("A-1", 1), "B-2" to OrderLine("B-2", 3)))

        assertThrows(AssertionError::class.java) {
            assertThat(inventory, isInventory {
                bySku(entries(mapOf("A-1" to isOrderLine { quantity(1) })))
            })
        }
        assertThrows(AssertionError::class.java) {
            assertThat(inventory, isInventory {
                bySku(entries(mapOf(
                    "A-1" to isOrderLine { quantity(1) },
                    "B-2" to isOrderLine { quantity(3) },
                    "C-3" to isOrderLine { quantity(1) },
                )))
            })
        }
    }

    @Test
    @DisplayName("A map of values that are no generated type takes entries with plain matchers")
    fun test_MapWithPlainValues() {
        val wishlist = Wishlist(emptySet(), mapOf("en" to "Hello", "de" to "Hallo"))

        assertThat(wishlist, isWishlist {
            notes(entries("de", equalTo("Hallo"), "en", equalTo("Hello")))
        })
    }

    @Test
    @DisplayName("Arrays take the collection matchers like lists, primitive arrays included")
    fun test_ArraysWithCollectionMatchers() {
        val shelf = Shelf(arrayOf("oak", "birch"), intArrayOf(2, 5))

        assertThat(shelf, isShelf {
            names(itemsInOrder(equalTo("oak"), equalTo("birch")))
            counts(itemsInAnyOrder(equalTo(5), equalTo(2)))
        })
        assertThrows(AssertionError::class.java) {
            assertThat(shelf, isShelf {
                names(itemsInAnyOrder(equalTo("oak")))
            })
        }
    }

    @Test
    @DisplayName("A collection mismatch reads the same as with the Java matcher")
    fun test_CollectionMismatchMessageIsTheOneOfTheJavaMatcher() {
        val dsl = isCart {
            lines(itemsInOrder(isOrderLine { sku("B-2") }))
        }
        val java = io.github.marmer.testutils.model.CartMatcher.isCart().withLines(
            itemsInOrder(io.github.marmer.testutils.model.OrderLineMatcher.isOrderLine().withSku("B-2"))
        )

        val error = assertThrows(AssertionError::class.java) { assertThat(cart, dsl) }

        assertEquals(
            assertThrows(AssertionError::class.java) { assertThat(cart, java) }.message,
            error.message,
        )
    }
}
