// PROTOTYPE. K1: no published Kotlin code; the docs show the stdlib apply { } idiom on the generated Java API.
package proto.demo.k1

import java.math.BigDecimal
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.startsWith
import proto.demo.*
import proto.hamcrest.AddressMatcher.isAddress
import proto.hamcrest.CartMatcher.isCart
import proto.hamcrest.CustomerMatcher.isCustomer
import proto.hamcrest.InventoryMatcher.isInventory
import proto.hamcrest.OrderLineMatcher.isOrderLine
import proto.hamcrest.OrderMatcher.isOrder
import proto.hamcrest.PersonMatcher.isPerson
import proto.hamcrest.PropertyMatcher.entries
import proto.hamcrest.PropertyMatcher.itemsInAnyOrder
import proto.hamcrest.PropertyMatcher.itemsInOrder
import proto.hamcrest.WishlistMatcher.isWishlist
import proto.model.OrderLine

fun s1() {
    // @code
    assertThat(ADA, isPerson().apply {
        withName("Bob"); withAge(36); withEmail(startsWith("bob@"))
    })
    // @end
}

fun s2() {
    // @code
    assertThat(ORDER, isOrder().apply {
        withId(42L)
        withTotal(BigDecimal("10.00"))
        withCustomer(isCustomer().apply {
            withName("Ada")
            withAddress(isAddress().apply { withCity("Berlin") })
        })
    })
    // @end
}

fun s3() {
    // @code
    assertThat(PARIS, isAddress().apply { withCity("Berlin"); withZip("10115") })
    // @end
}

fun s4() {
    // @code
    assertThat(ORDER_WITHOUT_CUSTOMER, isOrder().apply {
        withCustomer(isCustomer().apply { withName("Ada") })
    })
    // @end
}

fun s5() {
    // @code
    assertThat(CART, isCart().apply {
        withLines(itemsInOrder(
            isOrderLine().apply { withSku("A-1"); withQuantity(1) },
            isOrderLine().apply { withSku("B-2"); withQuantity(3) }))
    })
    // @end
}

fun s6() {
    // @code
    assertThat(WISHLIST, isWishlist().apply {
        withItems(itemsInAnyOrder(
            isOrderLine().apply { withSku("A-1"); withQuantity(1) },
            isOrderLine().apply { withSku("B-2"); withQuantity(3) },
            isOrderLine().apply { withSku("C-3"); withQuantity(1) }))
    })
    // @end
}

fun s7() {
    // @code
    assertThat(INVENTORY, isInventory().apply {
        withBySku(entries<String, OrderLine>(mapOf(
            "A-1" to isOrderLine().apply { withQuantity(1) },
            "B-2" to isOrderLine().apply { withQuantity(3) })))
    })
    // @end
}

fun s8() {
    // @code
    assertThat(ADA, isPerson().apply {
        checkingAllProperties()
        withName("Bob"); withAge(36); ignoringEmail()
    })
    // @end
}

fun s9() {
    // @code
    assertThat(ADA_WITHOUT_PHONE, isPerson().apply { withName("Ada"); withPhone(null) })
    // @end
}

fun s10() {
    // @code
    assertThat(PARIS, isAddress().apply { checkingAllProperties(); withCity("Paris"); withStreet("1 Rue de Rivoli") })
    // @end
}
