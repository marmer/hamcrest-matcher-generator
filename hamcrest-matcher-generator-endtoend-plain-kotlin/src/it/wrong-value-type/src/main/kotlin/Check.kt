import model.isOrder

/** A value of the wrong type: the order's id is a number, not a text. */
fun check() = isOrder {
    id("forty-two")
}
