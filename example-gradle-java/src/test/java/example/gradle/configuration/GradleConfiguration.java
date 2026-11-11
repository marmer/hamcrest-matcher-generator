/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package example.gradle.configuration;

// tag::configuration[]
import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

/** Generates a matcher and an assertion for every model class in {@code example.gradle.model}. */
@GenerateMatchers("example.gradle.model")
public final class GradleConfiguration {}
// end::configuration[]
