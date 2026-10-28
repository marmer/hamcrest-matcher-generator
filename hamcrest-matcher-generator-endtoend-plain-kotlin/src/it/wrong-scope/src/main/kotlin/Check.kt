import model.Order
import model.isOrder

/** A check of the order's id written into the customer's scope: the outer scope is not in reach. */
fun check(order: Order) = isOrder {
    customer { id(1L) }
}
