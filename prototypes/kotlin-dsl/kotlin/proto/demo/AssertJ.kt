// PROTOTYPE. The AssertJ backend used from Kotlin as generated (stand-ins from the mismatch prototype); no DSL.
package proto.demo.assertj

import org.assertj.core.api.Assertions.assertThat
import proto.assertj.AddressAssert.assertThat
import proto.assertj.CartAssert.assertThat
import proto.assertj.CustomerAssert.assertThat
import proto.assertj.OrderAssert.assertThat
import proto.assertj.OrderLineAssert.assertThat
import proto.demo.*

fun a1() {
    // @code
    assertThat(ORDER).hasId(42L).hasCustomerSatisfying {
        assertThat(it).hasName("Ada").hasAddressSatisfying { assertThat(it).hasCity("Berlin") }
    }
    // @end
}

fun a2() {
    // @code
    assertThat(CART).hasLinesSatisfying { lines ->
        assertThat(lines).satisfiesExactly(
            { assertThat(it).hasSku("A-1").hasQuantity(1) },
            { assertThat(it).hasSku("B-2").hasQuantity(3) },
            { assertThat(it).hasSku("C-3").hasQuantity(1) })
    }
    // @end
}
