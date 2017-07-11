package sample.classes.subpackage;

public class ExceptionWithProperties extends Exception {
    private static final long serialVersionUID = 1L;

    public String getSomeProperty() {
        return "somePropertyValue";
    }
}
