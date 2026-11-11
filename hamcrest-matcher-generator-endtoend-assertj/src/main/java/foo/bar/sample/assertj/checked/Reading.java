/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.checked;

import java.io.IOException;

/** A reading whose name cannot be read, as a sensor that is offline does not give one. */
public class Reading {

    /**
     * Reads the name, which the offline sensor cannot give.
     *
     * @throws IOException always
     */
    public String getName() throws IOException {
        throw new IOException("sensor offline");
    }

    public int getCount() {
        return 1;
    }
}
