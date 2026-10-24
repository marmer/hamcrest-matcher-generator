// PROTOTYPE. K4: separate Kotlin artifact, generic (no per-type code): property references on the 6.0.0 runtime.
package proto.demo.k4

import java.math.BigDecimal
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.startsWith
import proto.demo.*
import proto.dsl.k4.matches
import proto.hamcrest.PropertyMatcher.entries
import proto.hamcrest.PropertyMatcher.itemsInAnyOrder
import proto.hamcrest.PropertyMatcher.itemsInOrder
import proto.model.*

fun s1() {
    // @code
    assertThat(ADA, matches<Person> {
        Person::getName eq "Bob"; Person::getAge eq 36; Person::getEmail matches startsWith("bob@")
    })
    // @end
}

fun s2() {
    // @code
    assertThat(ORDER, matches<Order> {
        Order::getId eq 42L
        Order::getTotal eq BigDecimal("10.00")
        Order::getCustomer has {
            Customer::getName eq "Ada"
            Customer::getAddress has { Address::city eq "Berlin" }
        }
    })
    // @end
}

fun s3() {
    // @code
    assertThat(PARIS, matches<Address> { Address::city eq "Berlin"; Address::zip eq "10115" })
    // @end
}

fun s4() {
    // @code
    assertThat(ORDER_WITHOUT_CUSTOMER, matches<Order> { Order::getCustomer has { Customer::getName eq "Ada" } })
    // @end
}

fun s5() {
    // @code
    assertThat(CART, matches<Cart> {
        Cart::getLines matches itemsInOrder(
            matches<OrderLine> { OrderLine::getSku eq "A-1"; OrderLine::getQuantity eq 1 },
            matches<OrderLine> { OrderLine::getSku eq "B-2"; OrderLine::getQuantity eq 3 })
    })
    // @end
}

fun s6() {
    // @code
    assertThat(WISHLIST, matches<Wishlist> {
        Wishlist::getItems matches itemsInAnyOrder(
            matches<OrderLine> { OrderLine::getSku eq "A-1"; OrderLine::getQuantity eq 1 },
            matches<OrderLine> { OrderLine::getSku eq "B-2"; OrderLine::getQuantity eq 3 },
            matches<OrderLine> { OrderLine::getSku eq "C-3"; OrderLine::getQuantity eq 1 })
    })
    // @end
}

fun s7() {
    // @code
    assertThat(INVENTORY, matches<Inventory> {
        Inventory::getBySku matches entries<String, OrderLine>(mapOf(
            "A-1" to matches<OrderLine> { OrderLine::getQuantity eq 1 },
            "B-2" to matches<OrderLine> { OrderLine::getQuantity eq 3 }))
    })
    // @end
}

fun s8() {
    // @code
    assertThat(ADA, matches<Person> {
        checkingAllProperties()
        Person::getName eq "Bob"; Person::getAge eq 36; Person::getEmail.ignored()
    })
    // @end
}

fun s9() {
    // @code
    assertThat(ADA_WITHOUT_PHONE, matches<Person> { Person::getName eq "Ada"; Person::getPhone eq null })
    // @end
}

fun s10() {
    // @code
    assertThat(PARIS, matches<Address> { checkingAllProperties(); Address::city eq "Paris"; Address::street eq "1 Rue de Rivoli" })
    // @end
}
