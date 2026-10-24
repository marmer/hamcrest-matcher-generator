// PROTOTYPE. K0: the generated Java API used from Kotlin as is (no DSL, nothing new to ship).
package proto.demo.k0

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
    assertThat(ADA, isPerson().withName("Bob").withAge(36).withEmail(startsWith("bob@")))
    // @end
}

fun s2() {
    // @code
    assertThat(ORDER, isOrder().withId(42L).withTotal(BigDecimal("10.00"))
        .withCustomer(isCustomer().withName("Ada")
            .withAddress(isAddress().withCity("Berlin"))))
    // @end
}

fun s3() {
    // @code
    assertThat(PARIS, isAddress().withCity("Berlin").withZip("10115"))
    // @end
}

fun s4() {
    // @code
    assertThat(ORDER_WITHOUT_CUSTOMER, isOrder().withCustomer(isCustomer().withName("Ada")))
    // @end
}

fun s5() {
    // @code
    assertThat(CART, isCart().withLines(itemsInOrder(
        isOrderLine().withSku("A-1").withQuantity(1),
        isOrderLine().withSku("B-2").withQuantity(3))))
    // @end
}

fun s6() {
    // @code
    assertThat(WISHLIST, isWishlist().withItems(itemsInAnyOrder(
        isOrderLine().withSku("A-1").withQuantity(1),
        isOrderLine().withSku("B-2").withQuantity(3),
        isOrderLine().withSku("C-3").withQuantity(1))))
    // @end
}

fun s7() {
    // @code
    assertThat(INVENTORY, isInventory().withBySku(entries<String, OrderLine>(mapOf(
        "A-1" to isOrderLine().withQuantity(1),
        "B-2" to isOrderLine().withQuantity(3)))))
    // @end
}

fun s8() {
    // @code
    assertThat(ADA, isPerson().checkingAllProperties()
        .withName("Bob").withAge(36).ignoringEmail())
    // @end
}

fun s9() {
    // @code
    assertThat(ADA_WITHOUT_PHONE, isPerson().withName("Ada").withPhone(null))
    // @end
}
fun s10() {
    // @code
    assertThat(PARIS, isAddress().checkingAllProperties().withCity("Paris").withStreet("1 Rue de Rivoli"))
    // @end
}
