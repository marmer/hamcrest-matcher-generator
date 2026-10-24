// PROTOTYPE. K2: the Java generator also emits Consumer overloads (isOrder(Consumer), withCustomer(Consumer) for generated types).
// No Kotlin artifact; Java users get the same lambdas.
package proto.demo.k2

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
    assertThat(ADA, isPerson { it.withName("Bob").withAge(36).withEmail(startsWith("bob@")) })
    // @end
}

fun s2() {
    // @code
    assertThat(ORDER, isOrder {
        it.withId(42L).withTotal(BigDecimal("10.00"))
            .withCustomer { c -> c.withName("Ada").withAddress { a -> a.withCity("Berlin") } }
    })
    // @end
}

fun s3() {
    // @code
    assertThat(PARIS, isAddress { it.withCity("Berlin").withZip("10115") })
    // @end
}

fun s4() {
    // @code
    assertThat(ORDER_WITHOUT_CUSTOMER, isOrder { it.withCustomer { c -> c.withName("Ada") } })
    // @end
}

fun s5() {
    // @code
    assertThat(CART, isCart {
        it.withLines(itemsInOrder(
            isOrderLine { l -> l.withSku("A-1").withQuantity(1) },
            isOrderLine { l -> l.withSku("B-2").withQuantity(3) }))
    })
    // @end
}

fun s6() {
    // @code
    assertThat(WISHLIST, isWishlist {
        it.withItems(itemsInAnyOrder(
            isOrderLine { l -> l.withSku("A-1").withQuantity(1) },
            isOrderLine { l -> l.withSku("B-2").withQuantity(3) },
            isOrderLine { l -> l.withSku("C-3").withQuantity(1) }))
    })
    // @end
}

fun s7() {
    // @code
    assertThat(INVENTORY, isInventory {
        it.withBySku(entries<String, OrderLine>(mapOf(
            "A-1" to isOrderLine { l -> l.withQuantity(1) },
            "B-2" to isOrderLine { l -> l.withQuantity(3) })))
    })
    // @end
}

fun s8() {
    // @code
    assertThat(ADA, isPerson {
        it.checkingAllProperties().withName("Bob").withAge(36).ignoringEmail()
    })
    // @end
}

fun s9() {
    // @code
    assertThat(ADA_WITHOUT_PHONE, isPerson { it.withName("Ada").withPhone(null) })
    // @end
}

fun s10() {
    // @code
    assertThat(PARIS, isAddress { it.checkingAllProperties().withCity("Paris").withStreet("1 Rue de Rivoli") })
    // @end
}
