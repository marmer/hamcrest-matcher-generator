/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * The backend interface of the processor: {@link GeneratorBackend}, the {@link GenerationContext}
 * it receives and the {@link GeneratedSource} files it returns.
 *
 * <p>This package is internal to the project. The project's own backends are its only users. It
 * makes no stability promise: its types may change in any release. The rule is stated in arc42
 * chapter 5 (building block view) and amended in ADR 0001.
 */
package io.github.marmer.matchergenerator.core.spi;
