/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.hamcrest.configuration;

// tag::configuration[]
import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

/** Generates a matcher for every model class in {@code example.hamcrest.model}. */
@GenerateMatchers("example.hamcrest.model")
public final class HamcrestConfiguration {}
// end::configuration[]
