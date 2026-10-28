/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Compiles small Kotlin snippets against the generated DSL with the Kotlin compiler, the way a user
 * would. The snippets are checked with Kotlin 1.9 language features, the level the generated DSL
 * promises.
 */
internal class KotlinDslCompileFailureTest {
    private val header = """
        import io.github.marmer.testutils.model.Address
        import io.github.marmer.testutils.model.Customer
        import io.github.marmer.testutils.model.Order
        import io.github.marmer.testutils.model.isOrder
        import org.hamcrest.MatcherAssert.assertThat
        import org.hamcrest.Matchers.equalTo

        fun check(order: Order) {
    """.trimIndent()

    @Test
    @DisplayName("A check written into the scope of the outer type is rejected")
    fun test_CheckInTheWrongOuterScopeIsRejected() {
        val valid = header + """
            assertThat(order, isOrder { customer { name("Ada") }; id(1L) })
        }
        """.trimIndent()
        val wrongScope = header + """
            assertThat(order, isOrder { customer { id(1L) } })
        }
        """.trimIndent()

        assertEquals(ExitCode.OK, compile(valid).exitCode, "The snippet of the right scope compiles")
        val failure = compile(wrongScope)
        assertEquals(ExitCode.COMPILATION_ERROR, failure.exitCode)
        assertTrue(
            failure.errors.contains("can't be called in this context by implicit receiver"),
            failure.errors,
        )
    }

    @Test
    @DisplayName("A value of the wrong type is rejected")
    fun test_WrongValueTypeIsRejected() {
        val valid = header + """
            assertThat(order, isOrder { id(1L) })
        }
        """.trimIndent()
        val wrongType = header + """
            assertThat(order, isOrder { id("forty-two") })
        }
        """.trimIndent()

        assertEquals(ExitCode.OK, compile(valid).exitCode, "The snippet of the right type compiles")
        val failure = compile(wrongType)
        assertEquals(ExitCode.COMPILATION_ERROR, failure.exitCode)
        assertTrue(failure.errors.contains("fun id(value: Long)"), failure.errors)
    }

    @Test
    @DisplayName("A nested value of the wrong type is rejected")
    fun test_WrongNestedValueTypeIsRejected() {
        val wrongType = header + """
            assertThat(order, isOrder { customer { address { city(42) } } })
        }
        """.trimIndent()

        assertEquals(ExitCode.COMPILATION_ERROR, compile(wrongType).exitCode)
    }

    private class Result(val exitCode: ExitCode, val errors: String)

    private fun compile(source: String): Result {
        val directory = Files.createTempDirectory("kotlin-dsl-snippet")
        val file = directory.resolve("Snippet.kt")
        Files.writeString(file, source)
        val errors = ByteArrayOutputStream()
        val exitCode =
            K2JVMCompiler().exec(
                PrintStream(errors, true, Charsets.UTF_8),
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                directory.resolve("classes").toString(),
                "-language-version",
                "1.9",
                "-api-version",
                "1.9",
                "-jvm-target",
                "17",
                "-no-stdlib",
                "-no-reflect",
                file.toString(),
            )
        return Result(exitCode, errors.toString(Charsets.UTF_8))
    }
}
