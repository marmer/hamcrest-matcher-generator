// @variant K4
package proto.compile.n3k4

import org.hamcrest.Matcher
import proto.dsl.k4.matches
import proto.model.*

// @code
fun n() = matches<Person> { Person::getAge eq "36" }
// @end
