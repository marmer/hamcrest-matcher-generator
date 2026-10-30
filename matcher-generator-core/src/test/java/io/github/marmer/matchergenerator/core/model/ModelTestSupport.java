/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core.model;

import java.lang.reflect.Proxy;
import javax.lang.model.type.TypeMirror;

/** Builds model values for the model tests; the type mirror is never inspected there. */
final class ModelTestSupport {

    private ModelTestSupport() {}

    static TypeMirror anyType() {
        return (TypeMirror)
                Proxy.newProxyInstance(
                        ModelTestSupport.class.getClassLoader(),
                        new Class<?>[] {TypeMirror.class},
                        (proxy, method, args) -> null);
    }

    static PropertyModel property(final String name, final String accessor) {
        return new PropertyModel(name, anyType(), accessor, Nullability.UNSPECIFIED);
    }
}
