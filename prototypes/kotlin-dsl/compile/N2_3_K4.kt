// @variant K4
package proto.compile.n2k4

import org.hamcrest.Matcher
import proto.dsl.k4.matches
import proto.model.*

// @code
fun n() = matches<Order> { Order::getCustomer has { Customer::getName eq "Ada"; Order::getId eq 7L } }
// @end
