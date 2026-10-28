/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.assertj.configuration;

// tag::configuration[]
import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

/** Generates an assertion class for every model class in {@code example.assertj.model}. */
@GenerateMatchers("example.assertj.model")
public final class AssertJConfiguration {}
// end::configuration[]
