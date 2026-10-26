package demo

import proto.gen.Gen

@Gen
data class Foo(val name: String, val age: Int)
