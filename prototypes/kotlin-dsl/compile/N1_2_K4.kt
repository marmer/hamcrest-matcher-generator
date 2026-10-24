// @variant K4
package proto.compile.n1k4

import org.hamcrest.Matcher
import proto.dsl.k4.matches
import proto.model.*

// @code
fun n(m: Matcher<String>?) = matches<Person> { Person::getPhone matches m }
// @end
