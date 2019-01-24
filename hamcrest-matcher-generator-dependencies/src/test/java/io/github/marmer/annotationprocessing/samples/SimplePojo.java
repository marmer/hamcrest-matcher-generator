package io.github.marmer.annotationprocessing.samples;

import lombok.Data;

@Data
public class SimplePojo {
    private String dog;
    private int cat;

    public String testMethod() {
        return "blubba";
    }

    public int getAnotherGetterValue() {
        return 42;
    }
}
