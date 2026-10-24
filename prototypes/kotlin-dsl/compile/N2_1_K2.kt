// @variant K2
package proto.compile.n2k2

import org.hamcrest.Matcher
import proto.hamcrest.CustomerMatcher.isCustomer
import proto.hamcrest.OrderMatcher.isOrder
import proto.hamcrest.PersonMatcher.isPerson

// @code
fun n() = isOrder { it.withCustomer { c -> c.withName("Ada"); it.withId(7L) } }
// @end
