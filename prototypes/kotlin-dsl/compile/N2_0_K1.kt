// @scenario N2 scope leak: Order.id written inside the customer block by mistake
// @variant K1
package proto.compile.n2k1

import org.hamcrest.Matcher
import proto.hamcrest.CustomerMatcher.isCustomer
import proto.hamcrest.OrderMatcher.isOrder
import proto.hamcrest.PersonMatcher.isPerson

// @code
fun n() = isOrder().apply { withCustomer(isCustomer().apply { withName("Ada"); withId(7L) }) }
// @end
