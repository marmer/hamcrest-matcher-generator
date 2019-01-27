package io.github.marmer.annotationprocessing.generation;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Set;

public interface MatcherGenerator {
    String generateMatcherFor(PojoMatcherDescriptor descriptor);

    @Value
    @Builder
    @Wither
    class PojoMatcherDescriptor {
        String typeName;
        String packageName;
        Set<PropertyDescriptor> properties;
    }

    @Value
    @Builder
    @Wither
    class PropertyDescriptor {
        String typeName;
        String typePackage;
    }
}
