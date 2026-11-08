/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import io.github.marmer.matchergenerator.core.model.TypeModel;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * A type to generate for, modelled but not yet written, with the configuration that asked for it.
 */
record PlannedType(
        TypeElement typeElement,
        TypeModel model,
        Element configurationElement,
        AnnotationMirror configurationMirror) {}
