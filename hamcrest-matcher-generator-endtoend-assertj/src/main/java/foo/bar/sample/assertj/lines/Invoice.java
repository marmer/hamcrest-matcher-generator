/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.lines;

import java.util.List;
import java.util.Map;

public class Invoice {
    private final List<Line> lines;
    private final Map<String, Line> linesByCode;

    public Invoice(final List<Line> lines, final Map<String, Line> linesByCode) {
        this.lines = lines;
        this.linesByCode = linesByCode;
    }

    public List<Line> getLines() {
        return lines;
    }

    public Map<String, Line> getLinesByCode() {
        return linesByCode;
    }
}
