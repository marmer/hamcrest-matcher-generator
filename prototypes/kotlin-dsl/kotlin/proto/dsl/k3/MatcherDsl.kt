// PROTOTYPE. K3: per-type Kotlin wrapper, as a Kotlin-emitting generator (or hand-written layer) would produce it.
// One scope class per generated matcher; @DslMarker stops inner blocks from calling the outer scope by accident.
package proto.dsl.k3

import java.math.BigDecimal
import org.hamcrest.Matcher
import proto.hamcrest.AddressMatcher
import proto.hamcrest.CartMatcher
import proto.hamcrest.CustomerMatcher
import proto.hamcrest.InventoryMatcher
import proto.hamcrest.OrderLineMatcher
import proto.hamcrest.OrderMatcher
import proto.hamcrest.PersonMatcher
import proto.hamcrest.WishlistMatcher
import proto.model.Address
import proto.model.Customer
import proto.model.OrderLine

@DslMarker
annotation class MatcherDsl

fun isPerson(block: PersonSpec.() -> Unit): PersonMatcher = PersonMatcher.isPerson().also { PersonSpec(it).block() }

@MatcherDsl
class PersonSpec(private val m: PersonMatcher) {
    fun checkingAllProperties() { m.checkingAllProperties() }
    fun name(value: String?) { m.withName(value) }
    fun name(matcher: Matcher<in String>) { m.withName(matcher) }
    fun ignoringName() { m.ignoringName() }
    fun age(value: Int) { m.withAge(value) }
    fun age(matcher: Matcher<in Int>) { m.withAge(matcher) }
    fun ignoringAge() { m.ignoringAge() }
    fun email(value: String?) { m.withEmail(value) }
    fun email(matcher: Matcher<in String>) { m.withEmail(matcher) }
    fun ignoringEmail() { m.ignoringEmail() }
    fun phone(value: String?) { m.withPhone(value) }
    fun phone(matcher: Matcher<in String>) { m.withPhone(matcher) }
    fun ignoringPhone() { m.ignoringPhone() }
}

fun isOrder(block: OrderSpec.() -> Unit): OrderMatcher = OrderMatcher.isOrder().also { OrderSpec(it).block() }

@MatcherDsl
class OrderSpec(private val m: OrderMatcher) {
    fun checkingAllProperties() { m.checkingAllProperties() }
    fun id(value: Long) { m.withId(value) }
    fun id(matcher: Matcher<in Long>) { m.withId(matcher) }
    fun customer(value: Customer?) { m.withCustomer(value) }
    fun customer(matcher: Matcher<in Customer>) { m.withCustomer(matcher) }
    fun customer(block: CustomerSpec.() -> Unit) { m.withCustomer(isCustomer(block)) }
    fun total(value: BigDecimal?) { m.withTotal(value) }
    fun total(matcher: Matcher<in BigDecimal>) { m.withTotal(matcher) }
    fun tags(value: List<String>?) { m.withTags(value) }
    fun tags(matcher: Matcher<in List<String>>) { m.withTags(matcher) }
}

fun isCustomer(block: CustomerSpec.() -> Unit): CustomerMatcher = CustomerMatcher.isCustomer().also { CustomerSpec(it).block() }

@MatcherDsl
class CustomerSpec(private val m: CustomerMatcher) {
    fun name(value: String?) { m.withName(value) }
    fun name(matcher: Matcher<in String>) { m.withName(matcher) }
    fun phone(value: String?) { m.withPhone(value) }
    fun phone(matcher: Matcher<in String>) { m.withPhone(matcher) }
    fun address(value: Address?) { m.withAddress(value) }
    fun address(matcher: Matcher<in Address>) { m.withAddress(matcher) }
    fun address(block: AddressSpec.() -> Unit) { m.withAddress(isAddress(block)) }
}

fun isAddress(block: AddressSpec.() -> Unit): AddressMatcher = AddressMatcher.isAddress().also { AddressSpec(it).block() }

@MatcherDsl
class AddressSpec(private val m: AddressMatcher) {
    fun checkingAllProperties() { m.checkingAllProperties() }
    fun street(value: String?) { m.withStreet(value) }
    fun city(value: String?) { m.withCity(value) }
    fun city(matcher: Matcher<in String>) { m.withCity(matcher) }
    fun zip(value: String?) { m.withZip(value) }
}

fun isOrderLine(block: OrderLineSpec.() -> Unit): OrderLineMatcher = OrderLineMatcher.isOrderLine().also { OrderLineSpec(it).block() }

@MatcherDsl
class OrderLineSpec(private val m: OrderLineMatcher) {
    fun sku(value: String?) { m.withSku(value) }
    fun quantity(value: Int) { m.withQuantity(value) }
}

fun isCart(block: CartSpec.() -> Unit): CartMatcher = CartMatcher.isCart().also { CartSpec(it).block() }

@MatcherDsl
class CartSpec(private val m: CartMatcher) {
    fun owner(value: String?) { m.withOwner(value) }
    fun lines(value: List<OrderLine>?) { m.withLines(value) }
    fun lines(matcher: Matcher<in List<OrderLine>>) { m.withLines(matcher) }
}

fun isWishlist(block: WishlistSpec.() -> Unit): WishlistMatcher = WishlistMatcher.isWishlist().also { WishlistSpec(it).block() }

@MatcherDsl
class WishlistSpec(private val m: WishlistMatcher) {
    fun owner(value: String?) { m.withOwner(value) }
    fun items(matcher: Matcher<in Set<OrderLine>>) { m.withItems(matcher) }
}

fun isInventory(block: InventorySpec.() -> Unit): InventoryMatcher = InventoryMatcher.isInventory().also { InventorySpec(it).block() }

@MatcherDsl
class InventorySpec(private val m: InventoryMatcher) {
    fun owner(value: String?) { m.withOwner(value) }
    fun bySku(matcher: Matcher<in Map<String, OrderLine>>) { m.withBySku(matcher) }
}
