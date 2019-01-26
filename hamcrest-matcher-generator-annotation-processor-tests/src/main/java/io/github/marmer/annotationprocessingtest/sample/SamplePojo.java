package io.github.marmer.annotationprocessingtest.sample;

import lombok.Getter;

public class SamplePojo {
    private String someStringProperty;

    @Getter
    private int anotherProperty;

    public String getSomeStringProperty() {
        return someStringProperty;
    }

    public void setSomeStringProperty(final String someStringProperty) {
        this.someStringProperty = someStringProperty;
    }
}
