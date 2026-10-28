/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.assertj.lines;

import java.util.List;

/** A shipment with an array, a list, a nullable note and a nullable configured type. */
public class Shipment {
    private final String note;
    private final Line mainLine;
    private final List<Line> lines;
    private final Line[] parcels;

    public Shipment(
            final String note, final Line mainLine, final List<Line> lines, final Line[] parcels) {
        this.note = note;
        this.mainLine = mainLine;
        this.lines = lines;
        this.parcels = parcels;
    }

    public String getNote() {
        return note;
    }

    public Line getMainLine() {
        return mainLine;
    }

    public List<Line> getLines() {
        return lines;
    }

    public Line[] getParcels() {
        return parcels;
    }
}
