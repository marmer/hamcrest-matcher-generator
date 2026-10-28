/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest.kotlin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks every generated DSL scope class. Kotlin's {@code @DslMarker} makes a nested block reject
 * the checks of an outer scope, so a check written into the wrong scope does not compile.
 */
@kotlin.DslMarker
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MatcherDsl {}
