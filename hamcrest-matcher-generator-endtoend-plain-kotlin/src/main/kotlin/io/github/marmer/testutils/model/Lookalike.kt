/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.model

/** A property named like a member of [Any], which the Kotlin DSL leaves out and the Java matcher keeps. */
class Lookalike(val id: Long, val hashCode: String)
