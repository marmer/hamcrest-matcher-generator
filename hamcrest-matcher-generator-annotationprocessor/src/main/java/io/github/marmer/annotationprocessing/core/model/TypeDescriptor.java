package io.github.marmer.annotationprocessing.core.model;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
public class TypeDescriptor {
    private String typeName;
    private String packageName;
}
