package sample2.classes;

import org.hamcrest.Matcher;

public class PojoWithMatcherProperty {
    private final Matcher<?> someProperty;

    private PojoWithMatcherProperty(final Matcher<?> someProperty) {
        this.someProperty = someProperty;
    }

    public Matcher<?> getSomeProperty() {
        return someProperty;
    }
}
