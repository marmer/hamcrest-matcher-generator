package demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FooDslTest {
    @Test
    fun `generated Kotlin DSL calls generated Java matcher`() {
        // foo { } is from the generated FooDsl.kt; FooMatcher is the generated Java class
        val matcher: FooMatcher = foo {
            with("name", "Ada")
            with("age", 36)
        }
        assertEquals("Foo[name=Ada;age=36;]", matcher.describe())
        kotlin.test.assertTrue(matcher.matches(Foo("Ada", 36)))
        assertNotNull(FooMatcher.KAPT_KOTLIN_GENERATED)
        println("kapt.kotlin.generated = " + FooMatcher.KAPT_KOTLIN_GENERATED)
    }
}
