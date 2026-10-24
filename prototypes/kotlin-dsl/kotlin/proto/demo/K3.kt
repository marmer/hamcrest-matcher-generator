// PROTOTYPE. K3: separate Kotlin artifact with a per-type wrapper (receiver lambdas, Kotlin names, @DslMarker).
package proto.demo.k3

import java.math.BigDecimal
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.startsWith
import proto.demo.*
import proto.dsl.k3.*
import proto.hamcrest.PropertyMatcher.entries
import proto.hamcrest.PropertyMatcher.itemsInAnyOrder
import proto.hamcrest.PropertyMatcher.itemsInOrder
import proto.model.OrderLine

fun s1() {
    // @code
    assertThat(ADA, isPerson { name("Bob"); age(36); email(startsWith("bob@")) })
    // @end
}

fun s2() {
    // @code
    assertThat(ORDER, isOrder {
        id(42L)
        total(BigDecimal("10.00"))
        customer {
            name("Ada")
            address { city("Berlin") }
        }
    })
    // @end
}

fun s3() {
    // @code
    assertThat(PARIS, isAddress { city("Berlin"); zip("10115") })
    // @end
}

fun s4() {
    // @code
    assertThat(ORDER_WITHOUT_CUSTOMER, isOrder { customer { name("Ada") } })
    // @end
}

fun s5() {
    // @code
    assertThat(CART, isCart {
        lines(itemsInOrder(
            isOrderLine { sku("A-1"); quantity(1) },
            isOrderLine { sku("B-2"); quantity(3) }))
    })
    // @end
}

fun s6() {
    // @code
    assertThat(WISHLIST, isWishlist {
        items(itemsInAnyOrder(
            isOrderLine { sku("A-1"); quantity(1) },
            isOrderLine { sku("B-2"); quantity(3) },
            isOrderLine { sku("C-3"); quantity(1) }))
    })
    // @end
}

fun s7() {
    // @code
    assertThat(INVENTORY, isInventory {
        bySku(entries<String, OrderLine>(mapOf(
            "A-1" to isOrderLine { quantity(1) },
            "B-2" to isOrderLine { quantity(3) })))
    })
    // @end
}

fun s8() {
    // @code
    assertThat(ADA, isPerson {
        checkingAllProperties()
        name("Bob"); age(36); ignoringEmail()
    })
    // @end
}

fun s9() {
    // @code
    assertThat(ADA_WITHOUT_PHONE, isPerson { name("Ada"); phone(null) })
    // @end
}

fun s10() {
    // @code
    assertThat(PARIS, isAddress { checkingAllProperties(); city("Paris"); street("1 Rue de Rivoli") })
    // @end
}
