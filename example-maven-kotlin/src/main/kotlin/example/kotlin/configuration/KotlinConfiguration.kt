/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.kotlin.configuration

// tag::configuration[]
import io.github.marmer.matchergenerator.annotations.GenerateMatchers

/** Generates the matchers, the assertions and the Kotlin DSL for every model class in `example.kotlin.model`. */
@GenerateMatchers("example.kotlin.model")
class KotlinConfiguration
// end::configuration[]
