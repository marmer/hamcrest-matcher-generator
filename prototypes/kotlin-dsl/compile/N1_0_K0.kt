// @scenario N1 JSpecify: a nullable Matcher is rejected (value shorthand takes String?, the matcher overload does not)
// @variant K0
package proto.compile.n1k0

import org.hamcrest.Matcher
import proto.hamcrest.CustomerMatcher.isCustomer
import proto.hamcrest.OrderMatcher.isOrder
import proto.hamcrest.PersonMatcher.isPerson

// @code
fun n(m: Matcher<String>?) = isPerson().withPhone(m)
// @end
