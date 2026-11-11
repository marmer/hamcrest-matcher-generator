/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.testutils.model

import java.io.IOException

/** A reading whose name cannot be read, as a sensor that is offline does not give one. */
class CheckedReading {
    @get:Throws(IOException::class)
    val name: String
        get() = throw IOException("sensor offline")

    val count: Int = 1
}
