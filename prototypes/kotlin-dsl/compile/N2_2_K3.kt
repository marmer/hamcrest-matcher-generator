// @variant K3
package proto.compile.n2k3

import org.hamcrest.Matcher
import proto.dsl.k3.*

// @code
fun n() = isOrder { customer { name("Ada"); id(7L) } }
// @end
