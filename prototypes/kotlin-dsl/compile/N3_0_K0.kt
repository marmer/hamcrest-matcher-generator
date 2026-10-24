// @scenario N3 wrong value type: age("36") instead of age(36)
// @variant K0
package proto.compile.n3k0

import org.hamcrest.Matcher
import proto.hamcrest.CustomerMatcher.isCustomer
import proto.hamcrest.OrderMatcher.isOrder
import proto.hamcrest.PersonMatcher.isPerson

// @code
fun n() = isPerson().withAge("36")
// @end
