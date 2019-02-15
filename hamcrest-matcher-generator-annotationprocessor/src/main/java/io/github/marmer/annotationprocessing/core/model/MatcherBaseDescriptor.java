package io.github.marmer.annotationprocessing.core.model;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.List;

@Value
@Builder
@Wither
public class MatcherBaseDescriptor {
    private TypeDescriptor base;
    private List<PropertyDescriptor> properties;
}
