/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ShelfRecord(
        String label,
        List<BookRecord> books,
        Set<BookRecord> favourites,
        Map<String, BookRecord> byCode,
        BookRecord[] featured) {}
