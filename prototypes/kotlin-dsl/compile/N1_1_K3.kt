// @variant K3
package proto.compile.n1k3

import org.hamcrest.Matcher
import proto.dsl.k3.*

// @code
fun n(m: Matcher<String>?) = isPerson { phone(m) }
// @end
