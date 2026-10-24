// PROTOTYPE harness: runs every scenario for every candidate and prints the real output
// in the format scripts/agents/prototype-page.py reads.
package proto.demo

import proto.demo.assertj.a1
import proto.demo.assertj.a2
import proto.demo.k0.s10 as k0s10
import proto.demo.k1.s10 as k1s10
import proto.demo.k2.s10 as k2s10
import proto.demo.k3.s10 as k3s10
import proto.demo.k4.s10 as k4s10
import proto.demo.k0.s1 as k0s1
import proto.demo.k0.s2 as k0s2
import proto.demo.k0.s3 as k0s3
import proto.demo.k0.s4 as k0s4
import proto.demo.k0.s5 as k0s5
import proto.demo.k0.s6 as k0s6
import proto.demo.k0.s7 as k0s7
import proto.demo.k0.s8 as k0s8
import proto.demo.k0.s9 as k0s9
import proto.demo.k1.s1 as k1s1
import proto.demo.k1.s2 as k1s2
import proto.demo.k1.s3 as k1s3
import proto.demo.k1.s4 as k1s4
import proto.demo.k1.s5 as k1s5
import proto.demo.k1.s6 as k1s6
import proto.demo.k1.s7 as k1s7
import proto.demo.k1.s8 as k1s8
import proto.demo.k1.s9 as k1s9
import proto.demo.k2.s1 as k2s1
import proto.demo.k2.s2 as k2s2
import proto.demo.k2.s3 as k2s3
import proto.demo.k2.s4 as k2s4
import proto.demo.k2.s5 as k2s5
import proto.demo.k2.s6 as k2s6
import proto.demo.k2.s7 as k2s7
import proto.demo.k2.s8 as k2s8
import proto.demo.k2.s9 as k2s9
import proto.demo.k3.s1 as k3s1
import proto.demo.k3.s2 as k3s2
import proto.demo.k3.s3 as k3s3
import proto.demo.k3.s4 as k3s4
import proto.demo.k3.s5 as k3s5
import proto.demo.k3.s6 as k3s6
import proto.demo.k3.s7 as k3s7
import proto.demo.k3.s8 as k3s8
import proto.demo.k3.s9 as k3s9
import proto.demo.k4.s1 as k4s1
import proto.demo.k4.s2 as k4s2
import proto.demo.k4.s3 as k4s3
import proto.demo.k4.s4 as k4s4
import proto.demo.k4.s5 as k4s5
import proto.demo.k4.s6 as k4s6
import proto.demo.k4.s7 as k4s7
import proto.demo.k4.s8 as k4s8
import proto.demo.k4.s9 as k4s9

typealias Run = () -> Unit

fun scenario(group: String, id: String, title: String, vararg variants: Pair<String, Run>) {
    println("##### $group $id $title")
    for ((name, run) in variants) {
        println("=== $name")
        try {
            run(); println("PASSED")
        } catch (e: AssertionError) {
            println(e.message?.trim())
        }
    }
}

fun hamcrest(id: String, title: String, k0: Run, k1: Run, k2: Run, k3: Run, k4: Run) =
    scenario("HAMCREST", id, title, "K0" to k0, "K1" to k1, "K2" to k2, "K3" to k3, "K4" to k4)

fun main() {
    hamcrest("S1", "flat POJO: two properties differ",
        ::k0s1, ::k1s1, ::k2s1, ::k3s1, ::k4s1)
    hamcrest("S2", "nested: Order -> Customer -> Address (record)",
        ::k0s2, ::k1s2, ::k2s2, ::k3s2, ::k4s2)
    hamcrest("S3", "record: two components differ",
        ::k0s3, ::k1s3, ::k2s3, ::k3s3, ::k4s3)
    hamcrest("S4", "nested value is null",
        ::k0s4, ::k1s4, ::k2s4, ::k3s4, ::k4s4)
    hamcrest("S5", "list of a generated type, in order",
        ::k0s5, ::k1s5, ::k2s5, ::k3s5, ::k4s5)
    hamcrest("S6", "set of a generated type, any order",
        ::k0s6, ::k1s6, ::k2s6, ::k3s6, ::k4s6)
    hamcrest("S7", "map from key to a generated type",
        ::k0s7, ::k1s7, ::k2s7, ::k3s7, ::k4s7)
    hamcrest("S8", "completeness check: phone neither checked nor ignored",
        ::k0s8, ::k1s8, ::k2s8, ::k3s8, ::k4s8)
    hamcrest("S9", "JSpecify: null as expected value (phone is null) -> passes",
        ::k0s9, ::k1s9, ::k2s9, ::k3s9, ::k4s9)
    hamcrest("S10", "completeness check on a record: zip neither checked nor ignored",
        ::k0s10, ::k1s10, ::k2s10, ::k3s10, ::k4s10)
    scenario("ASSERTJ", "A1", "nested: Order -> Customer -> Address, as generated", "AS_IS" to ::a1)
    scenario("ASSERTJ", "A2", "list of a generated type, as generated", "AS_IS" to ::a2)
}
