/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * What a walk over a value and its matchers found: the failing checks, one line each, and the paths
 * of the properties neither checked nor ignored by a matcher with the completeness check. The
 * unaddressed paths are no failures of their own, they close the report as one line, so a matcher
 * that cannot tell a value apart by its values alone can ignore them.
 */
@NullMarked
final class MismatchReport {
    private final List<String> failures = new ArrayList<>();
    private final List<String> unaddressed = new ArrayList<>();

    /** Adds a failure of the value at the path. */
    void fail(final String path, final String text) {
        failures.add(PathWalk.withPath(path, text));
    }

    /** Adds a failure that has no path of its own. */
    void fail(final String text) {
        failures.add(text);
    }

    void addUnaddressed(final String path) {
        unaddressed.add(path);
    }

    boolean hasFailures() {
        return !failures.isEmpty();
    }

    /**
     * @return the failures without the closing line of the unaddressed paths
     */
    List<String> failures() {
        return List.copyOf(failures);
    }

    /**
     * @return the failures followed by the closing line of the unaddressed paths, if there are any
     */
    List<String> lines() {
        final List<String> lines = new ArrayList<>(failures);
        if (!unaddressed.isEmpty()) {
            lines.add("neither checked nor ignored: " + String.join(", ", unaddressed));
        }
        return lines;
    }
}
