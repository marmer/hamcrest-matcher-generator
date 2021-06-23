package io.github.marmer.testutils

import kotlin.test.assertEquals

class HelloTest {
    @org.junit.jupiter.api.Test
    fun justSomeKotlinTest() {
        assertEquals("Hello;)", hello())
    }
}
