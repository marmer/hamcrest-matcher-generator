// @scenario N4 K4 with Kotlin property syntax on a Java POJO (Person::name instead of Person::getName)
// @variant K4
package proto.compile.n4k4

import org.hamcrest.Matcher
import proto.dsl.k4.matches
import proto.model.*

// @code
fun n() = matches<Person> { Person::name eq "Ada" }
// @end
