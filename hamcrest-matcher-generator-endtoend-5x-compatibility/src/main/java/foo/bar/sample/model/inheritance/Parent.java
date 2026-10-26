package foo.bar.sample.model.inheritance;

import java.util.UUID;

public class Parent {
    private final UUID id;

    public Parent(final UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
