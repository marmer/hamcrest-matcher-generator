package io.github.marmer.matchergenerator.core.model;

import java.util.Objects;
import javax.lang.model.type.TypeMirror;

/**
 * A readable value of a configured type.
 *
 * @param name        the property name, e.g. {@code foo} for the accessor {@code getFoo()}
 * @param type        the type of the property value
 * @param accessor    the name of the method that reads the property
 * @param nullability whether the value may be {@code null}
 */
public record PropertyModel(String name, TypeMirror type, String accessor, Nullability nullability) {

    public PropertyModel {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(accessor, "accessor");
        Objects.requireNonNull(nullability, "nullability");
    }
}
