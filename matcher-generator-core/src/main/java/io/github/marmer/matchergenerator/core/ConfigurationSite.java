/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * The configuration that caused a message: the annotated element and its annotation mirror. A
 * diagnostic points at it, so the user finds the annotation.
 */
record ConfigurationSite(Element element, AnnotationMirror mirror) {}
