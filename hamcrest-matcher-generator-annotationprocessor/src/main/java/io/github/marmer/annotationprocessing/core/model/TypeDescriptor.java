package io.github.marmer.annotationprocessing.core.model;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Collections;
import java.util.List;

@Value
@Builder
@Wither
public class TypeDescriptor {
    private String typeName;
    @Builder.Default
    private List<String> parentNames = Collections.emptyList();
    private String packageName;
    private String fullQualifiedName;
    private boolean primitiveBased;
}
