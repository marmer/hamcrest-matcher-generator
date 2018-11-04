package sample2.classes;

public class SimplePojo {
    private final String simpleProp;

    public SimplePojo(final String simpleProp) {
        this.simpleProp = simpleProp;
    }

    public String anotherNonPropertyMethod() {
        return "someNonPropertyValue";
    }

    public String getSimpleProp() {
        return simpleProp;
    }
}
