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
        private BaseDescriptor baseDescriptor;
        private String typeName;
        private String packageName;
        private Set<PropertyDescriptor> properties;
    }

    @Value
    @Builder
    @Wither
    class PropertyDescriptor {
        private String typeName;
        private String typePackage;
    }

    @Value
    @Wither
    @Builder
    class BaseDescriptor {
        private String condigurationClassNameFullQualified;
        private String pojoClassNameFullQualified;
    }
}
