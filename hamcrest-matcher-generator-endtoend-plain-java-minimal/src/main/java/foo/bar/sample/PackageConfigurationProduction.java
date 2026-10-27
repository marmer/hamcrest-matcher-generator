/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample;

import io.github.marmer.matchergenerator.annotations.GenerateMatchers;

@GenerateMatchers({"foo.bar.sample.prodonly.model.SomeProdOnlyModel"})
public class PackageConfigurationProduction {}
