// PROTOTYPE sample data (same values as the "Mismatch message format" prototype).
package proto.demo

import java.math.BigDecimal
import proto.model.*

val ADA = Person("Ada", 36, "ada@example.org", "+44 20 0000")
val ADA_WITHOUT_PHONE = Person("Ada", 36, "ada@example.org", null)
val PARIS = Address("1 Rue de Rivoli", "Paris", "75001")
val ORDER = Order(42, Customer("Ada", "+44 20 0000", PARIS), BigDecimal("9.99"), listOf("books", "sale"))
val ORDER_WITHOUT_CUSTOMER = Order(43, null, BigDecimal("9.99"), listOf())
val CART = Cart("Ada", listOf(OrderLine("A-1", 1), OrderLine("B-2", 2), OrderLine("C-3", 1)))
val WISHLIST = Wishlist("Ada", linkedSetOf(OrderLine("C-3", 1), OrderLine("B-2", 2), OrderLine("A-1", 1)))
val INVENTORY = Inventory("Ada", listOf(OrderLine("A-1", 1), OrderLine("B-2", 2), OrderLine("C-3", 1)).associateBy { it.sku })
