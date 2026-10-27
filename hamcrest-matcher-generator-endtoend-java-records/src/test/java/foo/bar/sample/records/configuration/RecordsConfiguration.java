/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.configuration;

import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

@GenerateMatchers(
        value = "foo.bar.sample.records.model",
        exclude = "foo.bar.sample.records.model.HiddenRecord")
public final class RecordsConfiguration {}
